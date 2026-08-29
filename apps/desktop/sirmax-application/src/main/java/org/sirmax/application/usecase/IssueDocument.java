// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.BillingRepository;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.DocumentRepository;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.NumberingRepository;
import org.sirmax.application.port.OrganizationRepository;
import org.sirmax.application.port.ProcedureRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.document.DocumentKind;
import org.sirmax.domain.document.DocumentSnapshot;
import org.sirmax.domain.document.IssuedDocument;
import org.sirmax.domain.document.PaperFormat;
import org.sirmax.domain.document.VerificationCode;
import org.sirmax.domain.finance.Invoice;
import org.sirmax.domain.finance.InvoiceLine;
import org.sirmax.domain.finance.InvoiceStatus;
import org.sirmax.domain.finance.Payment;
import org.sirmax.domain.org.InstitutionProfile;
import org.sirmax.domain.org.OrganizationUnit;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.procedure.ProcedureEvent;
import org.sirmax.domain.procedure.ProcedureEventKind;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Money;
import org.sirmax.shared.Result;

/**
 * Turns an invoice into a printable document (master prompt §59E, §59F).
 *
 * <p>This is where the historical snapshot is taken. Everything the document will ever need —
 * the municipality's name, address, RNC and logo, the citizen's name and cédula, every line, every
 * total, the payment — is copied into a {@link DocumentSnapshot} at this moment. A rebrand in 2029
 * or a corrected citizen record cannot reach back and change what a 2026 document says, because the
 * renderer is given only this snapshot.
 *
 * <p>Issuing is not printing. The document gets its number and verification code here; putting paper
 * through a printer is {@link PrintDocument}, and doing it a second time never renumbers.
 */
public final class IssueDocument implements UseCase<IssueDocument.Command, IssuedDocument> {

    private static final String SEQUENCE = "DOC";
    private static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();

    /**
     * @param format Letter for the office invoice, a narrow width for the counter receipt (§59B)
     * @param paymentId when issuing a receipt for one specific payment
     */
    public record Command(
            Session session,
            String invoiceId,
            DocumentKind kind,
            PaperFormat format,
            Optional<String> paymentId,
            String source) {}

    private final DocumentRepository documents;
    private final BillingRepository billing;
    private final ProcedureRepository procedures;
    private final OrganizationRepository organizations;
    private final NumberingRepository numbering;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public IssueDocument(
            DocumentRepository documents,
            BillingRepository billing,
            ProcedureRepository procedures,
            OrganizationRepository organizations,
            NumberingRepository numbering,
            IdGenerator ids,
            Clock clock,
            UnitOfWork unitOfWork,
            Audit audit) {
        this.documents = documents;
        this.billing = billing;
        this.procedures = procedures;
        this.organizations = organizations;
        this.numbering = numbering;
        this.ids = ids;
        this.clock = clock;
        this.unitOfWork = unitOfWork;
        this.audit = audit;
    }

    @Override
    public Result<IssuedDocument> execute(Command c) {
        if (!c.session().can(Permission.INVOICE_ISSUE)
                && !c.session().can(Permission.DOCUMENT_CERTIFY)) {
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
        // A receipt attests a payment; issuing one before any money arrived would be a lie.
        if (c.kind() == DocumentKind.RECEIPT && invoice.paid().isZero()) {
            return Result.err("NOTHING_PAID", "document.nothing_paid");
        }

        return Result.ok(unitOfWork.execute(() -> doIssue(c, invoice)));
    }

    private IssuedDocument doIssue(Command c, Invoice invoice) {
        Instant now = clock.now();
        String number =
                numbering.allocate(
                        SEQUENCE, SEQUENCE, LocalDate.ofInstant(now, LOCAL_ZONE).getYear());
        VerificationCode code = VerificationCode.generate();

        DocumentSnapshot snapshot = snapshotOf(c, invoice, number, code, now);
        IssuedDocument document =
                new IssuedDocument(
                        ids.newId(),
                        number,
                        c.kind(),
                        null,
                        c.format(),
                        invoice.id(),
                        c.paymentId().orElse(null),
                        invoice.procedureId().orElse(null),
                        null,
                        code,
                        now,
                        c.session().user().id(),
                        snapshot,
                        null,
                        null,
                        0,
                        null,
                        false,
                        now);
        documents.save(document);

        invoice.procedureId()
                .ifPresent(
                        procedureId ->
                                procedures.appendEvent(
                                        ProcedureEvent.of(
                                                ids.newId(),
                                                procedureId,
                                                ProcedureEventKind.DOCUMENT_ISSUED,
                                                c.session().user().id(),
                                                number + " · " + c.kind(),
                                                now)));

        audit.record(
                c.session().audit(c.source()),
                "document.issued",
                "IssuedDocument",
                document.id(),
                invoice.number().orElse(null),
                number + " " + c.kind() + " " + c.format(),
                null);
        return document;
    }

    /** Freeze everything the document will ever render from (§59F). */
    private DocumentSnapshot snapshotOf(
            Command c, Invoice invoice, String number, VerificationCode code, Instant now) {
        Optional<OrganizationUnit> unit = organizations.findActive();
        Optional<InstitutionProfile> profile =
                unit.flatMap(u -> organizations.findProfile(u.id()));

        DocumentSnapshot.Institution institution =
                new DocumentSnapshot.Institution(
                        unit.map(OrganizationUnit::name).orElse("Ayuntamiento"),
                        Optional.empty(),
                        unit.map(OrganizationUnit::municipality),
                        profile.flatMap(InstitutionProfile::legalIdentifier),
                        profile.flatMap(InstitutionProfile::address),
                        profile.flatMap(InstitutionProfile::phone),
                        profile.flatMap(InstitutionProfile::email),
                        profile.flatMap(InstitutionProfile::website),
                        profile.flatMap(InstitutionProfile::logoPath));

        DocumentSnapshot.Customer customer =
                new DocumentSnapshot.Customer(
                        invoice.customerName(),
                        invoice.customerIdNumber().map(x -> "Cédula"),
                        invoice.customerIdNumber(),
                        Optional.empty(),
                        Optional.empty());

        List<DocumentSnapshot.Line> lines = new ArrayList<>();
        for (InvoiceLine line : invoice.lines()) {
            lines.add(
                    new DocumentSnapshot.Line(
                            line.concept(),
                            line.description(),
                            line.quantity(),
                            line.unit(),
                            line.unitPrice(),
                            line.discount(),
                            line.surcharge(),
                            line.lineTotal()));
        }

        DocumentSnapshot.Totals totals =
                new DocumentSnapshot.Totals(
                        invoice.subtotal(),
                        invoice.discount(),
                        invoice.surcharge(),
                        invoice.total(),
                        invoice.paid(),
                        invoice.balance());

        Optional<DocumentSnapshot.PaymentInfo> payment =
                lastPaymentOf(invoice, c.paymentId())
                        .map(
                                p ->
                                        new DocumentSnapshot.PaymentInfo(
                                                p.method().name(),
                                                p.amount(),
                                                p.tendered(),
                                                Optional.of(p.change()).filter(Money::isPositive),
                                                p.reference(),
                                                p.receivedAt(),
                                                Optional.of(c.session().user().displayName())));

        Optional<String> reference =
                invoice.procedureId()
                        .flatMap(procedures::findById)
                        .map(Procedure::code)
                        .or(invoice::number);

        return new DocumentSnapshot(
                c.kind(),
                number,
                now,
                institution,
                customer,
                lines,
                totals,
                payment,
                reference,
                Optional.of(c.session().user().displayName()),
                profile.flatMap(InstitutionProfile::invoiceFooter),
                code.value());
    }

    /** The named payment, or the most recent one — a receipt is normally for what just happened. */
    private Optional<Payment> lastPaymentOf(Invoice invoice, Optional<String> paymentId) {
        if (paymentId.isPresent()) {
            return billing.findPaymentById(paymentId.get());
        }
        List<Payment> payments = billing.findPaymentsByInvoice(invoice.id());
        return payments.isEmpty()
                ? Optional.empty()
                : Optional.of(payments.get(payments.size() - 1));
    }
}
