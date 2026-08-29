// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.document;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A document that has been issued and can be printed (master prompt §47, §59D, §59F).
 *
 * <p>Issuing and printing are separate acts. The document is created once, with its number, its
 * verification code and its frozen {@link DocumentSnapshot}; printing it — the first time or the
 * fifth — never renumbers it and never duplicates the payment behind it. Every output increments
 * {@link #printCount()} and is audited.
 *
 * <p>{@link #isReprintNext()} is what the renderer asks to decide whether to stamp COPIA on the
 * page, which §59D requires.
 */
public final class IssuedDocument {

    private final String id;
    private final String documentNumber;
    private final DocumentKind kind;
    private final String templateId; // nullable — the built-in layouts have none
    private final PaperFormat paperFormat;

    private final String invoiceId; // nullable
    private final String paymentId; // nullable
    private final String procedureId; // nullable
    private final String registeredDocumentId; // nullable

    private final VerificationCode verificationCode;
    private final Instant issuedAt;
    private final String issuedBy; // nullable
    private final DocumentSnapshot snapshot;

    private String storagePath; // nullable
    private String sha256; // nullable
    private int printCount;
    private Instant lastPrintedAt; // nullable
    private boolean voided;
    private final Instant createdAt;

    public IssuedDocument(
            String id,
            String documentNumber,
            DocumentKind kind,
            String templateId,
            PaperFormat paperFormat,
            String invoiceId,
            String paymentId,
            String procedureId,
            String registeredDocumentId,
            VerificationCode verificationCode,
            Instant issuedAt,
            String issuedBy,
            DocumentSnapshot snapshot,
            String storagePath,
            String sha256,
            int printCount,
            Instant lastPrintedAt,
            boolean voided,
            Instant createdAt) {
        this.id = requireText(id, "id");
        this.documentNumber = requireText(documentNumber, "documentNumber");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.templateId = blankToNull(templateId);
        this.paperFormat = Objects.requireNonNull(paperFormat, "paperFormat");
        this.invoiceId = blankToNull(invoiceId);
        this.paymentId = blankToNull(paymentId);
        this.procedureId = blankToNull(procedureId);
        this.registeredDocumentId = blankToNull(registeredDocumentId);
        this.verificationCode = Objects.requireNonNull(verificationCode, "verificationCode");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        this.issuedBy = blankToNull(issuedBy);
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.storagePath = blankToNull(storagePath);
        this.sha256 = blankToNull(sha256);
        if (printCount < 0) {
            throw new IllegalArgumentException("printCount must be >= 0");
        }
        this.printCount = printCount;
        this.lastPrintedAt = lastPrintedAt;
        this.voided = voided;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static IssuedDocument issue(
            String id,
            String documentNumber,
            DocumentKind kind,
            PaperFormat paperFormat,
            DocumentSnapshot snapshot,
            String issuedBy,
            Instant now) {
        return new IssuedDocument(
                id,
                documentNumber,
                kind,
                null,
                paperFormat,
                null,
                null,
                null,
                null,
                new VerificationCode(snapshot.verificationCode()),
                now,
                issuedBy,
                snapshot,
                null,
                null,
                0,
                null,
                false,
                now);
    }

    public String id() {
        return id;
    }

    public String documentNumber() {
        return documentNumber;
    }

    public DocumentKind kind() {
        return kind;
    }

    public Optional<String> templateId() {
        return Optional.ofNullable(templateId);
    }

    public PaperFormat paperFormat() {
        return paperFormat;
    }

    public Optional<String> invoiceId() {
        return Optional.ofNullable(invoiceId);
    }

    public Optional<String> paymentId() {
        return Optional.ofNullable(paymentId);
    }

    public Optional<String> procedureId() {
        return Optional.ofNullable(procedureId);
    }

    public Optional<String> registeredDocumentId() {
        return Optional.ofNullable(registeredDocumentId);
    }

    public VerificationCode verificationCode() {
        return verificationCode;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Optional<String> issuedBy() {
        return Optional.ofNullable(issuedBy);
    }

    /** The frozen data this document renders from — never today's data (§59F). */
    public DocumentSnapshot snapshot() {
        return snapshot;
    }

    public Optional<String> storagePath() {
        return Optional.ofNullable(storagePath);
    }

    public Optional<String> sha256() {
        return Optional.ofNullable(sha256);
    }

    public int printCount() {
        return printCount;
    }

    public Optional<Instant> lastPrintedAt() {
        return Optional.ofNullable(lastPrintedAt);
    }

    public boolean isVoided() {
        return voided;
    }

    public Instant createdAt() {
        return createdAt;
    }

    /** {@code true} when the next output is a reprint and must be marked as a copy (§59D). */
    public boolean isReprintNext() {
        return printCount > 0 && kind.marksReprints();
    }

    public void recordPrint(Instant now) {
        if (voided) {
            throw new IllegalStateException(
                    "Document " + documentNumber + " is voided and must not be printed");
        }
        this.printCount++;
        this.lastPrintedAt = now;
    }

    public void attachFile(String path, String hash) {
        this.storagePath = blankToNull(path);
        this.sha256 = blankToNull(hash);
    }

    /**
     * Void the document — the underlying invoice was voided, or it was issued in error. The row and
     * its number stay: a voided document that vanished would leave a gap nobody can explain.
     */
    public void voidDocument() {
        this.voided = true;
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
        return o instanceof IssuedDocument other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
