// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import org.sirmax.application.port.AuditSink;
import org.sirmax.domain.audit.AuditEvent;

/** Append-only {@link AuditSink} backed by the {@code audit_event} table (guarded by triggers). */
public final class SqliteAuditSink implements AuditSink {

    private static final String INSERT =
            "INSERT INTO audit_event"
                    + " (id, when_at, actor_user_id, action, entity_type, entity_id,"
                    + "  before_json, after_json, reason, session_id, source)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final SqliteDatabase db;

    public SqliteAuditSink(SqliteDatabase db) {
        this.db = db;
    }

    @Override
    public void record(AuditEvent e) {
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
                e.source());
    }
}
