// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * A named set of {@link Permission}s.
 *
 * <p>System roles ({@code isSystem() == true}) are seeded and cannot be deleted; their grants may
 * still be adjusted by an administrator with {@code role.manage}.
 */
public final class Role {

    private final String id;
    private String name;
    private String description;
    private final boolean system;
    private final EnumSet<Permission> permissions;

    public Role(
            String id,
            String name,
            String description,
            boolean system,
            Set<Permission> permissions) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.description = description == null ? "" : description;
        this.system = system;
        this.permissions =
                permissions == null || permissions.isEmpty()
                        ? EnumSet.noneOf(Permission.class)
                        : EnumSet.copyOf(permissions);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public boolean isSystem() {
        return system;
    }

    public Set<Permission> permissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public boolean grants(Permission permission) {
        return permissions.contains(permission);
    }

    public void rename(String newName) {
        this.name = requireText(newName, "name");
    }

    public void describe(String newDescription) {
        this.description = newDescription == null ? "" : newDescription;
    }

    public void grant(Permission permission) {
        permissions.add(Objects.requireNonNull(permission, "permission"));
    }

    public void revoke(Permission permission) {
        permissions.remove(permission);
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Role r && id.equals(r.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
