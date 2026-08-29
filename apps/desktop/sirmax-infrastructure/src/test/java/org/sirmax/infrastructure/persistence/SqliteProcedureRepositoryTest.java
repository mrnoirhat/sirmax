// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sirmax.application.port.ProcedureQuery;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.procedure.Priority;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.procedure.ProcedureEvent;
import org.sirmax.domain.procedure.ProcedureEventKind;
import org.sirmax.domain.procedure.ProcedureRequirementItem;
import org.sirmax.domain.procedure.ProcedureStatus;
import org.sirmax.domain.org.Department;
import org.sirmax.domain.org.OrganizationUnit;
import org.sirmax.domain.security.AppUser;
import org.sirmax.domain.security.PasswordHash;
import org.sirmax.domain.service.RequirementDef;
import org.sirmax.domain.service.RequirementKind;
import org.sirmax.domain.service.RequirementStage;
import org.sirmax.domain.service.ServiceCategory;
import org.sirmax.domain.service.ServiceDefinition;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.domain.service.ServiceType;

class SqliteProcedureRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-03-05T13:00:00Z");

    private SqliteDatabase db;
    private SqliteProcedureRepository procedures;
    private SqliteNumberingRepository numbering;

    @BeforeEach
    void setUp() {
        db = SqliteDatabase.openInMemory();
        db.migrate();
        procedures = new SqliteProcedureRepository(db);
        numbering = new SqliteNumberingRepository(db, () -> NOW);

        // Cases carry FKs to the service version, the department and the operator, so seed a
        // minimal but real office rather than working around the constraints.
        SqliteOrganizationRepository org = new SqliteOrganizationRepository(db);
        org.save(OrganizationUnit.create("org-1", "Ayuntamiento", "Santiago", "DO", NOW));
        org.save(Department.create("dep-1", "org-1", "Certificaciones", "CERT", NOW));
        new SqliteUserRepository(db)
                .save(
                        AppUser.create(
                                "u-1", "op", "Operadora", new PasswordHash("PBKDF2", "x"), "dep-1", NOW));

        SqliteServiceCatalogRepository catalog = new SqliteServiceCatalogRepository(db);
        catalog.saveCategory(ServiceCategory.create("cat-1", "CERT", "Certificaciones", 1, NOW));
        catalog.saveDefinition(
                ServiceDefinition.create(
                        "svc-1", "CERT-RES", "cat-1", "Certificado", ServiceType.GRATUITO, "DO", NOW));
        catalog.saveVersion(ServiceDefinitionVersion.draft("ver-1", "svc-1", 1, NOW));
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private Procedure save(String id, String code, ProcedureStatus status, Priority priority) {
        Procedure p =
                Procedure.open(
                        id,
                        code,
                        "svc-1",
                        "ver-1",
                        PartyRef.person("per-1"),
                        "recepcion",
                        LocalDate.of(2026, 3, 10),
                        NOW);
        p.setPriority(priority, NOW);
        if (status == ProcedureStatus.CLOSED) {
            p.close(NOW);
        }
        procedures.save(p);
        return p;
    }

    @Test
    void aCaseSurvivesTheRoundTripIncludingItsDecision() {
        Procedure p = save("p-1", "TRM-2026-000001", ProcedureStatus.OPEN, Priority.HIGH);
        p.assign("dep-1", null, NOW);
        p.decide(
                org.sirmax.domain.procedure.ProcedureOutcome.REJECTED, "Fuera del municipio", NOW);
        procedures.save(p);

        Procedure loaded = procedures.findByCode("TRM-2026-000001").orElseThrow();

        assertThat(loaded.id()).isEqualTo("p-1");
        assertThat(loaded.status()).isEqualTo(ProcedureStatus.REJECTED);
        assertThat(loaded.priority()).isEqualTo(Priority.HIGH);
        assertThat(loaded.outcomeReason()).contains("Fuera del municipio");
        assertThat(loaded.dueDate()).contains(LocalDate.of(2026, 3, 10));
        assertThat(loaded.currentStepKey()).isEmpty();
    }

    @Test
    void theDefaultWorklistExcludesTerminalCasesAndSortsUrgentFirst() {
        save("p-1", "TRM-2026-000001", ProcedureStatus.OPEN, Priority.NORMAL);
        save("p-2", "TRM-2026-000002", ProcedureStatus.OPEN, Priority.URGENT);
        save("p-3", "TRM-2026-000003", ProcedureStatus.CLOSED, Priority.URGENT);

        List<Procedure> queue = procedures.search(ProcedureQuery.openWork(20));

        assertThat(queue).extracting(Procedure::id).containsExactly("p-2", "p-1");
        assertThat(procedures.countSearch(ProcedureQuery.openWork(20))).isEqualTo(2);
    }

    @Test
    void unassignedAndAssignedQueriesPartitionTheQueue() {
        Procedure mine = save("p-1", "TRM-2026-000001", ProcedureStatus.OPEN, Priority.NORMAL);
        save("p-2", "TRM-2026-000002", ProcedureStatus.OPEN, Priority.NORMAL);
        mine.assign("dep-1", "u-1", NOW);
        procedures.save(mine);

        assertThat(procedures.search(ProcedureQuery.unassigned(20)))
                .extracting(Procedure::id)
                .containsExactly("p-2");
        assertThat(procedures.search(ProcedureQuery.assignedTo("u-1", 20)))
                .extracting(Procedure::id)
                .containsExactly("p-1");

        ProcedureQuery byDepartment =
                new ProcedureQuery(
                        Optional.empty(),
                        List.of(),
                        Optional.of("dep-1"),
                        Optional.empty(),
                        Optional.empty(),
                        false,
                        false,
                        20,
                        0);
        assertThat(procedures.search(byDepartment)).extracting(Procedure::id).containsExactly("p-1");
    }

    @Test
    void theChecklistRoundTripsIncludingWaiversAndConditions() {
        save("p-1", "TRM-2026-000001", ProcedureStatus.OPEN, Priority.NORMAL);
        ProcedureRequirementItem conditional =
                ProcedureRequirementItem.from(
                        "r-1",
                        "p-1",
                        new RequirementDef(
                                "titulo",
                                "Título de propiedad",
                                RequirementKind.DOCUMENT,
                                RequirementStage.REVIEW,
                                true,
                                Optional.of("tipo == 'propietario'"),
                                Optional.empty()));
        procedures.saveRequirement(conditional);

        conditional.waive("u-1", "Presentó contrato de alquiler", NOW);
        procedures.saveRequirement(conditional);

        ProcedureRequirementItem loaded =
                procedures.findRequirement("p-1", "titulo").orElseThrow();

        assertThat(loaded.isWaived()).isTrue();
        assertThat(loaded.isSatisfied()).isTrue();
        assertThat(loaded.note()).contains("Presentó contrato de alquiler");
        assertThat(loaded.conditionExpression()).contains("tipo == 'propietario'");
        assertThat(loaded.stage()).isEqualTo(RequirementStage.REVIEW);
    }

    @Test
    void formValuesAndTimelineEntriesPersist() {
        save("p-1", "TRM-2026-000001", ProcedureStatus.OPEN, Priority.NORMAL);

        procedures.saveFormValues("p-1", Map.of("direccion", "C/ Duarte 12"));
        procedures.saveFormValues("p-1", Map.of("direccion", "C/ Mella 8", "anios", "3"));
        procedures.appendEvent(
                ProcedureEvent.of("e-1", "p-1", ProcedureEventKind.OPENED, "u-1", "Certificado", NOW));
        procedures.appendEvent(
                ProcedureEvent.stepChange("e-2", "p-1", "u-1", "recepcion", "revision", NOW));

        assertThat(procedures.findFormValues("p-1"))
                .containsEntry("direccion", "C/ Mella 8")
                .containsEntry("anios", "3");
        assertThat(procedures.findEvents("p-1"))
                .extracting(ProcedureEvent::kind)
                .containsExactly(ProcedureEventKind.OPENED, ProcedureEventKind.STEP_ADVANCED);
        assertThat(procedures.findEvents("p-1").get(1).toStepKey()).contains("revision");
    }

    @Test
    void numbersComeOutOfTheSequenceInOrderAndSurviveAReload() {
        assertThat(numbering.allocate("TRM", "TRM", 2026)).isEqualTo("TRM-2026-000001");
        assertThat(numbering.allocate("TRM", "TRM", 2026)).isEqualTo("TRM-2026-000002");

        assertThat(numbering.findByCode("TRM").orElseThrow().nextValue()).isEqualTo(3L);
        assertThat(numbering.allocate("FACT", "FACT", 2026)).isEqualTo("FACT-2026-000001");
    }
}
