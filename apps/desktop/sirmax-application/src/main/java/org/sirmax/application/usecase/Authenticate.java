// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.util.List;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.PasswordHasher;
import org.sirmax.application.port.SecurityPolicyRepository;
import org.sirmax.application.port.RoleRepository;
import org.sirmax.application.port.UserRepository;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.security.AccessPolicy;
import org.sirmax.domain.security.AppUser;
import org.sirmax.domain.security.Role;
import org.sirmax.shared.Result;

/**
 * Verifies a username/password and opens a {@link Session} carrying the user's effective
 * permissions.
 *
 * <p>A wrong username and a wrong password return the same {@code auth.invalid} outcome so the form
 * does not disclose which accounts exist; a disabled or locked account is reported specifically so
 * the operator knows to ask an administrator.
 */
public final class Authenticate implements UseCase<Authenticate.Command, Session> {

    /** @param password wiped by this use case */
    public record Command(String username, char[] password, String source) {}

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordHasher hasher;
    private final IdGenerator ids;
    private final Clock clock;
    private final Audit audit;
    private final SecurityPolicyRepository securityPolicy; // nullable — see the constructors

    /** Without a policy repository: no lockout and no attempt log. Used by unit tests. */
    public Authenticate(
            UserRepository users,
            RoleRepository roles,
            PasswordHasher hasher,
            IdGenerator ids,
            Clock clock,
            Audit audit) {
        this(users, roles, hasher, ids, clock, audit, null);
    }

    public Authenticate(
            UserRepository users,
            RoleRepository roles,
            PasswordHasher hasher,
            IdGenerator ids,
            Clock clock,
            Audit audit,
            SecurityPolicyRepository securityPolicy) {
        this.users = users;
        this.roles = roles;
        this.hasher = hasher;
        this.ids = ids;
        this.clock = clock;
        this.audit = audit;
        this.securityPolicy = securityPolicy;
    }

    @Override
    public Result<Session> execute(Command command) {
        try {
            Instant now = clock.now();
            AppUser user = users.findByUsername(command.username()).orElse(null);

            // An unknown username and a wrong password answer identically. Distinguishing them
            // would turn the login screen into a way to enumerate who works at the ayuntamiento.
            if (user == null) {
                recordAttempt(command, null, now, "UNKNOWN_USER");
                return Result.err("INVALID_CREDENTIALS", "auth.invalid");
            }
            if (user.isLockedAt(now)) {
                recordAttempt(command, user, now, "LOCKED");
                return Result.err("ACCOUNT_LOCKED", "auth.account_locked");
            }
            if (!hasher.verify(command.password(), user.passwordHash())) {
                boolean nowLocked = registerFailure(user, now);
                recordAttempt(command, user, now, "BAD_PASSWORD");
                return nowLocked
                        ? Result.err("ACCOUNT_LOCKED", "auth.account_locked")
                        : Result.err("INVALID_CREDENTIALS", "auth.invalid");
            }
            if (!user.canSignIn()) {
                recordAttempt(command, user, now, user.status().name());
                return switch (user.status()) {
                    case DISABLED -> Result.err("ACCOUNT_DISABLED", "auth.account_disabled");
                    case LOCKED -> Result.err("ACCOUNT_LOCKED", "auth.account_locked");
                    default -> Result.err("INVALID_CREDENTIALS", "auth.invalid");
                };
            }

            List<Role> userRoles = roles.rolesOf(user.id());
            AccessPolicy policy = AccessPolicy.fromRoles(userRoles);
            Session session = new Session(ids.newId(), user, policy, now);

            user.recordSignIn(now);
            users.save(user);
            recordAttempt(command, user, now, null);
            audit.record(session.audit(command.source()), "auth.signin", "AppUser", user.id());

            return Result.ok(session);
        } finally {
            if (command.password() != null) {
                java.util.Arrays.fill(command.password(), '\0');
            }
        }
    }

    /**
     * Count the failure and lock the account once the policy's threshold is reached.
     *
     * @return {@code true} when this attempt is the one that locked it
     */
    private boolean registerFailure(AppUser user, Instant now) {
        if (securityPolicy == null) {
            return false;
        }
        boolean locked = user.recordFailedSignIn(securityPolicy.load(), now);
        users.save(user);
        return locked;
    }

    /** Every attempt is logged, successful or not — that is what makes the log readable (§43). */
    private void recordAttempt(Command command, AppUser user, Instant now, String failureKind) {
        if (securityPolicy == null) {
            return;
        }
        securityPolicy.recordAttempt(
                ids.newId(),
                command.username(),
                user == null ? null : user.id(),
                failureKind == null,
                now,
                command.source(),
                failureKind);
    }

    /** Re-derive a session's policy (e.g. after roles change) without re-checking the password. */
    public AccessPolicy currentPolicyFor(String userId) {
        return AccessPolicy.fromRoles(roles.rolesOf(userId));
    }
}
