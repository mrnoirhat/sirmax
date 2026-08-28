// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sirmax.domain.finance.ChargeType;
import org.sirmax.domain.finance.FeeRule;
import org.sirmax.domain.service.RequirementDef;
import org.sirmax.domain.service.RequirementStage;
import org.sirmax.domain.service.ServiceCategory;
import org.sirmax.domain.service.ServiceDefinition;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.domain.service.ServiceStatus;
import org.sirmax.domain.service.ServiceType;
import org.sirmax.domain.service.Sla;
import org.sirmax.domain.workflow.StepType;
import org.sirmax.domain.workflow.Transition;
import org.sirmax.domain.workflow.TransitionKind;
import org.sirmax.domain.workflow.WorkflowDefinition;
import org.sirmax.domain.workflow.WorkflowStep;

class SqliteServiceCatalogRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-04-10T09:00:00Z");

    private SqliteDatabase db;
    private SqliteServiceCatalogRepository repo;

    @BeforeEach
    void setUp() {
        db = SqliteDatabase.openInMemory();
        db.migrate();
        repo = new SqliteServiceCatalogRepository(db);
    }

    @AfterEach
    void tearDown() throws SQLException {
        db.connection().close();
    }

    @Test
    void categoryDefinitionAndVersionRoundTripIncludingJsonColumns() {
        repo.saveCategory(ServiceCategory.create("c1", "CERT", "Certificaciones", 1, NOW));
        assertThat(repo.findCategoryByCode("cert")).isPresent();
        assertThat(repo.listActiveCategories()).hasSize(1);

        ServiceDefinition def =
                ServiceDefinition.create(
                        "d1", "CERT-RES", "c1", "Certificado de residencia", ServiceType.CON_TASA, "DO", NOW);
        repo.saveDefinition(def);
        assertThat(repo.findDefinitionByCode("cert-res")).map(ServiceDefinition::name)
                .contains("Certificado de residencia");

        ServiceDefinitionVersion v1 = ServiceDefinitionVersion.draft("v1", "d1", 1, NOW);
        v1.setRequiresPayment(true);
        v1.setSla(new Sla(3, Sla.Basis.BUSINESS_DAYS, java.util.OptionalInt.of(5)));
        v1.setRequirements(
                List.of(
                        RequirementDef.mandatoryDocument("cedula", "Cédula", RequirementStage.INTAKE),
                        RequirementDef.mandatoryDocument(
                                "residencia", "Prueba de residencia", RequirementStage.INTAKE)));
        v1.setFeeRules(
                List.of(
                        FeeRule.fixed(
                                "fee1", ChargeType.TASA, "Certificación", "DOP", 50_000,
                                LocalDate.parse("2026-01-01"))));
        repo.saveVersion(v1);

        ServiceDefinitionVersion loaded = repo.findVersionById("v1").orElseThrow();
        assertThat(loaded.requiresPayment()).isTrue();
        assertThat(loaded.sla().targetDays()).isEqualTo(3);
        assertThat(loaded.sla().escalationThreshold()).contains(5);
        assertThat(loaded.requirements()).hasSize(2);
        assertThat(loaded.requirements().get(0).key()).isEqualTo("cedula");
        assertThat(loaded.requirements().get(0).label()).isEqualTo("Cédula");
        assertThat(loaded.feeRules()).hasSize(1);
        assertThat(loaded.feeRules().get(0).amountMinor()).isEqualTo(50_000);
        assertThat(loaded.feeRules().get(0).concept()).isEqualTo("Certificación");
        assertThat(loaded.status()).isEqualTo(ServiceStatus.DRAFT);
    }

    @Test
    void workflowAndFormSchemaSurviveTheRoundTrip() {
        repo.saveCategory(ServiceCategory.create("c1", "URB", "Urbanismo", 1, NOW));
        repo.saveDefinition(
                ServiceDefinition.create("d1", "DEMOL", "c1", "Demolición", ServiceType.CON_TASA, "DO", NOW));

        WorkflowStep intake =
                WorkflowStep.task("intake", "Recepción", "review");
        WorkflowStep review =
                new WorkflowStep(
                        "review",
                        "Revisión técnica",
                        StepType.REVIEW,
                        java.util.Optional.of("procedure.decide"),
                        3,
                        List.of(
                                new Transition(
                                        TransitionKind.APPROVE,
                                        java.util.Optional.empty(),
                                        java.util.Optional.empty()),
                                new Transition(
                                        TransitionKind.RETURN_FOR_CORRECTION,
                                        java.util.Optional.of("intake"),
                                        java.util.Optional.of("area > 100"))));
        WorkflowDefinition wf = new WorkflowDefinition("intake", List.of(intake, review));

        ServiceDefinitionVersion v = ServiceDefinitionVersion.draft("v1", "d1", 1, NOW);
        v.setWorkflow(wf);
        v.setFormSchema(
                new org.sirmax.domain.service.FormSchema(
                        List.of(
                                org.sirmax.domain.service.FormField.text("motivo", "Motivo", true),
                                new org.sirmax.domain.service.FormField(
                                        "tipo",
                                        "Tipo de obra",
                                        org.sirmax.domain.service.FieldType.SELECT,
                                        true,
                                        java.util.Optional.empty(),
                                        List.of(
                                                new org.sirmax.domain.service.FormField.Option(
                                                        "TOTAL", "Total"),
                                                new org.sirmax.domain.service.FormField.Option(
                                                        "PARCIAL", "Parcial"))))));
        repo.saveVersion(v);

        ServiceDefinitionVersion loaded = repo.findVersionById("v1").orElseThrow();
        assertThat(loaded.workflow().firstStepKey()).isEqualTo("intake");
        assertThat(loaded.workflow().steps()).hasSize(2);
        assertThat(loaded.workflow().step("review").orElseThrow().requiredPermission())
                .contains("procedure.decide");
        assertThat(
                        loaded.workflow().step("review").orElseThrow().transitions().stream()
                                .filter(t -> t.kind() == TransitionKind.RETURN_FOR_CORRECTION)
                                .findFirst()
                                .orElseThrow()
                                .condition())
                .contains("area > 100");
        assertThat(loaded.formSchema().fields()).hasSize(2);
        assertThat(loaded.formSchema().fields().get(1).options()).hasSize(2);
    }

    @Test
    void versionNumberingAndActiveLookup() {
        repo.saveCategory(ServiceCategory.create("c1", "CERT", "Certificaciones", 1, NOW));
        repo.saveDefinition(
                ServiceDefinition.create("d1", "S1", "c1", "S1", ServiceType.GRATUITO, "DO", NOW));

        assertThat(repo.nextVersionNumber("d1")).isEqualTo(1);

        ServiceDefinitionVersion v1 = ServiceDefinitionVersion.draft("v1", "d1", 1, NOW);
        v1.publish(NOW);
        repo.saveVersion(v1);
        assertThat(repo.nextVersionNumber("d1")).isEqualTo(2);
        assertThat(repo.findActiveVersion("d1")).map(ServiceDefinitionVersion::id).contains("v1");

        ServiceDefinitionVersion v2 = ServiceDefinitionVersion.draft("v2", "d1", 2, NOW);
        v2.publish(NOW);
        repo.saveVersion(v2);
        v1.deactivate();
        repo.saveVersion(v1);

        assertThat(repo.findActiveVersion("d1")).map(ServiceDefinitionVersion::versionNumber).contains(2);
        assertThat(repo.listVersions("d1")).hasSize(2);
    }
}
