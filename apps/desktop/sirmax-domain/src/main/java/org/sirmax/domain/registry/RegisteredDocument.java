// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.registry;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.domain.common.PartyRef;

/**
 * An entry in the municipal register — the Conservaduría (master prompt §4).
 *
 * <p>The spec is explicit that this is <b>not</b> a file attached to a case. A scan someone brought
 * to the counter is a {@code procedure_attachment}; this is an act of the register, with a
 * book/folio identity, named parties, and legal effect that outlives the procedure that produced it.
 *
 * <p>The register is append-only in spirit: once an entry is REGISTERED its identifying fields are
 * frozen and a correction is a marginal {@link Annotation}, never an edit. That is what a register
 * is for — if entries could be rewritten, the folio number would guarantee nothing.
 */
public final class RegisteredDocument {

    private final String id;
    private final String registrationNumber;
    private String documentType; // administrator-configured vocabulary
    private String title;
    private final String procedureId; // nullable

    private LocalDate documentDate; // nullable — when the document itself was executed
    private final Instant presentedAt;
    private Instant registeredAt; // nullable until registered

    private String book; // nullable
    private String volume; // nullable
    private String folio; // nullable
    private Status status;

    private String relatedAssetId; // nullable
    private String storagePath; // nullable
    private String sha256; // nullable
    private String notes; // nullable
    private final Instant createdAt;
    private Instant updatedAt;

    private final List<Party> parties = new ArrayList<>();
    private final List<Annotation> annotations = new ArrayList<>();

    /** Where an entry stands in the register (§4). */
    public enum Status {
        PRESENTED,
        UNDER_REVIEW,
        REGISTERED,
        REJECTED,
        ANNULLED;

        /** {@code true} once the entry has legal standing and must not be edited. */
        public boolean isFrozen() {
            return this == REGISTERED || this == ANNULLED;
        }
    }

    /**
     * A party named in the document. {@code role} is administrator-configured — "vendedor",
     * "comprador", "testigo" — because the vocabulary differs by document type and by country.
     */
    public record Party(String id, String documentId, PartyRef party, String role) {
        public Party {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(documentId, "documentId");
            Objects.requireNonNull(party, "party");
            Objects.requireNonNull(role, "role");
            if (role.isBlank()) {
                throw new IllegalArgumentException("role must not be blank");
            }
            role = role.strip();
        }
    }

    /** A marginal note — the only way an entry changes after registration. */
    public record Annotation(
            String id,
            String documentId,
            String text,
            Optional<String> annotatedBy,
            Instant annotatedAt) {
        public Annotation {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(documentId, "documentId");
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(annotatedAt, "annotatedAt");
            if (text.isBlank()) {
                throw new IllegalArgumentException("An annotation must have text");
            }
            text = text.strip();
            annotatedBy = annotatedBy == null ? Optional.empty() : annotatedBy;
        }
    }

    public RegisteredDocument(
            String id,
            String registrationNumber,
            String documentType,
            String title,
            String procedureId,
            LocalDate documentDate,
            Instant presentedAt,
            Instant registeredAt,
            String book,
            String volume,
            String folio,
            Status status,
            String relatedAssetId,
            String storagePath,
            String sha256,
            String notes,
            Instant createdAt,
            Instant updatedAt) {
        this.id = requireText(id, "id");
        this.registrationNumber = requireText(registrationNumber, "registrationNumber");
        this.documentType = requireText(documentType, "documentType");
        this.title = requireText(title, "title");
        this.procedureId = blankToNull(procedureId);
        this.documentDate = documentDate;
        this.presentedAt = Objects.requireNonNull(presentedAt, "presentedAt");
        this.registeredAt = registeredAt;
        this.book = blankToNull(book);
        this.volume = blankToNull(volume);
        this.folio = blankToNull(folio);
        this.status = Objects.requireNonNull(status, "status");
        this.relatedAssetId = blankToNull(relatedAssetId);
        this.storagePath = blankToNull(storagePath);
        this.sha256 = blankToNull(sha256);
        this.notes = blankToNull(notes);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** A document just presented at the counter, not yet in the register. */
    public static RegisteredDocument presented(
            String id,
            String registrationNumber,
            String documentType,
            String title,
            String procedureId,
            Instant now) {
        return new RegisteredDocument(
                id,
                registrationNumber,
                documentType,
                title,
                procedureId,
                null,
                now,
                null,
                null,
                null,
                null,
                Status.PRESENTED,
                null,
                null,
                null,
                null,
                now,
                now);
    }

    public String id() {
        return id;
    }

    public String registrationNumber() {
        return registrationNumber;
    }

    public String documentType() {
        return documentType;
    }

    public String title() {
        return title;
    }

    public Optional<String> procedureId() {
        return Optional.ofNullable(procedureId);
    }

    public Optional<LocalDate> documentDate() {
        return Optional.ofNullable(documentDate);
    }

    public Instant presentedAt() {
        return presentedAt;
    }

    public Optional<Instant> registeredAt() {
        return Optional.ofNullable(registeredAt);
    }

    public Optional<String> book() {
        return Optional.ofNullable(book);
    }

    public Optional<String> volume() {
        return Optional.ofNullable(volume);
    }

    public Optional<String> folio() {
        return Optional.ofNullable(folio);
    }

    public Status status() {
        return status;
    }

    public Optional<String> relatedAssetId() {
        return Optional.ofNullable(relatedAssetId);
    }

    public Optional<String> storagePath() {
        return Optional.ofNullable(storagePath);
    }

    public Optional<String> sha256() {
        return Optional.ofNullable(sha256);
    }

    public Optional<String> notes() {
        return Optional.ofNullable(notes);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public List<Party> parties() {
        return List.copyOf(parties);
    }

    public List<Annotation> annotations() {
        return List.copyOf(annotations);
    }

    /** {@code true} when a certified copy can be issued from this entry (§4). */
    public boolean canIssueCertifiedCopy() {
        return status == Status.REGISTERED;
    }

    public void addParty(Party party) {
        requireEditable();
        boolean duplicate =
                parties.stream()
                        .anyMatch(
                                p ->
                                        p.party().equals(party.party())
                                                && p.role().equalsIgnoreCase(party.role()));
        if (duplicate) {
            throw new IllegalArgumentException(
                    "That party already appears as " + party.role() + " on this document");
        }
        parties.add(party);
    }

    public void restoreParties(List<Party> persisted) {
        parties.clear();
        parties.addAll(persisted);
    }

    /** Marginal notes are allowed at any status — that is the point of them. */
    public void annotate(Annotation annotation) {
        annotations.add(annotation);
    }

    public void restoreAnnotations(List<Annotation> persisted) {
        annotations.clear();
        annotations.addAll(persisted);
    }

    public void updateDetails(
            String newDocumentType, String newTitle, LocalDate newDocumentDate, Instant now) {
        requireEditable();
        this.documentType = requireText(newDocumentType, "documentType");
        this.title = requireText(newTitle, "title");
        this.documentDate = newDocumentDate;
        touch(now);
    }

    public void relateToAsset(String assetId, Instant now) {
        requireEditable();
        this.relatedAssetId = blankToNull(assetId);
        touch(now);
    }

    public void attachScan(String path, String hash, Instant now) {
        this.storagePath = blankToNull(path);
        this.sha256 = blankToNull(hash);
        touch(now);
    }

    public void startReview(Instant now) {
        if (status != Status.PRESENTED) {
            throw new IllegalStateException("Only a presented document can enter review");
        }
        this.status = Status.UNDER_REVIEW;
        touch(now);
    }

    /**
     * Enter the document into the register at a book/folio. This is the act that gives it legal
     * standing, so from here the identifying fields are frozen.
     */
    public void register(String newBook, String newVolume, String newFolio, Instant now) {
        if (status.isFrozen()) {
            throw new IllegalStateException("Document " + registrationNumber + " is " + status);
        }
        this.book = blankToNull(newBook);
        this.volume = blankToNull(newVolume);
        this.folio = blankToNull(newFolio);
        if (this.book == null || this.folio == null) {
            throw new IllegalArgumentException("Registering needs at least a book and a folio");
        }
        this.status = Status.REGISTERED;
        this.registeredAt = now;
        touch(now);
    }

    public void reject(String reason, Instant now) {
        if (status.isFrozen()) {
            throw new IllegalStateException("Document " + registrationNumber + " is " + status);
        }
        String why = blankToNull(reason);
        if (why == null) {
            throw new IllegalArgumentException("A rejection must carry a reason");
        }
        this.status = Status.REJECTED;
        this.notes = why;
        touch(now);
    }

    /**
     * Annul a registered entry. The row and its folio stay — annulment is recorded <em>in</em> the
     * register, which is why the status exists instead of a delete.
     */
    public void annul(String reason, Instant now) {
        if (status != Status.REGISTERED) {
            throw new IllegalStateException("Only a registered document can be annulled");
        }
        String why = blankToNull(reason);
        if (why == null) {
            throw new IllegalArgumentException("An annulment must carry a reason");
        }
        this.status = Status.ANNULLED;
        this.notes = why;
        touch(now);
    }

    private void requireEditable() {
        if (status.isFrozen()) {
            throw new IllegalStateException(
                    "Document " + registrationNumber + " is " + status
                            + "; corrections are made by annotation");
        }
    }

    private void touch(Instant now) {
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String v = value.strip();
        if (v.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return v;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String v = value.strip();
        return v.isEmpty() ? null : v;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RegisteredDocument other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
