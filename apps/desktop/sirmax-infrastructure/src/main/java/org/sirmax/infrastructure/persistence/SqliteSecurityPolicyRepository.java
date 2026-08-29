// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.sirmax.infrastructure.persistence.JdbcHelper.bool;
import static org.sirmax.infrastructure.persistence.JdbcHelper.instant;
import static org.sirmax.infrastructure.persistence.JdbcHelper.str;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.sirmax.application.port.SecurityPolicyRepository;
import org.sirmax.domain.security.SecurityPolicy;

/** SQLite persistence for the security policy and the sign-in attempt log. */
public final class SqliteSecurityPolicyRepository implements SecurityPolicyRepository {

    private final SqliteDatabase db;

    public SqliteSecurityPolicyRepository(SqliteDatabase db) {
        this.db = db;
    }

    @Override
    public SecurityPolicy load() {
        return JdbcHelper.queryOne(
                        db.connection(),
                        "SELECT * FROM security_policy WHERE id = 1",
                        SqliteSecurityPolicyRepository::map)
                .orElseGet(() -> SecurityPolicy.defaults(Instant.EPOCH));
    }

    @Override
    public void save(SecurityPolicy p) {
        JdbcHelper.update(
                db.connection(),
                "UPDATE security_policy SET min_password_length = ?, max_failed_attempts = ?,"
                        + " lockout_minutes = ?, idle_lock_minutes = ?, session_max_hours = ?,"
                        + " max_attachment_mb = ?, updated_at = ? WHERE id = 1",
                p.minPasswordLength(),
                p.maxFailedAttempts(),
                p.lockoutMinutes(),
                p.idleLockMinutes(),
                p.sessionMaxHours(),
                p.maxAttachmentMb(),
                p.updatedAt());
    }

    @Override
    public void recordAttempt(
            String id,
            String username,
            String userId,
            boolean succeeded,
            Instant attemptedAt,
            String source,
            String failureKind) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO login_attempt"
                        + " (id, username, user_id, succeeded, attempted_at, source, failure_kind)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                id,
                username,
                userId,
                succeeded,
                attemptedAt,
                source,
                failureKind);
    }

    @Override
    public List<Attempt> recentAttempts(String username, int limit) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM login_attempt WHERE lower(username) = lower(?)"
                        + " ORDER BY attempted_at DESC LIMIT ?",
                SqliteSecurityPolicyRepository::mapAttempt,
                username,
                limit);
    }

    private static SecurityPolicy map(ResultSet rs) throws SQLException {
        return new SecurityPolicy(
                rs.getInt("min_password_length"),
                rs.getInt("max_failed_attempts"),
                rs.getInt("lockout_minutes"),
                rs.getInt("idle_lock_minutes"),
                rs.getInt("session_max_hours"),
                rs.getInt("max_attachment_mb"),
                instant(rs, "updated_at"));
    }

    private static Attempt mapAttempt(ResultSet rs) throws SQLException {
        return new Attempt(
                rs.getString("id"),
                rs.getString("username"),
                Optional.ofNullable(str(rs, "user_id")),
                bool(rs, "succeeded"),
                instant(rs, "attempted_at"),
                rs.getString("source"),
                Optional.ofNullable(str(rs, "failure_kind")));
    }
}
