// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.util.List;
import java.util.Optional;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.registry.Decision;
import org.sirmax.domain.registry.Inspection;
import org.sirmax.domain.registry.RegisteredDocument;

/**
 * Persistence for the municipal register, inspections and decisions (master prompt §4, §28, §29).
 *
 * <p>Saving a {@link RegisteredDocument} also replaces its parties and appends its annotations: the
 * three are one entry, and letting a caller commit a document without its parties would produce a
 * register entry that names nobody.
 */
public interface RegistryRepository {

    // ── registered documents (Conservaduría) ──

    void save(RegisteredDocument document);

    Optional<RegisteredDocument> findDocumentById(String id);

    Optional<RegisteredDocument> findDocumentByNumber(String registrationNumber);

    /** Register search: by title, type, book/folio or registration number (§4). */
    List<RegisteredDocument> searchDocuments(
            Optional<String> text,
            Optional<String> documentType,
            Optional<RegisteredDocument.Status> status,
            int limit,
            int offset);

    /** Every register entry naming a party — "¿qué hay registrado a nombre de esta persona?" */
    List<RegisteredDocument> documentsNaming(PartyRef party, int limit);

    List<RegisteredDocument> documentsForAsset(String assetId);

    // ── inspections ──

    void save(Inspection inspection);

    Optional<Inspection> findInspectionById(String id);

    List<Inspection> inspectionsFor(String procedureId);

    /** An inspector's visit list, soonest first. */
    List<Inspection> inspectionsAssignedTo(String userId, int limit);

    /** Scheduled visits that have not happened by {@code asOf}. */
    List<Inspection> overdueInspections(java.time.LocalDate asOf, int limit);

    // ── decisions ──

    void save(Decision decision);

    /** Every decision taken on a case, oldest first. */
    List<Decision> decisionsFor(String procedureId);
}
