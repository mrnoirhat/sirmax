// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.security;

import java.util.Objects;
import java.util.Optional;

/**
 * The "who / where from" a use case records on the audit events it emits.
 *
 * @param actorUserId the acting user, or empty for system actions (e.g. first-run provisioning)
 * @param sessionId originating session
 * @param source device / host / channel descriptor
 */
public record AuditContext(Optional<String> actorUserId, String sessionId, String source) {

    public AuditContext {
        actorUserId = actorUserId == null ? Optional.empty() : actorUserId;
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(source, "source");
    }

    public AuditContext(String actorUserId, String sessionId, String source) {
        this(Optional.ofNullable(actorUserId), sessionId, source);
    }

    /** A context for actions that run without a signed-in user (install/bootstrap). */
    public static AuditContext system(String source) {
        return new AuditContext(Optional.empty(), "system", source);
    }
}
