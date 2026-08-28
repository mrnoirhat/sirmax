// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sirmax.application.catalog.ServiceCatalogTemplates;
import org.sirmax.application.catalog.ServiceCategoryTemplate;
import org.sirmax.application.catalog.ServiceTemplate;
import org.sirmax.application.fakes.Fakes;
import org.sirmax.application.port.ServiceCatalogTemplateSource;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.finance.ChargeType;
import org.sirmax.domain.finance.FeeRule;
import org.sirmax.domain.security.AccessPolicy;
import org.sirmax.domain.security.AppUser;
import org.sirmax.domain.security.PasswordHash;
import org.sirmax.domain.security.Permission;
import org.sirmax.domain.service.RequirementDef;
import org.sirmax.domain.service.RequirementStage;
import org.sirmax.domain.service.ServiceCategory;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.domain.service.ServiceStatus;
import org.sirmax.domain.service.ServiceType;
import org.sirmax.domain.service.ServiceVersionValidator;
import org.sirmax.domain.service.Sla;
import org.sirmax.domain.service.Validity;
import org.sirmax.domain.workflow.StepType;
import org.sirmax.domain.workflow.Transition;
import org.sirmax.domain.workflow.TransitionKind;
import org.sirmax.domain.workflow.WorkflowDefinition;
import org.sirmax.domain.workflow.WorkflowStep;
import org.sirmax.shared.Result;

class SeedServiceCatalogTest {

    private static final Instant NOW = Instant.parse("2026-08-28T09:00:00Z");

    private final Fakes.InMemoryServiceCatalog catalog = new Fakes.InMemoryServiceCatalog();
    private final Fakes.SeqIds ids = new Fakes.SeqIds();
    private final Fakes.FixedClock clock = new Fakes.FixedClock(NOW);
    private final Fakes.RecordingAuditSink auditSink = new Fakes.RecordingAuditSink();
    private final Audit audit = new Audit(auditSink, clock, ids);
    private final Fakes.DirectUnitOfWork uow = new Fakes.DirectUnitOfWork();

    private Session admin;

    @BeforeEach
    void setUp() {
        admin =
                new Session(
                        "s1",
                        AppUser.create("u1", "admin", "Admin", new PasswordHash("FAKE", "h:x"), null, NOW),
                        AccessPolicy.of(EnumSet.of(Permission.SERVICE_CONFIGURE)),
                        NOW);
    }

    private SeedServiceCatalog useCase(ServiceCatalogTemplateSource source) {
        return new SeedServiceCatalog(source, catalog, ids, clock, uow, audit);
    }

    // ── a small valid bundle ──────────────────────────────────────────────

    private static WorkflowDefinition simpleWorkflow() {
        return new WorkflowDefinition(
                "solicitud",
                List.of(
                        new WorkflowStep(
                                "solicitud",
                                "Solicitud",
                                StepType.TASK,
                                java.util.Optional.empty(),
                                0,
                                List.of(Transition.advance("emision"))),
                        new WorkflowStep(
                                "emision",
                                "Emisión",
                                StepType.DOCUMENT_OUTPUT,
                                java.util.Optional.empty(),
                                0,
                                List.of(Transition.terminal(TransitionKind.APPROVE)))));
    }

    private static ServiceCatalogTemplates bundle() {
        RequirementDef cedula =
                RequirementDef.mandatoryDocument("cedula", "Cédula", RequirementStage.INTAKE);
        FeeRule fee =
                FeeRule.fixed(
                        "fee-1", ChargeType.TASA, "Tasa", "DOP", 0L, LocalDate.parse("2026-01-01"));

        ServiceTemplate free =
                new ServiceTemplate(
                        "QUEJA_GENERAL",
                        "Queja general",
                        "ATENCION",
                        java.util.Optional.of("Canal de quejas"),
                        ServiceType.GRATUITO,
                        false,
                        List.of(cedula),
                        simpleWorkflow(),
                        List.of(),
                        Sla.businessDays(10),
                        Validity.permanent(),
                        java.util.Optional.of("RCL"),
                        true,
                        java.util.Optional.empty());
        ServiceTemplate paid =
                new ServiceTemplate(
                        "CERT_RESIDENCIA",
                        "Certificación de residencia",
                        "CERT",
                        java.util.Optional.empty(),
                        ServiceType.CON_TASA,
                        true,
                        List.of(cedula),
                        simpleWorkflow(),
                        List.of(fee),
                        Sla.businessDays(5),
                        Validity.ofDays(90, true),
                        java.util.Optional.of("CERT"),
                        true,
                        java.util.Optional.empty());

        return new ServiceCatalogTemplates(
                "DO",
                1,
                List.of(
                        new ServiceCategoryTemplate("ATENCION", "Atención ciudadana", 10),
                        new ServiceCategoryTemplate("CERT", "Certificaciones", 20)),
                List.of(free, paid));
    }

    // ── tests ─────────────────────────────────────────────────────────────

    @Test
    void nonAdminIsForbidden() {
        Session op =
                new Session(
                        "s2",
                        AppUser.create("u2", "op", "Op", new PasswordHash("FAKE", "h:x"), null, NOW),
                        AccessPolicy.of(EnumSet.of(Permission.SERVICE_READ)),
                        NOW);
        Result<?> r = useCase(SeedServiceCatalogTest::bundleStatic).execute(new SeedServiceCatalog.Command(op, "t"));
        assertThat(((Result.Err<?>) r).messageKey()).isEqualTo("error.forbidden");
        assertThat(catalog.definitions).isEmpty();
    }

    private static ServiceCatalogTemplates bundleStatic() {
        return bundle();
    }

    @Test
    void seedsCategoriesAndServicesAsPublishableDrafts() {
        SeedServiceCatalog.Summary summary =
                useCase(SeedServiceCatalogTest::bundleStatic)
                        .execute(new SeedServiceCatalog.Command(admin, "t"))
                        .orElseThrow();

        assertThat(summary.categoriesCreated()).isEqualTo(2);
        assertThat(summary.servicesCreated()).isEqualTo(2);
        assertThat(summary.servicesSkipped()).isZero();

        assertThat(catalog.categories).hasSize(2);
        assertThat(catalog.definitions).hasSize(2);
        assertThat(catalog.versions).hasSize(2);

        for (ServiceDefinitionVersion v : catalog.versions.values()) {
            assertThat(v.versionNumber()).isEqualTo(1);
            assertThat(v.status()).isEqualTo(ServiceStatus.DRAFT);
            ServiceType type =
                    catalog.findDefinitionById(v.serviceDefinitionId()).orElseThrow().serviceType();
            assertThat(ServiceVersionValidator.validate(v, type)).isEmpty();
        }
        assertThat(auditSink.actions()).contains("service.catalog_seeded");
    }

    @Test
    void isIdempotent() {
        SeedServiceCatalog uc = useCase(SeedServiceCatalogTest::bundleStatic);
        uc.execute(new SeedServiceCatalog.Command(admin, "t")).orElseThrow();

        SeedServiceCatalog.Summary second =
                uc.execute(new SeedServiceCatalog.Command(admin, "t")).orElseThrow();

        assertThat(second.categoriesCreated()).isZero();
        assertThat(second.servicesCreated()).isZero();
        assertThat(second.servicesSkipped()).isEqualTo(2);
        assertThat(catalog.definitions).hasSize(2);
        assertThat(catalog.versions).hasSize(2);
    }

    @Test
    void keepsAnExistingCategoryWithTheSameCode() {
        catalog.saveCategory(
                ServiceCategory.create("pre-existing", "CERT", "Mis certificaciones", 99, NOW));

        SeedServiceCatalog.Summary summary =
                useCase(SeedServiceCatalogTest::bundleStatic)
                        .execute(new SeedServiceCatalog.Command(admin, "t"))
                        .orElseThrow();

        assertThat(summary.categoriesCreated()).isEqualTo(1); // only ATENCION
        assertThat(catalog.findCategoryByCode("CERT").orElseThrow().id()).isEqualTo("pre-existing");
        assertThat(catalog.findDefinitionByCode("CERT_RESIDENCIA").orElseThrow().categoryId())
                .isEqualTo("pre-existing");
    }

    @Test
    void rejectsABundleWhoseServiceHasNoCategory() {
        ServiceCatalogTemplates broken =
                new ServiceCatalogTemplates(
                        "DO",
                        1,
                        List.of(new ServiceCategoryTemplate("ATENCION", "Atención", 10)),
                        List.of(
                                new ServiceTemplate(
                                        "HUERFANO",
                                        "Servicio sin categoría",
                                        "NO_EXISTE",
                                        java.util.Optional.empty(),
                                        ServiceType.GRATUITO,
                                        false,
                                        List.of(),
                                        simpleWorkflow(),
                                        List.of(),
                                        Sla.none(),
                                        Validity.permanent(),
                                        java.util.Optional.empty(),
                                        true,
                                        java.util.Optional.empty())));

        Result<?> r = useCase(() -> broken).execute(new SeedServiceCatalog.Command(admin, "t"));

        assertThat(((Result.Err<?>) r).messageKey()).isEqualTo("service.catalog.invalid_bundle");
        assertThat(catalog.definitions).isEmpty();
        assertThat(catalog.categories).isEmpty();
    }
}
