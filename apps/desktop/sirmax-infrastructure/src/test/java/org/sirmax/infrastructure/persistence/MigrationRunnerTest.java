// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sirmax.shared.SirmaxException;

class MigrationRunnerTest {

    private Connection connection;
    private final MigrationRunner runner =
            new MigrationRunner(
                    MigrationSource.classpath(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

    @BeforeEach
    void openDb() {
        connection = SqliteConnectionFactory.inMemory().open();
    }

    @AfterEach
    void closeDb() throws SQLException {
        connection.close();
    }

    @Test
    void freshDatabaseGetsEveryMigration() {
        List<Integer> applied = runner.migrate(connection);

        assertThat(applied).contains(1, 2, 3, 4);
        assertThat(intQuery("SELECT count(*) FROM schema_migrations WHERE success = 1"))
                .isGreaterThanOrEqualTo(3);
        assertThat(intQuery("SELECT count(*) FROM permission")).isEqualTo(25);
        // V0003 tables exist and are queryable
        assertThat(intQuery("SELECT count(*) FROM service_category")).isZero();
        assertThat(intQuery("SELECT count(*) FROM service_definition_version")).isZero();
        assertThat(intQuery("SELECT count(*) FROM procedure")).isZero();
        assertThat(intQuery("SELECT count(*) FROM procedure_requirement")).isZero();
        // V0004 seeds the shared case-numbering sequence
        assertThat(intQuery("SELECT count(*) FROM numbering_sequence")).isEqualTo(1);
        // V0004 also adds the folded search key used by citizen search
        assertThat(intQuery("SELECT count(*) FROM person WHERE search_name IS NOT NULL")).isZero();
        assertThat(intQuery("SELECT count(*) FROM role WHERE is_system = 1")).isEqualTo(4);
        assertThat(
                        intQuery(
                                "SELECT count(*) FROM role_permission rp"
                                    + " JOIN role r ON r.id = rp.role_id"
                                    + " WHERE r.name = 'ADMINISTRADOR'"))
                .isEqualTo(25);
    }

    @Test
    void migratingAgainIsANoOp() {
        runner.migrate(connection);
        assertThat(runner.migrate(connection)).isEmpty();
    }

    @Test
    void upgradeAppliesOnlyTheNewMigrations() {
        List<Migration> all = MigrationSource.classpath().load();
        MigrationSource onlyV1 = () -> List.of(all.get(0));

        new MigrationRunner(onlyV1, Clock.systemUTC()).migrate(connection);
        assertThat(intQuery("SELECT count(*) FROM schema_migrations")).isEqualTo(1);

        List<Integer> expectedNew =
                all.stream().map(Migration::version).filter(v -> v > 1).toList();
        List<Integer> upgraded = runner.migrate(connection);
        assertThat(upgraded).isEqualTo(expectedNew).doesNotContain(1);
        assertThat(intQuery("SELECT count(*) FROM permission")).isEqualTo(25);
    }

    @Test
    void checksumDriftInAnAppliedMigrationIsRejected() {
        runner.migrate(connection);

        List<Migration> tampered =
                MigrationSource.classpath().load().stream()
                        .map(
                                m ->
                                        m.version() == 1
                                                ? new Migration(1, m.description(), m.sql() + " ", "deadbeef")
                                                : m)
                        .toList();
        MigrationRunner drifted = new MigrationRunner(() -> tampered, Clock.systemUTC());

        assertThatThrownBy(() -> drifted.validate(connection))
                .isInstanceOf(SirmaxException.class)
                .hasMessageContaining("checksum mismatch");
    }

    @Test
    void introducingAVersionBelowTheHighestAppliedIsRejected() throws SQLException {
        // Simulate a DB where V1 and V3 are applied but V2 never existed.
        try (Statement st = connection.createStatement()) {
            st.execute(
                    "CREATE TABLE schema_migrations (version INTEGER PRIMARY KEY, description TEXT,"
                        + " checksum TEXT NOT NULL, applied_at TEXT NOT NULL, success INTEGER NOT"
                        + " NULL DEFAULT 1)");
            st.execute(
                    "INSERT INTO schema_migrations VALUES (1,'one','c1','2026-01-01T00:00:00Z',1)");
            st.execute(
                    "INSERT INTO schema_migrations VALUES (3,'three','c3','2026-01-01T00:00:00Z',1)");
        }
        List<Migration> source =
                List.of(
                        new Migration(1, "one", "SELECT 1", "c1"),
                        new Migration(2, "two", "SELECT 2", "c2"),
                        new Migration(3, "three", "SELECT 3", "c3"));

        assertThatThrownBy(() -> new MigrationRunner(() -> source, Clock.systemUTC()).migrate(connection))
                .isInstanceOf(SirmaxException.class)
                .hasMessageContaining("Out-of-order");
    }

    @Test
    void auditEventTableIsAppendOnlyAfterMigration() throws SQLException {
        runner.migrate(connection);
        try (Statement st = connection.createStatement()) {
            st.execute(
                    "INSERT INTO audit_event (id, when_at, action, entity_type, entity_id,"
                        + " session_id, source) VALUES ('a','2026-01-01T00:00:00Z','x','T','1','s','d')");

            assertThatThrownBy(() -> st.execute("UPDATE audit_event SET action = 'y'"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("append-only");
            assertThatThrownBy(() -> st.execute("DELETE FROM audit_event"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("append-only");
        }
    }

    private int intQuery(String sql) {
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new SirmaxException("query failed: " + sql, e);
        }
    }
}
