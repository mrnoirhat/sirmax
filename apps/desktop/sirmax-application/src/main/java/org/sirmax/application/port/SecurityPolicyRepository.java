// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.sirmax.domain.security.SecurityPolicy;

/**
 * The installation's security policy and the sign-in attempt log (master prompt §43).
 *
 * <p>Attempts are recorded whether they succeed or fail. Keeping the successes is what lets an
 * administrator tell "she keeps mistyping it on Mondays" from "someone tried nine usernames at
 * 3am" — a failure log alone shows only the second shape of both.
 */
public interface SecurityPolicyRepository {

    SecurityPolicy load();

    void save(SecurityPolicy policy);

    /**
     * Record one sign-in attempt.
     *
     * @param username as typed; the account may not exist, and that is worth knowing
     * @param failureKind null on success, otherwise UNKNOWN_USER / BAD_PASSWORD / LOCKED / DISABLED
     */
    void recordAttempt(
            String id,
            String username,
            String userId,
            boolean succeeded,
            Instant attemptedAt,
            String source,
            String failureKind);

    /** Recent attempts for a username, newest first — what the lockout screen shows. */
    List<Attempt> recentAttempts(String username, int limit);

    record Attempt(
            String id,
            String username,
            Optional<String> userId,
            boolean succeeded,
            Instant attemptedAt,
            String source,
            Optional<String> failureKind) {}
}
