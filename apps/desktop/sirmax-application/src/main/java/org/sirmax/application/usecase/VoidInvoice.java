// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.util.Optional;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.BillingRepository;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.finance.Invoice;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Result;

/**
 * Voids an invoice (master prompt §59A.6).
 *
 * <p>Voiding is the only way to cancel a document that was issued, and it always leaves the row:
 * the number stays spent (§27), the reason is stored, and the audit log records who did it. An
 * invoice with money against it must be refunded first — otherwise the drawer would be holding cash
 * that no document accounts for.
 */
public final class VoidInvoice implements UseCase<VoidInvoice.Command, Invoice> {

    public record Command(Session session, String invoiceId, String reason, String source) {}

    private final BillingRepository billing;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public VoidInvoice(
            BillingRepository billing, Clock clock, UnitOfWork unitOfWork, Audit audit) {
        this.billing = billing;
        this.clock = clock;
        this.unitOfWork = unitOfWork;
        this.audit = audit;
    }

    @Override
    public Result<Invoice> execute(Command c) {
        if (!c.session().can(Permission.INVOICE_VOID)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        if (c.reason() == null || c.reason().isBlank()) {
            return Result.err("REASON_REQUIRED", "invoice.void_needs_reason");
        }

        Optional<Invoice> found = billing.findInvoiceById(c.invoiceId());
        if (found.isEmpty()) {
            return Result.err("INVOICE_NOT_FOUND", "invoice.not_found");
        }
        Invoice invoice = found.get();
        if (invoice.status() == org.sirmax.domain.finance.InvoiceStatus.VOIDED) {
            return Result.err("ALREADY_VOIDED", "invoice.already_voided");
        }
        if (invoice.paid().isPositive()) {
            return Result.err("REFUND_FIRST", "invoice.refund_before_void");
        }

        return Result.ok(
                unitOfWork.execute(
                        () -> {
                            Instant now = clock.now();
                            invoice.voidInvoice(c.reason(), now);
                            billing.save(invoice);
                            audit.record(
                                    c.session().audit(c.source()),
                                    "invoice.voided",
                                    "Invoice",
                                    invoice.id(),
                                    invoice.number().orElse(null),
                                    null,
                                    c.reason());
                            return invoice;
                        }));
    }
}
