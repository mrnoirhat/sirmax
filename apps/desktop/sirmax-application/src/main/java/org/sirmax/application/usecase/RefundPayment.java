// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.BillingRepository;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.NumberingRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.finance.Invoice;
import org.sirmax.domain.finance.Payment;
import org.sirmax.domain.finance.Refund;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Money;
import org.sirmax.shared.Result;

/**
 * Returns money to a payer (master prompt §59A.6).
 *
 * <p>The {@link Payment} row is never edited or deleted; a {@link Refund} is written beside it and
 * the payment is marked reversed once nothing is left of it. After the fact the drawer shows both
 * legs — money in, money out — which is what reconciliation and any later audit need.
 *
 * <p>Partial refunds are allowed. The sum of refunds against one payment can never exceed it, and a
 * refund always carries a reason and the authorizing operator.
 */
public final class RefundPayment implements UseCase<RefundPayment.Command, Refund> {

    private static final String SEQUENCE = "DEV";
    private static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();

    public record Command(
            Session session,
            String paymentId,
            Optional<Money> amount,
            String reason,
            String source) {}

    private final BillingRepository billing;
    private final NumberingRepository numbering;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public RefundPayment(
            BillingRepository billing,
            NumberingRepository numbering,
            IdGenerator ids,
            Clock clock,
            UnitOfWork unitOfWork,
            Audit audit) {
        this.billing = billing;
        this.numbering = numbering;
        this.ids = ids;
        this.clock = clock;
        this.unitOfWork = unitOfWork;
        this.audit = audit;
    }

    @Override
    public Result<Refund> execute(Command c) {
        if (!c.session().can(Permission.PAYMENT_REFUND)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        if (c.reason() == null || c.reason().isBlank()) {
            return Result.err("REASON_REQUIRED", "refund.needs_reason");
        }

        Optional<Payment> found = billing.findPaymentById(c.paymentId());
        if (found.isEmpty()) {
            return Result.err("PAYMENT_NOT_FOUND", "payment.not_found");
        }
        Payment payment = found.get();
        if (!payment.isSettled()) {
            return Result.err("ALREADY_REVERSED", "payment.already_reversed");
        }

        Money alreadyRefunded = totalRefunded(payment);
        Money refundable = payment.amount().minus(alreadyRefunded);
        Money amount = c.amount().orElse(refundable);
        if (!amount.currency().equals(payment.amount().currency())) {
            return Result.err("CURRENCY_MISMATCH", "payment.currency_mismatch");
        }
        if (!amount.isPositive()) {
            return Result.err("INVALID_AMOUNT", "payment.invalid_amount");
        }
        if (amount.compareTo(refundable) > 0) {
            return Result.err("EXCEEDS_PAYMENT", "refund.exceeds_payment");
        }

        Optional<Invoice> invoice = billing.findInvoiceById(payment.invoiceId());
        if (invoice.isEmpty()) {
            return Result.err("INVOICE_NOT_FOUND", "invoice.not_found");
        }

        return Result.ok(
                unitOfWork.execute(() -> doRefund(c, payment, invoice.get(), amount, refundable)));
    }

    private Refund doRefund(
            Command c, Payment payment, Invoice invoice, Money amount, Money refundable) {
        Instant now = clock.now();
        LocalDate today = LocalDate.ofInstant(now, LOCAL_ZONE);

        String code = numbering.allocate(SEQUENCE, SEQUENCE, today.getYear());
        Refund refund =
                new Refund(
                        ids.newId(),
                        code,
                        payment.id(),
                        invoice.id(),
                        payment.cashSessionId(),
                        amount,
                        c.reason(),
                        Optional.of(c.session().user().id()),
                        now);
        billing.save(refund);

        // Fully refunded: the payment no longer stands. Partially: it does, for the remainder.
        if (amount.compareTo(refundable) == 0) {
            billing.save(payment.reversed());
        }

        invoice.reversePayment(amount, now);
        billing.save(invoice);

        audit.record(
                c.session().audit(c.source()),
                "payment.refunded",
                "Refund",
                refund.id(),
                payment.code(),
                code + " " + amount,
                c.reason());
        return refund;
    }

    private Money totalRefunded(Payment payment) {
        Money sum = Money.zero(payment.amount().currency());
        for (Refund refund : billing.findRefundsByPayment(payment.id())) {
            sum = sum.plus(refund.amount());
        }
        return sum;
    }
}
