// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.sirmax.domain.audit.AuditChain;
import org.sirmax.domain.audit.AuditEvent;

/**
 * Reading the audit trail (master prompt §40 — audit is a first-class feature, not a debug log).
 *
 * <p>Deliberately separate from {@link AuditSink}: writing is something almost every use case does,
 * reading is a privileged operation gated by {@code audit.read}. Keeping them apart means no use
 * case can accidentally acquire the ability to browse the trail it is writing to.
 *
 * <p>There is no update or delete. The trail is append-only by design and by trigger.
 */
public interface AuditRepository {

    /** Events matching the filters, newest first. Every filter is optional. */
    List<AuditEvent> search(
            Optional<String> entityType,
            Optional<String> entityId,
            Optional<String> actorUserId,
            Optional<Instant> from,
            Optional<Instant> to,
            int limit,
            int offset);

    /** Everything recorded against one entity, oldest first — the entity's own history. */
    List<AuditEvent> forEntity(String entityType, String entityId);

    long count();

    /**
     * The whole trail as chain entries, oldest first — what {@link AuditChain#verify} walks.
     *
     * <p>Returns entries rather than events because verification needs the stored hashes alongside
     * the content: recomputing from the content alone would only prove the content hashes to
     * itself.
     */
    List<AuditChain.Entry> chainEntries(int limit, int offset);
}
