// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.backup;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One archived copy of the municipality's database (master prompt §41).
 *
 * <p>{@link #sha256()} is of the file <em>as written</em> — after compression and encryption — and
 * is what {@link #markValidated} re-checks. That makes validation a real integrity test rather than
 * a reassurance: if the bytes on disk no longer hash to this, the archive is not the one SIRMAX
 * produced, whether through disk rot, a partial write or tampering.
 *
 * <p>{@code rowCounts} is a coarse fingerprint of what was inside — how many people, procedures,
 * invoices. It is metadata, so it survives compression and encryption, and an operator can tell a
 * full backup from one taken against an empty database without decrypting anything.
 */
public final class BackupRecord {

    private final String id;
    private final String code;
    private final BackupKind kind;
    private BackupStatus status;

    private final String storagePath;
    private final long sizeBytes;
    private final String sha256;
    private final boolean compressed;
    private final boolean encrypted;

    private final int schemaVersion;
    private final Map<String, Long> rowCounts;

    private final Instant createdAt;
    private final String createdBy; // nullable
    private Instant validatedAt; // nullable

    private String remoteProvider; // nullable
    private String remoteFileId; // nullable
    private Instant uploadedAt; // nullable
    private String notes; // nullable

    public BackupRecord(
            String id,
            String code,
            BackupKind kind,
            BackupStatus status,
            String storagePath,
            long sizeBytes,
            String sha256,
            boolean compressed,
            boolean encrypted,
            int schemaVersion,
            Map<String, Long> rowCounts,
            Instant createdAt,
            String createdBy,
            Instant validatedAt,
            String remoteProvider,
            String remoteFileId,
            Instant uploadedAt,
            String notes) {
        this.id = requireText(id, "id");
        this.code = requireText(code, "code");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.status = Objects.requireNonNull(status, "status");
        this.storagePath = requireText(storagePath, "storagePath");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must be >= 0");
        }
        this.sizeBytes = sizeBytes;
        this.sha256 = requireText(sha256, "sha256");
        this.compressed = compressed;
        this.encrypted = encrypted;
        this.schemaVersion = schemaVersion;
        this.rowCounts = rowCounts == null ? Map.of() : Map.copyOf(rowCounts);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.createdBy = blankToNull(createdBy);
        this.validatedAt = validatedAt;
        this.remoteProvider = blankToNull(remoteProvider);
        this.remoteFileId = blankToNull(remoteFileId);
        this.uploadedAt = uploadedAt;
        this.notes = blankToNull(notes);
    }

    public static BackupRecord created(
            String id,
            String code,
            BackupKind kind,
            String storagePath,
            long sizeBytes,
            String sha256,
            boolean compressed,
            boolean encrypted,
            int schemaVersion,
            Map<String, Long> rowCounts,
            String createdBy,
            Instant now) {
        return new BackupRecord(
                id,
                code,
                kind,
                BackupStatus.CREATED,
                storagePath,
                sizeBytes,
                sha256,
                compressed,
                encrypted,
                schemaVersion,
                rowCounts,
                now,
                createdBy,
                null,
                null,
                null,
                null,
                null);
    }

    public String id() {
        return id;
    }

    public String code() {
        return code;
    }

    public BackupKind kind() {
        return kind;
    }

    public BackupStatus status() {
        return status;
    }

    public String storagePath() {
        return storagePath;
    }

    public long sizeBytes() {
        return sizeBytes;
    }

    public String sha256() {
        return sha256;
    }

    public boolean compressed() {
        return compressed;
    }

    public boolean encrypted() {
        return encrypted;
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public Map<String, Long> rowCounts() {
        return rowCounts;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Optional<String> createdBy() {
        return Optional.ofNullable(createdBy);
    }

    public Optional<Instant> validatedAt() {
        return Optional.ofNullable(validatedAt);
    }

    public Optional<String> remoteProvider() {
        return Optional.ofNullable(remoteProvider);
    }

    public Optional<String> remoteFileId() {
        return Optional.ofNullable(remoteFileId);
    }

    public Optional<Instant> uploadedAt() {
        return Optional.ofNullable(uploadedAt);
    }

    public Optional<String> notes() {
        return Optional.ofNullable(notes);
    }

    /** {@code true} when this copy has left the building (§41 — an operator must be able to ask). */
    public boolean isOffsite() {
        return remoteFileId != null;
    }

    public boolean isRestorable() {
        return status.isRestorable();
    }

    /** The archive was re-read and matched its hash (§42 step 2). */
    public void markValidated(Instant now) {
        if (status == BackupStatus.FAILED) {
            throw new IllegalStateException("A failed backup has nothing to validate");
        }
        // An uploaded backup stays UPLOADED: that is the stronger of the two statements.
        if (status != BackupStatus.UPLOADED) {
            this.status = BackupStatus.VALIDATED;
        }
        this.validatedAt = now;
    }

    /** The archive did not match its hash. It is never silently re-validated afterwards. */
    public void markCorrupt(String reason, Instant now) {
        this.status = BackupStatus.CORRUPT;
        this.notes = blankToNull(reason);
        this.validatedAt = now;
    }

    public void markFailed(String reason) {
        this.status = BackupStatus.FAILED;
        this.notes = blankToNull(reason);
    }

    /**
     * The local archive was deleted by the retention policy. The record keeps its hash, size and
     * fingerprint — and its remote file id, if it has one, so an off-site copy stays reachable.
     */
    public void markPruned(Instant now) {
        this.status = BackupStatus.PRUNED;
        this.notes = "Archivo local eliminado por política de retención";
        Objects.requireNonNull(now, "now");
    }

    public void markUploaded(String provider, String fileId, Instant now) {
        this.remoteProvider = requireText(provider, "provider");
        this.remoteFileId = requireText(fileId, "fileId");
        this.uploadedAt = now;
        this.status = BackupStatus.UPLOADED;
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
        return o instanceof BackupRecord other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
