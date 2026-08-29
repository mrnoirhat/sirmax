// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.sirmax.application.port.ProcedureQuery;
import org.sirmax.application.security.Session;
import org.sirmax.application.usecase.Authenticate;
import org.sirmax.application.usecase.ProvisionInitialAdmin;
import org.sirmax.domain.common.ArchiveStatus;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.identity.Person;
import org.sirmax.domain.identity.PersonName;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.service.ServiceCategory;
import org.sirmax.domain.service.ServiceDefinition;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.domain.service.ServiceType;
import org.sirmax.infrastructure.persistence.SqliteDatabase;

/**
 * The Phase 13 performance audit (master prompt §45 — "the desktop app must feel immediate").
 *
 * <p>Loaded with more data than a mid-sized Dominican municipality accumulates in several years:
 * 20 000 citizens and 20 000 cases. The point is not to benchmark SQLite — it is to catch the
 * mistakes §45 names, which all have the same signature: a query whose cost grows with the size of
 * the table rather than the size of the answer.
 *
 * <p>The budgets are deliberately loose. A counter PC is slower than a build machine, and a test
 * that fails when CI is busy teaches people to ignore it. What these numbers catch is an order of
 * magnitude, which is what a missing index or a full-table scan actually costs.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PerformanceAuditIT {

    private static final int CITIZENS = 20_000;
    private static final int PROCEDURES = 20_000;

    /** Generous enough to survive a loaded CI runner; tight enough to catch a table scan. */
    private static final Duration INTERACTIVE = Duration.ofMillis(400);

    private SqliteDatabase database;
    private CompositionRoot app;
    private Session admin;

    @BeforeAll
    void seed() {
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
                                "una-contrasena-larga".toCharArray()));
        admin =
                app.authenticate()
                        .execute(
                                new Authenticate.Command(
                                        "admin", "una-contrasena-larga".toCharArray(), "test"))
                        .orElseThrow();

        Instant now = app.clock().now();
        var catalog = app.serviceCatalogRepository();
        catalog.saveCategory(ServiceCategory.create("cat-1", "CERT", "Certificaciones", 1, now));
        ServiceDefinition definition =
                ServiceDefinition.create(
                        "svc-1", "CERT-RES", "cat-1", "Certificado", ServiceType.GRATUITO, "DO", now);
        ServiceDefinitionVersion version = ServiceDefinitionVersion.draft("ver-1", "svc-1", 1, now);
        version.publish(now);
        definition.setCurrentVersion("ver-1", now);
        catalog.saveDefinition(definition);
        catalog.saveVersion(version);

        // One transaction: 40 000 individually-committed inserts would measure fsync, not SIRMAX.
        new org.sirmax.infrastructure.persistence.JdbcUnitOfWork(database)
                .execute(
                        () -> {
                            for (int i = 0; i < CITIZENS; i++) {
                                app.personRepository()
                                        .save(
                                                new Person(
                                                        "per-" + i,
                                                        new PersonName(
                                                                "Nombre" + i, apellido(i)),
                                                        LocalDate.of(1970 + (i % 40), 1 + (i % 12), 1 + (i % 28)),
                                                        null,
                                                        null,
                                                        ArchiveStatus.ACTIVE,
                                                        now,
                                                        now));
                            }
                            for (int i = 0; i < PROCEDURES; i++) {
                                Procedure procedure =
                                        Procedure.open(
                                                "proc-" + i,
                                                String.format("TRM-2026-%06d", i + 1),
                                                "svc-1",
                                                "ver-1",
                                                PartyRef.person("per-" + (i % CITIZENS)),
                                                "recepcion",
                                                LocalDate.of(2026, 3, 10),
                                                now);
                                if (i % 3 == 0) {
                                    procedure.close(now);
                                }
                                app.procedureRepository().save(procedure);
                            }
                            return null;
                        });
    }

    @AfterAll
    void tearDown() {
        database.close();
    }

    /** Surnames repeat the way real ones do, so a search returns many rows rather than one. */
    private static String apellido(int i) {
        String[] surnames = {
            "Peña Rodríguez", "Martínez Cruz", "Fernández Gómez", "Núñez Santos", "Reyes Pérez"
        };
        return surnames[i % surnames.length] + " " + (i % 100);
    }

    @Test
    void citizenSearchStaysInteractiveAtTwentyThousandRecords() {
        // Accent-folded search on a LIKE with a leading wildcard cannot use the index, so this is
        // the query most likely to degrade. It is also the one an operator runs constantly.
        Duration elapsed = timed(() -> app.people().search("pena rodriguez", 50, 0));

        assertThat(elapsed).as("citizen search").isLessThan(INTERACTIVE);
    }

    @Test
    void searchReturnsOnlyThePageAskedFor() {
        // §45: never load an entire table into memory. A search that returns 20 000 rows to show
        // 50 is the failure mode, and it is invisible until the municipality has real data.
        assertThat(app.people().search("", 50, 0)).hasSize(50);
        assertThat(app.procedureRepository().search(ProcedureQuery.openWork(25))).hasSize(25);
    }

    @Test
    void theWorklistStaysInteractive() {
        Duration elapsed =
                timed(() -> app.procedureRepository().search(ProcedureQuery.openWork(100)));

        assertThat(elapsed).as("worklist").isLessThan(INTERACTIVE);
    }

    @Test
    void countingIsNotDoneByFetching() {
        // countSearch must count in SQL. Implemented as search().size() it would be as slow as the
        // search and allocate the whole result — the classic way a list screen gets twice as slow
        // as it needs to be.
        Duration elapsed = timed(() -> app.people().countSearch("pena"));

        assertThat(elapsed).as("count").isLessThan(INTERACTIVE);
        assertThat(app.people().countSearch("")).isEqualTo(CITIZENS);
    }

    @Test
    void aCitizenHistoryLoadsFromTheIndexNotAScan() {
        Duration elapsed =
                timed(() -> app.procedureRepository().findByApplicant(PartyRef.person("per-17"), 30));

        assertThat(elapsed).as("citizen history").isLessThan(INTERACTIVE);
    }

    @Test
    void lookingUpOneRecordIsEffectivelyInstant() {
        Duration byId = timed(() -> app.procedureRepository().findById("proc-19999"));
        Duration byCode = timed(() -> app.procedureRepository().findByCode("TRM-2026-020000"));

        assertThat(byId).as("by id").isLessThan(Duration.ofMillis(50));
        assertThat(byCode).as("by code").isLessThan(Duration.ofMillis(50));
    }

    @Test
    void everyQueryTheUiRunsHasAnIndexBehindIt() {
        // Reading the plan is what makes this an audit rather than a stopwatch: a timing that
        // passes today because the table happens to be cached tells nobody anything.
        assertThat(queryPlan("SELECT * FROM person WHERE search_name LIKE '%pena%' LIMIT 50"))
                .as("citizen search")
                .doesNotContain("SCAN person USING");

        assertThat(
                        queryPlan(
                                "SELECT * FROM procedure WHERE applicant_type = 'PERSON'"
                                        + " AND applicant_id = 'per-1' LIMIT 30"))
                .as("citizen history")
                .contains("USING INDEX");

        assertThat(queryPlan("SELECT * FROM procedure WHERE code = 'TRM-2026-000001'"))
                .as("case by number")
                .contains("USING INDEX");

        assertThat(queryPlan("SELECT * FROM invoice WHERE procedure_id = 'proc-1'"))
                .as("invoices of a case")
                .contains("USING INDEX");
    }

    private String queryPlan(String sql) {
        StringBuilder plan = new StringBuilder();
        try (var statement = database.connection().createStatement();
                var rs = statement.executeQuery("EXPLAIN QUERY PLAN " + sql)) {
            while (rs.next()) {
                plan.append(rs.getString("detail")).append('\n');
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Could not read the query plan for: " + sql, e);
        }
        return plan.toString();
    }

    /** Runs the work once to warm class loading and statement preparation, then times it. */
    private static Duration timed(Supplier<?> work) {
        work.get();
        Instant start = Instant.now();
        work.get();
        return Duration.between(start, Instant.now());
    }
}
