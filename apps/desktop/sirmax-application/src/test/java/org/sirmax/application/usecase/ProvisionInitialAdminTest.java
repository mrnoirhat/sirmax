// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sirmax.application.fakes.Fakes;
import org.sirmax.application.security.Audit;
import org.sirmax.domain.security.Permission;
import org.sirmax.domain.security.Role;
import org.sirmax.shared.Result;

class ProvisionInitialAdminTest {

    private final Fakes.InMemoryUsers users = new Fakes.InMemoryUsers();
    private final Fakes.InMemoryRoles roles = new Fakes.InMemoryRoles(users);
    private final Fakes.InMemoryOrganizations orgs = new Fakes.InMemoryOrganizations();
    private final Fakes.RecordingAuditSink audit = new Fakes.RecordingAuditSink();
    private final Fakes.SeqIds ids = new Fakes.SeqIds();
    private final Fakes.FixedClock clock = new Fakes.FixedClock(Instant.parse("2026-01-01T00:00:00Z"));

    private ProvisionInitialAdmin useCase;

    @BeforeEach
    void setUp() {
        roles.add(new Role("role-admin", "ADMINISTRADOR", "", true, Set.of(Permission.CONFIG_MANAGE)));
        useCase =
                new ProvisionInitialAdmin(
                        users,
                        roles,
                        orgs,
                        new Fakes.ReversibleHasher(),
                        ids,
                        clock,
                        new Fakes.DirectUnitOfWork(),
                        new Audit(audit, clock, ids));
    }

    private ProvisionInitialAdmin.Command command() {
        return new ProvisionInitialAdmin.Command(
                "Ayuntamiento de Ejemplo",
                "Ejemplo",
                "DO",
                "admin",
                "Administradora",
                "s3cret-pass".toCharArray());
    }

    @Test
    void firstRunCreatesOrganizationAdminAndRoleAssignment() {
        Result<ProvisionInitialAdmin.Provisioned> r = useCase.execute(command());

        assertThat(r.isOk()).isTrue();
        ProvisionInitialAdmin.Provisioned p = r.orElseThrow();
        assertThat(orgs.units).containsKey(p.organizationUnitId());
        assertThat(orgs.profiles).containsKey(p.organizationUnitId());
        assertThat(users.findById(p.adminUserId())).isPresent();
        assertThat(users.roleIdsOf(p.adminUserId())).containsExactly("role-admin");
        assertThat(audit.actions()).contains("organization.provisioned", "user.created");
    }

    @Test
    void secondRunIsRefused() {
        useCase.execute(command());
        Result<ProvisionInitialAdmin.Provisioned> again = useCase.execute(command());
        assertThat(again.isErr()).isTrue();
        assertThat(((Result.Err<?>) again).messageKey()).isEqualTo("provision.already_done");
    }

    @Test
    void weakPasswordIsRejected() {
        ProvisionInitialAdmin.Command weak =
                new ProvisionInitialAdmin.Command(
                        "Org", "M", "DO", "admin", "Admin", "short".toCharArray());
        Result<ProvisionInitialAdmin.Provisioned> r = useCase.execute(weak);
        assertThat(((Result.Err<?>) r).messageKey()).isEqualTo("provision.weak_password");
        assertThat(users.count()).isZero();
    }

    @Test
    void passwordArrayIsWipedAfterUse() {
        char[] pw = "s3cret-pass".toCharArray();
        useCase.execute(
                new ProvisionInitialAdmin.Command("Org", "M", "DO", "admin", "Admin", pw));
        assertThat(new String(pw)).isEqualTo("\0".repeat(pw.length));
    }
}
