// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.security;

import java.time.Instant;
import java.util.Objects;
import org.sirmax.domain.security.AccessPolicy;
import org.sirmax.domain.security.AppUser;
import org.sirmax.domain.security.Permission;

/**
 * A signed-in operator: the {@link AppUser} plus their effective {@link AccessPolicy} (the union of
 * their roles' permissions) and a session id used for auditing.
 */
public record Session(String sessionId, AppUser user, AccessPolicy policy, Instant startedAt) {

    public Session {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(startedAt, "startedAt");
    }

    public boolean can(Permission permission) {
        return policy.allows(permission);
    }

    /** @throws org.sirmax.domain.security.AccessDeniedException if not permitted */
    public void require(Permission permission) {
        policy.require(permission);
    }

    public AuditContext audit(String source) {
        return new AuditContext(user.id(), sessionId, source);
    }
}
