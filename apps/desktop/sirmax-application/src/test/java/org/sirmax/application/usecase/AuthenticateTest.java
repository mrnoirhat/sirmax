// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sirmax.application.fakes.Fakes;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.security.AppUser;
import org.sirmax.domain.security.AppUserStatus;
import org.sirmax.domain.security.PasswordHash;
import org.sirmax.domain.security.Permission;
import org.sirmax.domain.security.Role;
import org.sirmax.shared.Result;

class AuthenticateTest {

    private static final Instant NOW = Instant.parse("2026-01-02T10:00:00Z");

    private final Fakes.InMemoryUsers users = new Fakes.InMemoryUsers();
    private final Fakes.InMemoryRoles roles = new Fakes.InMemoryRoles(users);
    private final Fakes.RecordingAuditSink audit = new Fakes.RecordingAuditSink();
    private final Fakes.SeqIds ids = new Fakes.SeqIds();
    private final Fakes.FixedClock clock = new Fakes.FixedClock(NOW);

    private Authenticate authenticate;

    @BeforeEach
    void setUp() {
        roles.add(
                new Role("r-op", "OPERADOR", "", true, Set.of(Permission.PERSON_READ, Permission.PROCEDURE_WORK)));
        AppUser op =
                AppUser.create(
                        "u1",
                        "op1",
                        "Operador Uno",
                        new PasswordHash("FAKE", "h:correct-horse"),
                        null,
                        NOW.minusSeconds(3600));
        users.save(op);
        users.replaceRoles("u1", Set.of("r-op"));

        authenticate =
                new Authenticate(
                        users,
                        roles,
                        new Fakes.ReversibleHasher(),
                        ids,
                        clock,
                        new Audit(audit, clock, ids));
    }

    private Result<Session> tryLogin(String user, String pass) {
        return authenticate.execute(new Authenticate.Command(user, pass.toCharArray(), "test"));
    }

    @Test
    void validCredentialsOpenASessionWithTheUnionOfRolePermissions() {
        Result<Session> r = tryLogin("op1", "correct-horse");

        assertThat(r.isOk()).isTrue();
        Session s = r.orElseThrow();
        assertThat(s.user().username()).isEqualTo("op1");
        assertThat(s.can(Permission.PERSON_READ)).isTrue();
        assertThat(s.can(Permission.PROCEDURE_WORK)).isTrue();
        assertThat(s.can(Permission.INVOICE_VOID)).isFalse();
        assertThat(s.startedAt()).isEqualTo(NOW);
        assertThat(users.findById("u1").orElseThrow().lastLoginAt()).contains(NOW);
        assertThat(audit.actions()).contains("auth.signin");
    }

    @Test
    void wrongPasswordAndUnknownUserBothReturnTheGenericFailure() {
        assertThat(((Result.Err<?>) tryLogin("op1", "nope")).messageKey()).isEqualTo("auth.invalid");
        assertThat(((Result.Err<?>) tryLogin("ghost", "whatever")).messageKey())
                .isEqualTo("auth.invalid");
    }

    @Test
    void disabledAccountIsReportedSpecifically() {
        users.findById("u1").orElseThrow().changeStatus(AppUserStatus.DISABLED, NOW);
        assertThat(((Result.Err<?>) tryLogin("op1", "correct-horse")).messageKey())
                .isEqualTo("auth.account_disabled");
    }

    @Test
    void passwordArrayIsWiped() {
        char[] pw = "correct-horse".toCharArray();
        authenticate.execute(new Authenticate.Command("op1", pw, "test"));
        assertThat(new String(pw)).isEqualTo("\0".repeat(pw.length));
    }
}
