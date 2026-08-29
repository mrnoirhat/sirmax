// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Optional;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.BillingRepository;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.NumberingRepository;
import org.sirmax.application.port.PersonRepository;
import org.sirmax.application.port.ProcedureRepository;
import org.sirmax.application.port.ServiceCatalogRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.common.PartyType;
import org.sirmax.domain.finance.Charge;
import org.sirmax.domain.finance.ChargeLine;
import org.sirmax.domain.finance.FeeCalculator;
import org.sirmax.domain.finance.FeeInput;
import org.sirmax.domain.finance.Invoice;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.procedure.ProcedureEvent;
import org.sirmax.domain.procedure.ProcedureEventKind;
import org.sirmax.domain.security.Permission;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.shared.Money;
import org.sirmax.shared.Result;

/**
 * Turns a case's fee rules into an issued invoice (master prompt §19 → §59A).
 *
 * <p>The fee engine runs against the rules of the version the case was opened with, not the
 * service's current rules — a permit applied for in December is billed at December's tariff (§39).
 * The resulting {@link Charge} becomes frozen invoice lines.
 *
 * <p>Issuing allocates the invoice number from the {@code FACT} sequence inside the same
 * transaction as the write, so a rollback never burns a number and no two invoices can share one
 * (§27, §59A.3).
 *
 * <p>An operator-supplied discount needs {@code fee.override}: waiving municipal income is a
 * supervisory act, and it is audited with its reason.
 */
public final class IssueInvoice implements UseCase<IssueInvoice.Command, Invoice> {

    private static final String SEQUENCE = "FACT";
    private static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();

    public record Command(
            Session session,
            String procedureId,
            Optional<Long> quantity,
            Optional<Money> discount,
            Optional<String> discountReason,
            Optional<String> cashSessionId,
            String source) {}

    private final BillingRepository billing;
    private final ProcedureRepository procedures;
    private final ServiceCatalogRepository catalog;
    private final PersonRepository people;
    private final NumberingRepository numbering;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public IssueInvoice(
            BillingRepository billing,
            ProcedureRepository procedures,
            ServiceCatalogRepository catalog,
            PersonRepository people,
            NumberingRepository numbering,
            IdGenerator ids,
            Clock clock,
            UnitOfWork unitOfWork,
            Audit audit) {
        this.billing = billing;
        this.procedures = procedures;
        this.catalog = catalog;
        this.people = people;
        this.numbering = numbering;
        this.ids = ids;
        this.clock = clock;
        this.unitOfWork = unitOfWork;
        this.audit = audit;
    }

    @Override
    public Result<Invoice> execute(Command c) {
        if (!c.session().can(Permission.INVOICE_ISSUE)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        if (c.discount().isPresent() && !c.session().can(Permission.FEE_OVERRIDE)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        if (c.discount().filter(Money::isPositive).isPresent()
                && c.discountReason().map(String::isBlank).orElse(true)) {
            return Result.err("REASON_REQUIRED", "invoice.discount_needs_reason");
        }

        Optional<Procedure> found = procedures.findById(c.procedureId());
        if (found.isEmpty()) {
            return Result.err("PROCEDURE_NOT_FOUND", "procedure.not_found");
        }
        Procedure procedure = found.get();

        Optional<ServiceDefinitionVersion> version =
                catalog.findVersionById(procedure.serviceVersionId());
        if (version.isEmpty()) {
            return Result.err("VERSION_NOT_FOUND", "service.version_not_found");
        }
        if (version.get().feeRules().isEmpty()) {
            return Result.err("NO_FEE_RULES", "invoice.no_fee_rules");
        }
        if (billing.findInvoicesByProcedure(procedure.id()).stream()
                .anyMatch(i -> !i.status().equals(org.sirmax.domain.finance.InvoiceStatus.VOIDED))) {
            return Result.err("ALREADY_INVOICED", "invoice.already_issued");
        }

        LocalDate today = LocalDate.ofInstant(clock.now(), LOCAL_ZONE);
        Charge charge =
                FeeCalculator.calculate(
                        version.get().feeRules(),
                        FeeInput.quantity(today, c.quantity().orElse(1L)));
        if (charge.isEmpty() || charge.isZero()) {
            return Result.err("NOTHING_TO_BILL", "invoice.nothing_to_bill");
        }

        return Result.ok(unitOfWork.execute(() -> doIssue(c, procedure, charge, today)));
    }

    private Invoice doIssue(Command c, Procedure procedure, Charge charge, LocalDate today) {
        Instant now = clock.now();
        Currency currency = Currency.getInstance(charge.currencyCode());

        Invoice invoice =
                Invoice.draft(
                        ids.newId(),
                        procedure.applicant(),
                        customerName(procedure),
                        null,
                        procedure.id(),
                        procedure.serviceDefinitionId(),
                        currency,
                        now);
        for (ChargeLine line : charge.lines()) {
            invoice.addLine(ids.newId(), line);
        }
        c.discount().ifPresent(d -> invoice.setDiscount(d, now));
        c.discountReason().ifPresent(r -> invoice.setNotes(r, now));

        String number = numbering.allocate(SEQUENCE, SEQUENCE, today.getYear());
        invoice.issue(
                number,
                today.getYear(),
                c.session().user().id(),
                c.cashSessionId().orElse(null),
                now);
        billing.save(invoice);

        procedures.appendEvent(
                ProcedureEvent.of(
                        ids.newId(),
                        procedure.id(),
                        ProcedureEventKind.INVOICED,
                        c.session().user().id(),
                        number + " · " + invoice.total(),
                        now));
        // The case now waits on money; the workflow's payment checkpoint reads this.
        procedure.blockOnPayment(now);
        procedures.save(procedure);

        audit.record(
                c.session().audit(c.source()),
                "invoice.issued",
                "Invoice",
                invoice.id(),
                null,
                number + " " + invoice.total(),
                c.discountReason().orElse(null));
        return invoice;
    }

    /**
     * The customer name is snapshotted onto the invoice, so it is resolved once here and never
     * looked up again on reprint (§59F).
     */
    private String customerName(Procedure procedure) {
        if (procedure.applicant().type() == PartyType.PERSON) {
            return people.findById(procedure.applicant().id())
                    .map(p -> p.fullName())
                    .orElse(procedure.applicant().id());
        }
        return procedure.applicant().id();
    }
}
