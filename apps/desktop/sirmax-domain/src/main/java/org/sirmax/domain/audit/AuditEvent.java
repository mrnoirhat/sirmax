// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable audit record (master prompt §40).
 *
 * <p>Audit events are append-only and cannot be edited from the regular UI. They are recorded
 * especially for fees, invoices, payments, refunds, approvals, permission changes, configuration
 * changes, backups/restores and official documents.
 *
 * @param id time-ordered identifier
 * @param whenAt event timestamp (UTC)
 * @param actorUserId user who performed the action, if any (system actions may have none)
 * @param action verb, e.g. {@code "invoice.void"}
 * @param entityType e.g. {@code "Invoice"}
 * @param entityId identifier of the affected entity
 * @param beforeJson serialized state before, if applicable
 * @param afterJson serialized state after, if applicable
 * @param reason human-provided reason, if the action requires one
 * @param sessionId originating session
 * @param source device/host/channel descriptor
 */
public record AuditEvent(
        String id,
        Instant whenAt,
        Optional<String> actorUserId,
        String action,
        String entityType,
        String entityId,
        Optional<String> beforeJson,
        Optional<String> afterJson,
        Optional<String> reason,
        String sessionId,
        String source) {

    public AuditEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(whenAt, "whenAt");
        actorUserId = actorUserId == null ? Optional.empty() : actorUserId;
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(entityId, "entityId");
        beforeJson = beforeJson == null ? Optional.empty() : beforeJson;
        afterJson = afterJson == null ? Optional.empty() : afterJson;
        reason = reason == null ? Optional.empty() : reason;
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(source, "source");
    }
}
