// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sirmax.application.usecase.Authenticate;
import org.sirmax.application.usecase.ProvisionInitialAdmin;
import org.sirmax.application.usecase.RegisterPerson;
import org.sirmax.domain.identity.IdentificationType;
import org.sirmax.domain.security.Permission;
import org.sirmax.infrastructure.persistence.SqliteDatabase;
import org.sirmax.shared.Result;

/**
 * End-to-end wiring check for Phase 3: the real {@link CompositionRoot} against an in-memory SQLite
 * database (with the real migrations) — provision the first admin, sign in, and register a citizen.
 */
class ProvisionAndAuthenticateIT {

    private SqliteDatabase db;
    private CompositionRoot app;

    @BeforeEach
    void setUp() {
        db = SqliteDatabase.openInMemory();
        app = CompositionRoot.bootstrap(db);
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void provisionThenSignInThenRegisterAPerson() {
        assertThat(app.needsInitialSetup()).isTrue();

        Result<ProvisionInitialAdmin.Provisioned> provisioned =
                app.provisionInitialAdmin()
                        .execute(
                                new ProvisionInitialAdmin.Command(
                                        "Ayuntamiento de Prueba",
                                        "Prueba",
                                        "DO",
                                        "admin",
                                        "Administradora",
                                        "super-secret-1".toCharArray()));
        assertThat(provisioned.isOk()).isTrue();
        assertThat(app.needsInitialSetup()).isFalse();
        assertThat(app.organizationRepository().findActive()).isPresent();

        Result<org.sirmax.application.security.Session> login =
                app.authenticate()
                        .execute(new Authenticate.Command("admin", "super-secret-1".toCharArray(), "it"));
        assertThat(login.isOk()).isTrue();
        var session = login.orElseThrow();
        assertThat(session.policy().permissions()).hasSize(Permission.values().length);

        Result<org.sirmax.domain.identity.Person> person =
                app.registerPerson()
                        .execute(
                                new RegisterPerson.Command(
                                        session,
                                        "Ana",
                                        "Reyes",
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.of(IdentificationType.CEDULA),
                                        Optional.of("001-7654321-0"),
                                        "it"));
        assertThat(person.isOk()).isTrue();
        assertThat(app.personRepository().countSearch("reyes")).isEqualTo(1);

        // wrong password is rejected and does not leak which part failed
        Result<org.sirmax.application.security.Session> bad =
                app.authenticate()
                        .execute(new Authenticate.Command("admin", "wrong".toCharArray(), "it"));
        assertThat(bad.isErr()).isTrue();
    }

    @Test
    void provisioningRefusesOnASecondRun() {
        var cmd =
                new ProvisionInitialAdmin.Command(
                        "Org", "M", "DO", "admin", "Admin", "super-secret-1".toCharArray());
        app.provisionInitialAdmin().execute(cmd);
        Result<ProvisionInitialAdmin.Provisioned> again =
                app.provisionInitialAdmin()
                        .execute(
                                new ProvisionInitialAdmin.Command(
                                        "Org2", "M2", "DO", "admin2", "Admin2",
                                        "super-secret-2".toCharArray()));
        assertThat(again.isErr()).isTrue();
    }
}
