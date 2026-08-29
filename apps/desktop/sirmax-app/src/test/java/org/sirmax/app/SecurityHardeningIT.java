// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sirmax.application.security.Session;
import org.sirmax.application.usecase.Authenticate;
import org.sirmax.application.usecase.ProvisionInitialAdmin;
import org.sirmax.application.usecase.RegisterPerson;
import org.sirmax.application.usecase.VerifyAuditIntegrity;
import org.sirmax.domain.audit.AuditChain;
import org.sirmax.domain.security.SecurityPolicy;
import org.sirmax.infrastructure.persistence.SqliteDatabase;
import org.sirmax.shared.Result;

/**
 * Security hardening against the real graph — master prompt §40, §43.
 *
 * <p>Two properties are being proved here, and both are about what happens when someone is trying
 * to get away with something: an account locks after repeated failures, and the audit trail cannot
 * be rewritten without the rewrite showing.
 */
class SecurityHardeningIT {

    private static final char[] PASSWORD = "una-contrasena-larga".toCharArray();

    private SqliteDatabase database;
    private CompositionRoot app;
    private Session admin;

    @BeforeEach
    void setUp() {
        database = SqliteDatabase.openInMemory();
        app = CompositionRoot.bootstrap(database);

        app.provisionInitialAdmin()
                .execute(
                        new ProvisionInitialAdmin.Command(
                                "Ayuntamiento de Santiago",
                                "Santiago",
                                "DO",
                                "admin",
                                "Administradora",
                                PASSWORD.clone()));
        admin = signIn(PASSWORD.clone()).orElseThrow();
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    private Result<Session> signIn(char[] password) {
        return app.authenticate().execute(new Authenticate.Command("admin", password, "test"));
    }

    // ── §43: lockout ──

    @Test
    void anAccountLocksAfterTheConfiguredNumberOfFailures() {
        SecurityPolicy policy = app.securityPolicy().load();

        for (int attempt = 1; attempt < policy.maxFailedAttempts(); attempt++) {
            assertThat(((Result.Err<?>) signIn("incorrecta".toCharArray())).messageKey())
                    .isEqualTo("auth.invalid");
        }
        // The attempt that trips the threshold says so, rather than pretending it was just wrong.
        assertThat(((Result.Err<?>) signIn("incorrecta".toCharArray())).messageKey())
                .isEqualTo("auth.account_locked");

        // And the right password is refused while the lock stands.
        assertThat(((Result.Err<?>) signIn(PASSWORD.clone())).messageKey())
                .isEqualTo("auth.account_locked");
    }

    @Test
    void aSuccessfulSignInClearsTheFailureCount() {
        signIn("incorrecta".toCharArray());
        signIn("incorrecta".toCharArray());
        assertThat(app.userRepository().findByUsername("admin").orElseThrow().failedAttempts())
                .isEqualTo(2);

        assertThat(signIn(PASSWORD.clone()).isOk()).isTrue();

        assertThat(app.userRepository().findByUsername("admin").orElseThrow().failedAttempts())
                .isZero();
    }

    @Test
    void anUnknownUsernameAnswersExactlyLikeAWrongPassword() {
        Result<?> unknown =
                app.authenticate()
                        .execute(
                                new Authenticate.Command(
                                        "no-existe", "cualquiera".toCharArray(), "test"));
        Result<?> wrongPassword = signIn("incorrecta".toCharArray());

        // Distinguishing them would turn the login screen into a staff directory.
        assertThat(((Result.Err<?>) unknown).messageKey())
                .isEqualTo(((Result.Err<?>) wrongPassword).messageKey());
    }

    @Test
    void everyAttemptIsLoggedWithWhatWentWrong() {
        signIn("incorrecta".toCharArray());
        signIn(PASSWORD.clone());

        var attempts = app.securityPolicy().recentAttempts("admin", 10);

        assertThat(attempts).hasSizeGreaterThanOrEqualTo(3); // setup sign-in included
        assertThat(attempts.get(0).succeeded()).isTrue();
        assertThat(attempts.get(1).succeeded()).isFalse();
        assertThat(attempts.get(1).failureKind()).contains("BAD_PASSWORD");
    }

    // ── §40: audit integrity ──

    @Test
    void anUntouchedAuditTrailVerifies() {
        registerCitizen("Ana", "Rodríguez Cruz");

        AuditChain.Verification result =
                app.verifyAuditIntegrity()
                        .execute(new VerifyAuditIntegrity.Command(admin))
                        .orElseThrow();

        assertThat(result.isIntact()).isTrue();
        assertThat(result.verifiedEntries()).isGreaterThan(0);
        assertThat(result.unchainedEntries()).isZero();
    }

    @Test
    void theDatabaseItselfRefusesToRewriteAnAuditEntry() {
        registerCitizen("Ana", "Rodríguez Cruz");

        assertThatThrownBy(() -> execute("UPDATE audit_event SET action = 'nada'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> execute("DELETE FROM audit_event"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("append-only");
    }

    /**
     * The triggers stop SQL, but whoever holds the file can drop them. This is the case the hash
     * chain exists for: tampering still happens, and is still visible.
     */
    @Test
    void droppingTheTriggersAndEditingAnEntryIsStillDetected() throws SQLException {
        registerCitizen("Ana", "Rodríguez Cruz");
        registerCitizen("Pedro", "Martínez");

        execute("DROP TRIGGER audit_event_no_update");
        execute("UPDATE audit_event SET reason = 'autorizado' WHERE rowid = 2");

        AuditChain.Verification result =
                app.verifyAuditIntegrity()
                        .execute(new VerifyAuditIntegrity.Command(admin))
                        .orElseThrow();

        assertThat(result.isIntact()).isFalse();
        assertThat(result.breakKind()).contains(AuditChain.Break.CONTENT);
    }

    @Test
    void deletingAnAuditEntryOutrightIsAlsoDetected() throws SQLException {
        registerCitizen("Ana", "Rodríguez Cruz");
        registerCitizen("Pedro", "Martínez");
        registerCitizen("Luisa", "Fernández");

        execute("DROP TRIGGER audit_event_no_delete");
        execute("DELETE FROM audit_event WHERE rowid = 3");

        AuditChain.Verification result =
                app.verifyAuditIntegrity()
                        .execute(new VerifyAuditIntegrity.Command(admin))
                        .orElseThrow();

        assertThat(result.isIntact()).isFalse();
        assertThat(result.breakKind()).contains(AuditChain.Break.LINK);
    }

    @Test
    void verifyingTheTrailNeedsAuditRead() {
        Session withoutPermission =
                new Session(
                        "s-x",
                        admin.user(),
                        org.sirmax.domain.security.AccessPolicy.of(
                                java.util.EnumSet.noneOf(
                                        org.sirmax.domain.security.Permission.class)),
                        admin.startedAt());

        Result<?> result =
                app.verifyAuditIntegrity()
                        .execute(new VerifyAuditIntegrity.Command(withoutPermission));

        assertThat(((Result.Err<?>) result).messageKey()).isEqualTo("error.forbidden");
    }

    private void registerCitizen(String given, String family) {
        app.registerPerson()
                .execute(
                        new RegisterPerson.Command(
                                admin,
                                given,
                                family,
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                "test"))
                .orElseThrow();
    }

    private void execute(String sql) throws SQLException {
        try (Statement statement = database.connection().createStatement()) {
            statement.execute(sql);
        }
    }
}
