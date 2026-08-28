// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.util.List;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.PasswordHasher;
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

    public Authenticate(
            UserRepository users,
            RoleRepository roles,
            PasswordHasher hasher,
            IdGenerator ids,
            Clock clock,
            Audit audit) {
        this.users = users;
        this.roles = roles;
        this.hasher = hasher;
        this.ids = ids;
        this.clock = clock;
        this.audit = audit;
    }

    @Override
    public Result<Session> execute(Command command) {
        try {
            AppUser user = users.findByUsername(command.username()).orElse(null);
            if (user == null || !hasher.verify(command.password(), user.passwordHash())) {
                return Result.err("INVALID_CREDENTIALS", "auth.invalid");
            }
            if (!user.canSignIn()) {
                return switch (user.status()) {
                    case DISABLED -> Result.err("ACCOUNT_DISABLED", "auth.account_disabled");
                    case LOCKED -> Result.err("ACCOUNT_LOCKED", "auth.account_locked");
                    default -> Result.err("INVALID_CREDENTIALS", "auth.invalid");
                };
            }

            Instant now = clock.now();
            List<Role> userRoles = roles.rolesOf(user.id());
            AccessPolicy policy = AccessPolicy.fromRoles(userRoles);
            Session session = new Session(ids.newId(), user, policy, now);

            user.recordSignIn(now);
            users.save(user);
            audit.record(session.audit(command.source()), "auth.signin", "AppUser", user.id());

            return Result.ok(session);
        } finally {
            if (command.password() != null) {
                java.util.Arrays.fill(command.password(), '\0');
            }
        }
    }

    /** Re-derive a session's policy (e.g. after roles change) without re-checking the password. */
    public AccessPolicy currentPolicyFor(String userId) {
        return AccessPolicy.fromRoles(roles.rolesOf(userId));
    }
}
