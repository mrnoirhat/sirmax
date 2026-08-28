// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.security;

/**
 * Thrown when an action requires a {@link Permission} the current user does not have.
 *
 * <p>This is a programming/authorization fault raised by {@link AccessPolicy#require}; use cases that
 * want to return a friendly denial instead should check {@link AccessPolicy#allows} first.
 */
public final class AccessDeniedException extends RuntimeException {

    private final transient Permission permission;

    public AccessDeniedException(Permission permission) {
        super("Missing permission: " + permission.key());
        this.permission = permission;
    }

    public Permission permission() {
        return permission;
    }
}
