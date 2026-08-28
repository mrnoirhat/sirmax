// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.sirmax.infrastructure.persistence.JdbcHelper.bool;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sirmax.application.port.RoleRepository;
import org.sirmax.domain.security.Permission;
import org.sirmax.domain.security.Role;

public final class SqliteRoleRepository implements RoleRepository {

    private final SqliteDatabase db;

    public SqliteRoleRepository(SqliteDatabase db) {
        this.db = db;
    }

    @Override
    public void save(Role role) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO role (id, name, description, is_system, created_at)"
                    + " VALUES (?, ?, ?, ?, ?)"
                    + " ON CONFLICT(id) DO UPDATE SET name=excluded.name,"
                    + " description=excluded.description",
                role.id(),
                role.name(),
                role.description(),
                role.isSystem() ? 1 : 0,
                java.time.Instant.now());
        JdbcHelper.update(db.connection(), "DELETE FROM role_permission WHERE role_id = ?", role.id());
        for (Permission p : role.permissions()) {
            JdbcHelper.update(
                    db.connection(),
                    "INSERT INTO role_permission (role_id, permission_key) VALUES (?, ?)",
                    role.id(),
                    p.key());
        }
    }

    @Override
    public Optional<Role> findById(String id) {
        return loadOne("SELECT * FROM role WHERE id = ?", id);
    }

    @Override
    public Optional<Role> findByName(String name) {
        return loadOne("SELECT * FROM role WHERE lower(name) = lower(?)", name);
    }

    @Override
    public List<Role> list() {
        List<String> ids =
                JdbcHelper.queryList(
                        db.connection(),
                        "SELECT id FROM role ORDER BY name",
                        rs -> rs.getString(1));
        return ids.stream().map(this::findById).flatMap(Optional::stream).toList();
    }

    @Override
    public List<Role> findAllById(Collection<String> roleIds) {
        return roleIds.stream().map(this::findById).flatMap(Optional::stream).toList();
    }

    @Override
    public List<Role> rolesOf(String userId) {
        List<String> ids =
                JdbcHelper.queryList(
                        db.connection(),
                        "SELECT role_id FROM user_role WHERE user_id = ?",
                        rs -> rs.getString(1),
                        userId);
        return ids.stream().map(this::findById).flatMap(Optional::stream).toList();
    }

    private Optional<Role> loadOne(String sql, Object param) {
        record Head(String id, String name, String description, boolean system) {}
        Optional<Head> head =
                JdbcHelper.queryOne(
                        db.connection(),
                        sql,
                        rs ->
                                new Head(
                                        rs.getString("id"),
                                        rs.getString("name"),
                                        rs.getString("description"),
                                        bool(rs, "is_system")),
                        param);
        if (head.isEmpty()) {
            return Optional.empty();
        }
        Head h = head.get();
        Set<Permission> permissions = permissionsOf(h.id());
        return Optional.of(new Role(h.id(), h.name(), h.description(), h.system(), permissions));
    }

    private Set<Permission> permissionsOf(String roleId) {
        List<String> keys =
                JdbcHelper.queryList(
                        db.connection(),
                        "SELECT permission_key FROM role_permission WHERE role_id = ?",
                        rs -> rs.getString(1),
                        roleId);
        Set<Permission> out = new LinkedHashSet<>();
        for (String k : keys) {
            Permission.fromKey(k).ifPresent(out::add);
        }
        return out.isEmpty() ? EnumSet.noneOf(Permission.class) : out;
    }
}
