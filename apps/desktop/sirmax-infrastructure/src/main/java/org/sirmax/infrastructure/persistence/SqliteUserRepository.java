// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.sirmax.infrastructure.persistence.JdbcHelper.instant;
import static org.sirmax.infrastructure.persistence.JdbcHelper.str;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sirmax.application.port.UserRepository;
import org.sirmax.domain.security.AppUser;
import org.sirmax.domain.security.AppUserStatus;
import org.sirmax.domain.security.PasswordHash;

public final class SqliteUserRepository implements UserRepository {

    private final SqliteDatabase db;

    public SqliteUserRepository(SqliteDatabase db) {
        this.db = db;
    }

    @Override
    public void save(AppUser u) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO app_user"
                    + " (id, username, display_name, password_hash, password_algo, status,"
                    + "  department_id, created_at, updated_at, last_login_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT(id) DO UPDATE SET display_name=excluded.display_name,"
                    + " password_hash=excluded.password_hash, password_algo=excluded.password_algo,"
                    + " status=excluded.status, department_id=excluded.department_id,"
                    + " updated_at=excluded.updated_at, last_login_at=excluded.last_login_at",
                u.id(),
                u.username(),
                u.displayName(),
                u.passwordHash().value(),
                u.passwordHash().algorithm(),
                u.status().name(),
                u.departmentId().orElse(null),
                u.createdAt(),
                u.updatedAt(),
                u.lastLoginAt().map(Instant::toString).orElse(null));
    }

    @Override
    public Optional<AppUser> findById(String id) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM app_user WHERE id = ?",
                SqliteUserRepository::mapUser,
                id);
    }

    @Override
    public Optional<AppUser> findByUsername(String username) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM app_user WHERE lower(username) = lower(?)",
                SqliteUserRepository::mapUser,
                username);
    }

    @Override
    public List<AppUser> list() {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM app_user ORDER BY display_name",
                SqliteUserRepository::mapUser);
    }

    @Override
    public long count() {
        return JdbcHelper.queryLong(db.connection(), "SELECT count(*) FROM app_user");
    }

    @Override
    public Set<String> roleIdsOf(String userId) {
        return new LinkedHashSet<>(
                JdbcHelper.queryList(
                        db.connection(),
                        "SELECT role_id FROM user_role WHERE user_id = ?",
                        rs -> rs.getString(1),
                        userId));
    }

    @Override
    public void replaceRoles(String userId, Set<String> roleIds) {
        JdbcHelper.update(db.connection(), "DELETE FROM user_role WHERE user_id = ?", userId);
        for (String roleId : roleIds) {
            JdbcHelper.update(
                    db.connection(),
                    "INSERT INTO user_role (user_id, role_id) VALUES (?, ?)",
                    userId,
                    roleId);
        }
    }

    private static AppUser mapUser(ResultSet rs) throws SQLException {
        String lastLogin = str(rs, "last_login_at");
        return new AppUser(
                rs.getString("id"),
                rs.getString("username"),
                rs.getString("display_name"),
                new PasswordHash(rs.getString("password_algo"), rs.getString("password_hash")),
                AppUserStatus.valueOf(rs.getString("status")),
                str(rs, "department_id"),
                instant(rs, "created_at"),
                instant(rs, "updated_at"),
                lastLogin == null ? null : Instant.parse(lastLogin));
    }
}
