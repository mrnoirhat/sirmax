// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.sirmax.infrastructure.persistence.JdbcHelper.instant;
import static org.sirmax.infrastructure.persistence.JdbcHelper.str;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sirmax.application.port.AuditRepository;
import org.sirmax.domain.audit.AuditChain;
import org.sirmax.domain.audit.AuditEvent;

/** Read-only access to the append-only {@code audit_event} table. */
public final class SqliteAuditRepository implements AuditRepository {

    private final SqliteDatabase db;

    public SqliteAuditRepository(SqliteDatabase db) {
        this.db = db;
    }

    @Override
    public List<AuditEvent> search(
            Optional<String> entityType,
            Optional<String> entityId,
            Optional<String> actorUserId,
            Optional<Instant> from,
            Optional<Instant> to,
            int limit,
            int offset) {
        List<String> clauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        entityType.ifPresent(
                v -> {
                    clauses.add("entity_type = ?");
                    params.add(v);
                });
        entityId.ifPresent(
                v -> {
                    clauses.add("entity_id = ?");
                    params.add(v);
                });
        actorUserId.ifPresent(
                v -> {
                    clauses.add("actor_user_id = ?");
                    params.add(v);
                });
        from.ifPresent(
                v -> {
                    clauses.add("when_at >= ?");
                    params.add(v);
                });
        to.ifPresent(
                v -> {
                    clauses.add("when_at <= ?");
                    params.add(v);
                });

        String where = clauses.isEmpty() ? "1 = 1" : String.join(" AND ", clauses);
        params.add(limit);
        params.add(offset);

        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM audit_event WHERE " + where
                        + " ORDER BY when_at DESC, rowid DESC LIMIT ? OFFSET ?",
                SqliteAuditRepository::map,
                params.toArray());
    }

    @Override
    public List<AuditEvent> forEntity(String entityType, String entityId) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM audit_event WHERE entity_type = ? AND entity_id = ?"
                        + " ORDER BY when_at, rowid",
                SqliteAuditRepository::map,
                entityType,
                entityId);
    }

    @Override
    public List<AuditChain.Entry> chainEntries(int limit, int offset) {
        // rowid order, not when_at: the chain is built in insertion order, and two entries can
        // share a timestamp. Verifying them in a different order would report a false break.
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM audit_event ORDER BY rowid LIMIT ? OFFSET ?",
                SqliteAuditRepository::mapEntry,
                limit,
                offset);
    }

    @Override
    public long count() {
        return JdbcHelper.queryLong(db.connection(), "SELECT count(*) FROM audit_event");
    }

    private static AuditChain.Entry mapEntry(ResultSet rs) throws SQLException {
        return new AuditChain.Entry(
                map(rs),
                Optional.ofNullable(str(rs, "prev_hash")),
                Optional.ofNullable(str(rs, "entry_hash")));
    }

    private static AuditEvent map(ResultSet rs) throws SQLException {
        return new AuditEvent(
                rs.getString("id"),
                instant(rs, "when_at"),
                Optional.ofNullable(str(rs, "actor_user_id")),
                rs.getString("action"),
                rs.getString("entity_type"),
                rs.getString("entity_id"),
                Optional.ofNullable(str(rs, "before_json")),
                Optional.ofNullable(str(rs, "after_json")),
                Optional.ofNullable(str(rs, "reason")),
                rs.getString("session_id"),
                rs.getString("source"));
    }
}
