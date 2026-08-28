// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.security;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * The effective permissions of a signed-in user: the union of the permissions of all their roles.
 *
 * <p>Authorization checks in the application layer go through {@link #allows(Permission)} /
 * {@link #require(Permission)}.
 */
public final class AccessPolicy {

    private static final AccessPolicy NONE = new AccessPolicy(EnumSet.noneOf(Permission.class));

    private final EnumSet<Permission> permissions;

    private AccessPolicy(EnumSet<Permission> permissions) {
        this.permissions = permissions;
    }

    public static AccessPolicy none() {
        return NONE;
    }

    public static AccessPolicy of(Set<Permission> permissions) {
        return permissions.isEmpty()
                ? NONE
                : new AccessPolicy(EnumSet.copyOf(permissions));
    }

    /** The union of every role's permissions. */
    public static AccessPolicy fromRoles(Collection<Role> roles) {
        EnumSet<Permission> all = EnumSet.noneOf(Permission.class);
        for (Role r : roles) {
            all.addAll(r.permissions());
        }
        return all.isEmpty() ? NONE : new AccessPolicy(all);
    }

    public boolean allows(Permission permission) {
        return permissions.contains(permission);
    }

    /** @throws AccessDeniedException if the permission is not granted */
    public void require(Permission permission) {
        if (!allows(permission)) {
            throw new AccessDeniedException(permission);
        }
    }

    public Set<Permission> permissions() {
        return EnumSet.copyOf(permissions); // EnumSet overload — safe even when empty
    }
}
