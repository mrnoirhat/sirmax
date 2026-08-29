// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.util.Optional;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.DocumentPrinter;
import org.sirmax.application.port.DocumentRenderer;
import org.sirmax.application.port.DocumentRepository;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.document.IssuedDocument;
import org.sirmax.domain.document.PaperFormat;
import org.sirmax.domain.document.PrinterProfile;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Result;

/**
 * Renders an issued document and sends it to a printer (master prompt §59D).
 *
 * <p>The rules that matter here are all about <em>not</em> creating anything: printing never
 * allocates a number, never touches the invoice, and never duplicates a payment. It renders the
 * document's frozen snapshot, hands the PDF to the spooler, and records that it happened.
 *
 * <p>A reprint needs {@code invoice.reprint} and is stamped COPIA. Every output — the first one
 * included — lands in the print history and the audit log, which is what makes "this receipt was
 * printed four times" a question the municipality can answer.
 */
public final class PrintDocument implements UseCase<PrintDocument.Command, PrintDocument.Outcome> {

    /**
     * @param printerProfileId which configured printer; empty resolves the default for the format
     * @param reason why this is being reprinted, for the audit trail
     */
    public record Command(
            Session session,
            String issuedDocumentId,
            Optional<String> printerProfileId,
            Optional<String> reason,
            String workstation,
            String source) {}

    /**
     * @param sentToPrinter {@code false} when the operator cancelled the OS dialog
     * @param wasReprint the output carried the COPIA mark
     */
    public record Outcome(IssuedDocument document, boolean sentToPrinter, boolean wasReprint) {}

    private final DocumentRepository documents;
    private final DocumentRenderer renderer;
    private final DocumentPrinter printer;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public PrintDocument(
            DocumentRepository documents,
            DocumentRenderer renderer,
            DocumentPrinter printer,
            IdGenerator ids,
            Clock clock,
            UnitOfWork unitOfWork,
            Audit audit) {
        this.documents = documents;
        this.renderer = renderer;
        this.printer = printer;
        this.ids = ids;
        this.clock = clock;
        this.unitOfWork = unitOfWork;
        this.audit = audit;
    }

    @Override
    public Result<Outcome> execute(Command c) {
        Optional<IssuedDocument> found = documents.findById(c.issuedDocumentId());
        if (found.isEmpty()) {
            return Result.err("DOCUMENT_NOT_FOUND", "document.not_found");
        }
        IssuedDocument document = found.get();
        if (document.isVoided()) {
            return Result.err("DOCUMENT_VOIDED", "document.voided");
        }

        boolean isReprint = document.printCount() > 0;
        if (isReprint && !c.session().can(Permission.INVOICE_REPRINT)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        if (!isReprint && !c.session().can(Permission.INVOICE_ISSUE)
                && !c.session().can(Permission.DOCUMENT_CERTIFY)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }

        Optional<PrinterProfile> profile = resolveProfile(c, document.paperFormat());
        if (profile.isEmpty()) {
            return Result.err("NO_PRINTER_PROFILE", "document.no_printer_profile");
        }

        // Rendered from the snapshot alone, so a reprint years later is byte-for-byte the same
        // document apart from the COPIA mark (§59F).
        byte[] pdf =
                renderer.render(
                        document.snapshot(), document.paperFormat(), document.isReprintNext());
        boolean sent = printer.print(pdf, profile.get());
        if (!sent) {
            // The operator cancelled the dialog. Nothing was printed, so nothing is recorded:
            // a print history that counts cancellations cannot answer "how many copies exist".
            return Result.ok(new Outcome(document, false, isReprint));
        }

        return Result.ok(
                unitOfWork.execute(() -> record(c, document, profile.get(), isReprint)));
    }

    /** Render without printing — the preview, and the "Guardar PDF" action (§59A.7, §59E). */
    public Result<byte[]> renderOnly(Session session, String issuedDocumentId) {
        if (!session.can(Permission.INVOICE_ISSUE)
                && !session.can(Permission.INVOICE_REPRINT)
                && !session.can(Permission.DOCUMENT_CERTIFY)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        return documents
                .findById(issuedDocumentId)
                .map(
                        document ->
                                Result.ok(
                                        renderer.render(
                                                document.snapshot(),
                                                document.paperFormat(),
                                                document.isReprintNext())))
                .orElseGet(() -> Result.err("DOCUMENT_NOT_FOUND", "document.not_found"));
    }

    private Outcome record(
            Command c, IssuedDocument document, PrinterProfile profile, boolean isReprint) {
        Instant now = clock.now();
        document.recordPrint(now);
        documents.save(document);
        documents.recordPrint(
                ids.newId(),
                document.id(),
                now,
                c.session().user().id(),
                profile.id(),
                isReprint,
                c.reason().orElse(null));

        audit.record(
                c.session().audit(c.source()),
                isReprint ? "document.reprinted" : "document.printed",
                "IssuedDocument",
                document.id(),
                null,
                document.documentNumber() + " · copia " + document.printCount(),
                c.reason().orElse(null));
        return new Outcome(document, true, isReprint);
    }

    private Optional<PrinterProfile> resolveProfile(Command c, PaperFormat format) {
        return c.printerProfileId()
                .flatMap(documents::findProfileById)
                .or(() -> documents.resolveProfile(format, c.workstation()));
    }
}
