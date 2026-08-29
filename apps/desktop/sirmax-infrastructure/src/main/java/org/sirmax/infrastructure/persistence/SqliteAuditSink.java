// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import org.sirmax.application.port.AuditSink;
import org.sirmax.domain.audit.AuditChain;
import org.sirmax.domain.audit.AuditEvent;

/**
 * Append-only {@link AuditSink} backed by the {@code audit_event} table (guarded by triggers).
 *
 * <p>Every entry is hashed against the previous one (master prompt §40). The triggers already refuse
 * UPDATE and DELETE, but a trigger can be dropped by whoever holds the file; the chain cannot be
 * quietly rewritten, because altering one entry invalidates every hash after it.
 *
 * <p>The previous hash is read inside the same write, and SQLite has one writer, so two entries
 * cannot pick up the same predecessor.
 */
public final class SqliteAuditSink implements AuditSink {

    private static final String INSERT =
            "INSERT INTO audit_event"
                    + " (id, when_at, actor_user_id, action, entity_type, entity_id,"
                    + "  before_json, after_json, reason, session_id, source, prev_hash, entry_hash)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    /** The last hash written, in insertion order. rowid is monotonic for an append-only table. */
    private static final String LAST_HASH =
            "SELECT entry_hash FROM audit_event WHERE entry_hash IS NOT NULL"
                    + " ORDER BY rowid DESC LIMIT 1";

    private final SqliteDatabase db;

    public SqliteAuditSink(SqliteDatabase db) {
        this.db = db;
    }

    @Override
    public void record(AuditEvent e) {
        String previous =
                JdbcHelper.queryOne(db.connection(), LAST_HASH, rs -> rs.getString(1))
                        .orElse(AuditChain.GENESIS);
        String hash = AuditChain.hash(e, previous);

        JdbcHelper.update(
                db.connection(),
                INSERT,
                e.id(),
                e.whenAt(),
                e.actorUserId().orElse(null),
                e.action(),
                e.entityType(),
                e.entityId(),
                e.beforeJson().orElse(null),
                e.afterJson().orElse(null),
                e.reason().orElse(null),
                e.sessionId(),
                e.source(),
                previous,
                hash);
    }
}
