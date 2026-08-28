// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.security;

/** Account state for an {@link AppUser}. */
public enum AppUserStatus {
    /** Can sign in. */
    ACTIVE,
    /** Deactivated by an administrator; cannot sign in. */
    DISABLED,
    /** Temporarily locked (e.g. failed sign-in attempts); cannot sign in until unlocked. */
    LOCKED;

    public boolean canSignIn() {
        return this == ACTIVE;
    }
}
