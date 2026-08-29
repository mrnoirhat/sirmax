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
import org.sirmax.application.port.ProcedureRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.finance.CashSession;
import org.sirmax.domain.finance.Invoice;
import org.sirmax.domain.finance.InvoiceStatus;
import org.sirmax.domain.finance.Payment;
import org.sirmax.domain.finance.PaymentMethod;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.procedure.ProcedureEvent;
import org.sirmax.domain.procedure.ProcedureEventKind;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Money;
import org.sirmax.shared.Result;

/**
 * Collects money against an invoice (master prompt §20, §59A.5, §59A.6).
 *
 * <p>Partial payments are supported: the invoice tracks its own balance and moves to
 * {@code PARTIALLY_PAID} until it is settled. Overpayment in cash is handled as <b>change</b> — only
 * what was owed is recorded as municipal income, and the rest is handed back — which is the
 * difference between a counter and an accounting error.
 *
 * <p>Cash must be taken inside an open cash session, so the drawer can be reconciled at close.
 * Transfers, cards and cheques may be recorded without one and never touch the drawer count.
 */
public final class RegisterPayment implements UseCase<RegisterPayment.Command, RegisterPayment.Receipt> {

    private static final String SEQUENCE = "REC";
    private static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();

    public record Command(
            Session session,
            String invoiceId,
            PaymentMethod method,
            Money amount,
            Optional<Money> tendered,
            Optional<String> reference,
            Optional<String> payerName,
            String source) {}

    /**
     * @param change what the cashier must hand back; zero unless cash was tendered above the balance
     */
    public record Receipt(Payment payment, Invoice invoice, Money change) {

        public boolean isSettled() {
            return invoice.isSettled();
        }
    }

    private final BillingRepository billing;
    private final ProcedureRepository procedures;
    private final NumberingRepository numbering;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public RegisterPayment(
            BillingRepository billing,
            ProcedureRepository procedures,
            NumberingRepository numbering,
            IdGenerator ids,
            Clock clock,
            UnitOfWork unitOfWork,
            Audit audit) {
        this.billing = billing;
        this.procedures = procedures;
        this.numbering = numbering;
        this.ids = ids;
        this.clock = clock;
        this.unitOfWork = unitOfWork;
        this.audit = audit;
    }

    @Override
    public Result<Receipt> execute(Command c) {
        if (!c.session().can(Permission.PAYMENT_REGISTER)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }

        Optional<Invoice> found = billing.findInvoiceById(c.invoiceId());
        if (found.isEmpty()) {
            return Result.err("INVOICE_NOT_FOUND", "invoice.not_found");
        }
        Invoice invoice = found.get();
        if (invoice.status() == InvoiceStatus.DRAFT) {
            return Result.err("INVOICE_NOT_ISSUED", "invoice.not_issued");
        }
        if (invoice.status() == InvoiceStatus.VOIDED) {
            return Result.err("INVOICE_VOIDED", "invoice.voided");
        }
        if (invoice.isSettled()) {
            return Result.err("ALREADY_PAID", "invoice.already_paid");
        }
        if (!c.amount().isPositive()) {
            return Result.err("INVALID_AMOUNT", "payment.invalid_amount");
        }
        if (!c.amount().currency().equals(invoice.currency())) {
            return Result.err("CURRENCY_MISMATCH", "payment.currency_mismatch");
        }

        Optional<CashSession> cashSession =
                billing.findOpenSessionFor(c.session().user().id());
        if (c.method().affectsCashDrawer() && cashSession.isEmpty()) {
            return Result.err("NO_CASH_SESSION", "cash.no_open_session");
        }
        if (c.method().expectsReference() && c.reference().map(String::isBlank).orElse(true)) {
            return Result.err("REFERENCE_REQUIRED", "payment.reference_required");
        }

        return Result.ok(unitOfWork.execute(() -> doRegister(c, invoice, cashSession)));
    }

    private Receipt doRegister(Command c, Invoice invoice, Optional<CashSession> cashSession) {
        Instant now = clock.now();
        LocalDate today = LocalDate.ofInstant(now, LOCAL_ZONE);

        Money change = invoice.applyPayment(c.amount(), now);
        // Only what settled the balance is income; the change is the citizen's money going back.
        Money recorded = c.amount().minus(change);

        String code = numbering.allocate(SEQUENCE, SEQUENCE, today.getYear());
        Payment payment =
                new Payment(
                        ids.newId(),
                        code,
                        invoice.id(),
                        cashSession.map(CashSession::id),
                        c.method(),
                        recorded,
                        c.tendered(),
                        c.reference(),
                        c.payerName(),
                        Payment.Status.SETTLED,
                        Optional.of(c.session().user().id()),
                        now,
                        Optional.empty());

        billing.save(payment);
        billing.save(invoice);

        invoice.procedureId()
                .flatMap(procedures::findById)
                .ifPresent(procedure -> recordOnCase(c, procedure, invoice, code, now));

        audit.record(
                c.session().audit(c.source()),
                "payment.registered",
                "Payment",
                payment.id(),
                null,
                code + " " + recorded + " " + c.method(),
                null);
        return new Receipt(payment, invoice, change);
    }

    /** Keep the case timeline honest, and unblock it once the invoice is settled. */
    private void recordOnCase(
            Command c, Procedure procedure, Invoice invoice, String receiptCode, Instant now) {
        procedures.appendEvent(
                ProcedureEvent.of(
                        ids.newId(),
                        procedure.id(),
                        ProcedureEventKind.PAID,
                        c.session().user().id(),
                        receiptCode + " · " + invoice.paid(),
                        now));
        if (invoice.isSettled() && !procedure.status().isTerminal()) {
            procedure.resume(now);
            procedures.save(procedure);
        }
    }
}
