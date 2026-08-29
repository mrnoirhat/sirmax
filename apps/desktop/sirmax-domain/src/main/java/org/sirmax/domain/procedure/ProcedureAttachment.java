// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.procedure;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A file attached to a case — usually the scan that satisfies a DOCUMENT requirement.
 *
 * <p>SIRMAX stores the bytes on disk under the app data directory and keeps only the path plus a
 * SHA-256 here, so the Phase 10 integrity check can tell an intact archive from a tampered one.
 *
 * @param requirementKey the checklist line this file satisfies, when it was uploaded against one
 * @param storagePath path relative to the attachments root; never an absolute path from the operator
 */
public record ProcedureAttachment(
        String id,
        String procedureId,
        Optional<String> requirementKey,
        String fileName,
        Optional<String> contentType,
        long sizeBytes,
        String storagePath,
        Optional<String> sha256,
        Instant uploadedAt,
        Optional<String> uploadedBy) {

    public ProcedureAttachment {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(procedureId, "procedureId");
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(storagePath, "storagePath");
        Objects.requireNonNull(uploadedAt, "uploadedAt");
        if (fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must be >= 0");
        }
        requirementKey = orEmpty(requirementKey);
        contentType = orEmpty(contentType);
        sha256 = orEmpty(sha256);
        uploadedBy = orEmpty(uploadedBy);
    }

    private static Optional<String> orEmpty(Optional<String> v) {
        return v == null ? Optional.empty() : v;
    }
}
