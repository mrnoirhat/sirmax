// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sirmax.application.fakes.Fakes;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import java.time.LocalDate;
import org.sirmax.domain.common.ArchiveStatus;
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
import org.sirmax.shared.Result;

class ServiceEngineTest {

    private static final Instant NOW = Instant.parse("2026-04-10T09:00:00Z");

    private final Fakes.InMemoryServiceCatalog catalog = new Fakes.InMemoryServiceCatalog();
    private final Fakes.SeqIds ids = new Fakes.SeqIds();
    private final Fakes.FixedClock clock = new Fakes.FixedClock(NOW);
    private final Fakes.RecordingAuditSink auditSink = new Fakes.RecordingAuditSink();
    private final Audit audit = new Audit(auditSink, clock, ids);
    private final Fakes.DirectUnitOfWork uow = new Fakes.DirectUnitOfWork();

    private CreateServiceDraft create;
    private ConfigureServiceDraft configure;
    private PublishServiceVersion publish;
    private CreateServiceDraftVersion newDraft;
    private SetServiceAvailability availability;

    private Session admin;

    @BeforeEach
    void setUp() {
        create = new CreateServiceDraft(catalog, ids, clock, uow, audit);
        configure = new ConfigureServiceDraft(catalog, uow, audit);
        publish = new PublishServiceVersion(catalog, clock, uow, audit);
        newDraft = new CreateServiceDraftVersion(catalog, ids, clock, uow, audit);
        availability = new SetServiceAvailability(catalog, clock, uow, audit);

        catalog.saveCategory(ServiceCategory.create("cat-cert", "CERT", "Certificaciones", 1, NOW));

        AppUser u = AppUser.create("u1", "admin", "Admin", new PasswordHash("FAKE", "h:x"), null, NOW);
        admin = new Session("s1", u, AccessPolicy.of(EnumSet.of(Permission.SERVICE_CONFIGURE)), NOW);
    }

    private CreateServiceDraft.Command createCmd(String code, ServiceType type) {
        return new CreateServiceDraft.Command(admin, code, "cat-cert", "Certificado", type, "DO", "t");
    }

    @Test
    void nonAdminCannotCreate() {
        Session op =
                new Session(
                        "s2",
                        AppUser.create("u2", "op", "Op", new PasswordHash("FAKE", "h:x"), null, NOW),
                        AccessPolicy.of(EnumSet.of(Permission.SERVICE_READ)),
                        NOW);
        Result<?> r = create.execute(new CreateServiceDraft.Command(op, "X", "cat-cert", "n", ServiceType.GRATUITO, "DO", "t"));
        assertThat(((Result.Err<?>) r).messageKey()).isEqualTo("error.forbidden");
    }

    @Test
    void createConfigurePublishSupersedeFlow() {
        // create v1 draft
        CreateServiceDraft.Created created =
                create.execute(createCmd("CERT-RES", ServiceType.CON_TASA)).orElseThrow();
        assertThat(catalog.definitions).hasSize(1);
        assertThat(catalog.versions).hasSize(1);

        // duplicate code refused
        assertThat(((Result.Err<?>) create.execute(createCmd("cert-res", ServiceType.CON_TASA))).messageKey())
                .isEqualTo("service.code_taken");

        // publish without config fails validation (CON_TASA needs requiresPayment + fee rules)
        assertThat(
                        ((Result.Err<?>)
                                        publish.execute(
                                                new PublishServiceVersion.Command(
                                                        admin, created.draftVersionId(), "t")))
                                .messageKey())
                .isEqualTo("service.validate.paid_service_must_require_payment");

        // configure the draft
        configure.execute(
                new ConfigureServiceDraft.Command(
                        admin,
                        created.draftVersionId(),
                        Optional.of(
                                List.of(
                                        RequirementDef.mandatoryDocument(
                                                "cedula", "Cédula", RequirementStage.INTAKE))),
                        Optional.of(true),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(
                                List.of(
                                        FeeRule.fixed(
                                                "fee1",
                                                ChargeType.TASA,
                                                "Certificación",
                                                "DOP",
                                                50_000,
                                                LocalDate.parse("2026-01-01")))),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        "t"));

        // publish v1
        ServiceDefinitionVersion v1 =
                publish.execute(new PublishServiceVersion.Command(admin, created.draftVersionId(), "t"))
                        .orElseThrow();
        assertThat(v1.status()).isEqualTo(ServiceStatus.ACTIVE);
        assertThat(catalog.definitions.get(created.definitionId()).currentVersionId())
                .contains(v1.id());

        // start v2 from the active one
        ServiceDefinitionVersion v2draft =
                newDraft.execute(new CreateServiceDraftVersion.Command(admin, created.definitionId(), "t"))
                        .orElseThrow();
        assertThat(v2draft.versionNumber()).isEqualTo(2);
        assertThat(v2draft.requiresPayment()).isTrue(); // cloned

        // while a draft is open, another cannot be started
        assertThat(
                        ((Result.Err<?>)
                                        newDraft.execute(
                                                new CreateServiceDraftVersion.Command(
                                                        admin, created.definitionId(), "t")))
                                .messageKey())
                .isEqualTo("service.draft_exists");

        // publish v2 -> v1 becomes INACTIVE
        publish.execute(new PublishServiceVersion.Command(admin, v2draft.id(), "t")).orElseThrow();
        assertThat(catalog.findVersionById(v1.id()).orElseThrow().status())
                .isEqualTo(ServiceStatus.INACTIVE);
        assertThat(catalog.findActiveVersion(created.definitionId()).orElseThrow().versionNumber())
                .isEqualTo(2);
    }

    @Test
    void deactivateAndReactivateAService() {
        CreateServiceDraft.Created c =
                create.execute(createCmd("CERT-VIDA", ServiceType.GRATUITO)).orElseThrow();

        availability.execute(new SetServiceAvailability.Command(admin, c.definitionId(), false, "t"));
        assertThat(catalog.definitions.get(c.definitionId()).archiveStatus())
                .isEqualTo(ArchiveStatus.ARCHIVED);

        availability.execute(new SetServiceAvailability.Command(admin, c.definitionId(), true, "t"));
        assertThat(catalog.definitions.get(c.definitionId()).archiveStatus())
                .isEqualTo(ArchiveStatus.ACTIVE);

        assertThat(auditSink.actions()).contains("service.deactivated", "service.activated");
    }
}
