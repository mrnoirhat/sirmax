// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.sirmax.domain.document.IssuedDocument;
import org.sirmax.domain.document.PaperFormat;
import org.sirmax.domain.document.PrinterProfile;
import org.sirmax.domain.document.VerificationCode;

/** Persistence for issued documents, their print history and the workstation printer profiles. */
public interface DocumentRepository {

    // ── issued documents ──

    void save(IssuedDocument document);

    Optional<IssuedDocument> findById(String id);

    Optional<IssuedDocument> findByNumber(String documentNumber);

    /** Look a document up by the code printed on it — the §47 verification path. */
    Optional<IssuedDocument> findByVerificationCode(VerificationCode code);

    List<IssuedDocument> findForInvoice(String invoiceId);

    List<IssuedDocument> findForProcedure(String procedureId);

    /**
     * The most recently issued documents, newest first.
     *
     * <p>The screen could only look a document up by number or verification code, which is right
     * when a citizen is standing there holding one — and useless for "what did we issue today".
     * Recency is the order that answers that; the register is not browsed alphabetically.
     */
    List<IssuedDocument> listRecent(int limit, int offset);

    // ── print history (§59D) ──

    /**
     * Record one physical output.
     *
     * @param isReprint {@code false} only for the very first print of a document
     */
    void recordPrint(
            String id,
            String issuedDocumentId,
            Instant printedAt,
            String printedBy,
            String printerProfileId,
            boolean isReprint,
            String reason);

    /** Every output of a document, oldest first. */
    List<PrintEntry> printHistory(String issuedDocumentId);

    /** One line of a document's print history. */
    record PrintEntry(
            String id,
            String issuedDocumentId,
            Instant printedAt,
            Optional<String> printedBy,
            Optional<String> printerProfileId,
            boolean isReprint,
            Optional<String> reason) {}

    // ── printer profiles ──

    void save(PrinterProfile profile);

    Optional<PrinterProfile> findProfileById(String id);

    List<PrinterProfile> listProfiles();

    /**
     * The profile to use for {@code format} on {@code workstation}: a host-specific one wins over a
     * general one, and an explicit default wins over the rest.
     */
    Optional<PrinterProfile> resolveProfile(PaperFormat format, String workstation);
}
