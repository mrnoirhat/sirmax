// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.app;

import java.util.Objects;
import java.util.Optional;
import org.sirmax.application.security.Session;
import org.sirmax.domain.security.Permission;

/**
 * Holds the signed-in {@link Session} for the life of the window.
 *
 * <p>Views ask this rather than passing a session down every constructor, and they ask
 * {@link #can(Permission)} before showing an action: a button the operator cannot use should not be
 * on screen at all (master prompt §78).
 */
public final class UiSession {

    private Session session;

    /** {@code true} once someone has signed in. */
    public boolean isSignedIn() {
        return session != null;
    }

    /** The active session; throws if nobody is signed in — views only run behind the login gate. */
    public Session require() {
        if (session == null) {
            throw new IllegalStateException("No operator is signed in");
        }
        return session;
    }

    public Optional<Session> current() {
        return Optional.ofNullable(session);
    }

    /** The operator's display name, for the top bar. */
    public String displayName() {
        return session == null ? "" : session.user().displayName();
    }

    public boolean can(Permission permission) {
        return session != null && session.can(permission);
    }

    public void signIn(Session newSession) {
        this.session = Objects.requireNonNull(newSession, "session");
    }

    public void signOut() {
        this.session = null;
    }
}
