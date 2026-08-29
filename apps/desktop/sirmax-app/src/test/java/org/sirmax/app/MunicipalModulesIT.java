// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sirmax.application.security.Session;
import org.sirmax.application.usecase.ConductInspection;
import org.sirmax.application.usecase.GrantAgreement;
import org.sirmax.application.usecase.ProvisionInitialAdmin;
import org.sirmax.application.usecase.RegisterDocument;
import org.sirmax.application.usecase.StartProcedure;
import org.sirmax.application.usecase.TransferAgreement;
import org.sirmax.domain.asset.Agreement;
import org.sirmax.domain.asset.AgreementKind;
import org.sirmax.domain.asset.AssetHolder;
import org.sirmax.domain.asset.AssetKind;
import org.sirmax.domain.asset.Availability;
import org.sirmax.domain.asset.MunicipalAsset;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.registry.Inspection;
import org.sirmax.domain.registry.RegisteredDocument;
import org.sirmax.domain.service.ServiceCategory;
import org.sirmax.domain.service.ServiceDefinition;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.domain.service.ServiceType;
import org.sirmax.infrastructure.persistence.SqliteDatabase;
import org.sirmax.shared.Money;
import org.sirmax.shared.Result;

/**
 * The Phase 7 modules against the real graph.
 *
 * <p>The point being tested is architectural: a cemetery niche, a market stall and a municipal
 * parcel go through the <em>same</em> asset and agreement code. If any of them needed a special case,
 * these tests would not all be able to share a helper.
 */
class MunicipalModulesIT {

    private SqliteDatabase database;
    private CompositionRoot root;
    private Session officer;

    private static Money dop(String amount) {
        return Money.of(amount, "DOP");
    }

    @BeforeEach
    void setUp() {
        database = SqliteDatabase.openInMemory();
        root = CompositionRoot.bootstrap(database);

        root.provisionInitialAdmin()
                .execute(
                        new ProvisionInitialAdmin.Command(
                                "Ayuntamiento de Santiago",
                                "Santiago",
                                "DO",
                                "admin",
                                "Administradora",
                                "una-contrasena-larga".toCharArray()));
        officer =
                root.authenticate()
                        .execute(
                                new org.sirmax.application.usecase.Authenticate.Command(
                                        "admin", "una-contrasena-larga".toCharArray(), "test"))
                        .orElseThrow();
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    /** A container plus one grantable space inside it — the shape every module shares. */
    private MunicipalAsset space(AssetKind containerKind, AssetKind spaceKind, String code) {
        var now = root.clock().now();
        MunicipalAsset container =
                MunicipalAsset.create(
                        "container-" + spaceKind, "C-" + spaceKind, containerKind, "Contenedor", null, now);
        root.assets().save(container);

        MunicipalAsset asset =
                MunicipalAsset.create(
                        "asset-" + spaceKind, code, spaceKind, code, container.id(), now);
        root.assets().save(asset);
        return asset;
    }

    private Result<Agreement> grant(MunicipalAsset asset, AgreementKind kind, PartyRef holder) {
        return root.grantAgreement()
                .execute(
                        new GrantAgreement.Command(
                                officer,
                                asset.id(),
                                holder,
                                kind,
                                LocalDate.of(2026, 3, 1),
                                Optional.of(LocalDate.of(2027, 2, 28)),
                                dop("1500.00"),
                                Agreement.BillingFrequency.MONTHLY,
                                Optional.empty(),
                                "test"));
    }

    @Test
    void aStallANicheAndAParcelAllGrantThroughTheSameCode() {
        record Case(AssetKind container, AssetKind space, AgreementKind kind, String code) {}

        List<Case> cases =
                List.of(
                        new Case(
                                AssetKind.MARKET,
                                AssetKind.MARKET_STALL,
                                AgreementKind.STALL_ASSIGNMENT,
                                "CASILLA-12"),
                        new Case(
                                AssetKind.CEMETERY,
                                AssetKind.CEMETERY_PLOT,
                                AgreementKind.CONCESSION,
                                "NICHO-A-45"),
                        new Case(
                                AssetKind.OTHER,
                                AssetKind.PARCEL,
                                AgreementKind.LEASE,
                                "PARCELA-88"));

        for (Case c : cases) {
            MunicipalAsset asset = space(c.container(), c.space(), c.code());
            Agreement agreement =
                    grant(asset, c.kind(), PartyRef.person("per-1")).orElseThrow();

            assertThat(agreement.status()).isEqualTo(Agreement.Status.ACTIVE);
            assertThat(agreement.code()).startsWith("CONT-");
            // granting flips the space to occupied and opens a holder period
            assertThat(root.assets().findById(asset.id()).orElseThrow().availability())
                    .isEqualTo(Availability.OCCUPIED);
            assertThat(root.assets().currentHoldersOf(asset.id())).hasSize(1);
        }
    }

    @Test
    void anOccupiedSpaceCannotBeGrantedAgain() {
        MunicipalAsset stall =
                space(AssetKind.MARKET, AssetKind.MARKET_STALL, "CASILLA-12");
        grant(stall, AgreementKind.STALL_ASSIGNMENT, PartyRef.person("per-1")).orElseThrow();

        Result<Agreement> second =
                grant(stall, AgreementKind.STALL_ASSIGNMENT, PartyRef.person("per-2"));

        assertThat(((Result.Err<?>) second).messageKey()).isEqualTo("asset.not_available");
    }

    @Test
    void aContainerItselfCannotBeGranted() {
        var now = root.clock().now();
        MunicipalAsset market =
                MunicipalAsset.create("m-1", "MERCADO-CENTRAL", AssetKind.MARKET, "Mercado", null, now);
        root.assets().save(market);

        Result<Agreement> result =
                grant(market, AgreementKind.STALL_ASSIGNMENT, PartyRef.person("per-1"));

        assertThat(((Result.Err<?>) result).messageKey()).isEqualTo("asset.not_grantable");
    }

    @Test
    void aTransferChainsTheContractsAndClosesTheOutgoingHolderPeriod() {
        MunicipalAsset stall = space(AssetKind.MARKET, AssetKind.MARKET_STALL, "CASILLA-12");
        Agreement original =
                grant(stall, AgreementKind.STALL_ASSIGNMENT, PartyRef.person("per-1"))
                        .orElseThrow();

        Agreement successor =
                root.transferAgreement()
                        .execute(
                                new TransferAgreement.Command(
                                        officer,
                                        original.id(),
                                        PartyRef.person("per-2"),
                                        LocalDate.of(2026, 7, 1),
                                        "Traspaso por herencia",
                                        Optional.empty(),
                                        "test"))
                        .orElseThrow();

        assertThat(successor.transferredFromId()).contains(original.id());
        assertThat(root.assets().findAgreementById(original.id()).orElseThrow().status())
                .isEqualTo(Agreement.Status.TRANSFERRED);

        List<AssetHolder> history = root.assets().holdersOf(stall.id());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).toDate()).contains(LocalDate.of(2026, 6, 30));
        assertThat(history.get(1).isCurrent()).isTrue();
        assertThat(history.get(1).party()).isEqualTo(PartyRef.person("per-2"));

        // the history answers "who held this on a given date" without ambiguity
        assertThat(history.get(0).wasHeldOn(LocalDate.of(2026, 5, 1))).isTrue();
        assertThat(history.get(1).wasHeldOn(LocalDate.of(2026, 5, 1))).isFalse();
    }

    @Test
    void transferringToTheSameHolderIsRefused() {
        MunicipalAsset stall = space(AssetKind.MARKET, AssetKind.MARKET_STALL, "CASILLA-12");
        Agreement original =
                grant(stall, AgreementKind.STALL_ASSIGNMENT, PartyRef.person("per-1"))
                        .orElseThrow();

        Result<Agreement> result =
                root.transferAgreement()
                        .execute(
                                new TransferAgreement.Command(
                                        officer,
                                        original.id(),
                                        PartyRef.person("per-1"),
                                        LocalDate.of(2026, 7, 1),
                                        "Error",
                                        Optional.empty(),
                                        "test"));

        assertThat(((Result.Err<?>) result).messageKey()).isEqualTo("agreement.same_holder");
    }

    @Test
    void aDocumentIsPresentedThenBookedIntoTheRegister() {
        RegisteredDocument presented =
                root.registerDocument()
                        .present(
                                new RegisterDocument.PresentCommand(
                                        officer,
                                        "Acto de venta",
                                        "Venta de solar en Gurabo",
                                        Optional.of(LocalDate.of(2026, 2, 10)),
                                        List.of(
                                                new RegisterDocument.PartyRole(
                                                        PartyRef.person("per-1"), "vendedor"),
                                                new RegisterDocument.PartyRole(
                                                        PartyRef.person("per-2"), "comprador")),
                                        Optional.empty(),
                                        Optional.empty(),
                                        "test"))
                        .orElseThrow();

        assertThat(presented.registrationNumber()).isEqualTo("REG-2026-000001");
        assertThat(presented.status()).isEqualTo(RegisteredDocument.Status.PRESENTED);

        RegisteredDocument registered =
                root.registerDocument()
                        .register(
                                new RegisterDocument.RegisterCommand(
                                        officer,
                                        presented.id(),
                                        "7",
                                        Optional.of("II"),
                                        "134",
                                        "test"))
                        .orElseThrow();

        assertThat(registered.status()).isEqualTo(RegisteredDocument.Status.REGISTERED);
        assertThat(registered.canIssueCertifiedCopy()).isTrue();

        // it survives the round trip with its parties intact
        RegisteredDocument reloaded =
                root.registry().findDocumentByNumber("REG-2026-000001").orElseThrow();
        assertThat(reloaded.parties()).hasSize(2);
        assertThat(reloaded.folio()).contains("134");
        assertThat(root.registry().documentsNaming(PartyRef.person("per-2"), 10)).hasSize(1);
    }

    @Test
    void aDocumentWithoutPartiesIsRefused() {
        Result<?> result =
                root.registerDocument()
                        .present(
                                new RegisterDocument.PresentCommand(
                                        officer,
                                        "Acto de venta",
                                        "Sin partes",
                                        Optional.empty(),
                                        List.of(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        "test"));

        assertThat(((Result.Err<?>) result).messageKey()).isEqualTo("registry.parties_required");
    }

    @Test
    void anInspectionIsScheduledThenCompletedOntoTheCaseTimeline() {
        Procedure procedure = openCase();

        Inspection scheduled =
                root.conductInspection()
                        .schedule(
                                new ConductInspection.ScheduleCommand(
                                        officer,
                                        procedure.id(),
                                        Optional.of(officer.user().id()),
                                        Optional.of(LocalDate.of(2026, 3, 20)),
                                        Optional.empty(),
                                        "test"))
                        .orElseThrow();
        assertThat(scheduled.code()).isEqualTo("INSP-2026-000001");

        Result<?> withoutFindings =
                root.conductInspection()
                        .complete(
                                new ConductInspection.CompleteCommand(
                                        officer,
                                        scheduled.id(),
                                        Inspection.Result.FAILED,
                                        Optional.empty(),
                                        List.of(),
                                        Optional.empty(),
                                        "test"));
        assertThat(((Result.Err<?>) withoutFindings).messageKey())
                .isEqualTo("inspection.findings_required");

        Inspection completed =
                root.conductInspection()
                        .complete(
                                new ConductInspection.CompleteCommand(
                                        officer,
                                        scheduled.id(),
                                        Inspection.Result.PASSED_WITH_CONDITIONS,
                                        Optional.of("Falta la acera; plazo de 30 días"),
                                        List.of(
                                                new Inspection.ChecklistAnswer(
                                                        "acera",
                                                        "Acera terminada",
                                                        false,
                                                        Optional.of("Pendiente")),
                                                new Inspection.ChecklistAnswer(
                                                        "retiros",
                                                        "Retiros respetados",
                                                        true,
                                                        Optional.empty())),
                                        Optional.of(LocalDate.of(2026, 4, 20)),
                                        "test"))
                        .orElseThrow();

        assertThat(completed.result()).contains(Inspection.Result.PASSED_WITH_CONDITIONS);
        assertThat(completed.result().orElseThrow().allowsProgress()).isTrue();
        assertThat(completed.breaches()).hasSize(1);

        // the checklist survives the JSON round trip
        Inspection reloaded = root.registry().findInspectionById(scheduled.id()).orElseThrow();
        assertThat(reloaded.checklist()).hasSize(2);
        assertThat(reloaded.breaches().get(0).key()).isEqualTo("acera");
        assertThat(reloaded.followUpDate()).contains(LocalDate.of(2026, 4, 20));

        // and it landed on the case timeline
        assertThat(root.procedureRepository().findEvents(procedure.id()))
                .anyMatch(e -> e.detail().orElse("").contains("PASSED_WITH_CONDITIONS"));
    }

    private Procedure openCase() {
        var now = root.clock().now();
        var catalog = root.serviceCatalogRepository();
        catalog.saveCategory(ServiceCategory.create("cat-1", "URB", "Planeamiento", 1, now));
        ServiceDefinition definition =
                ServiceDefinition.create(
                        "svc-1", "URB-CONS", "cat-1", "Licencia de construcción",
                        ServiceType.CON_TASA, "DO", now);
        ServiceDefinitionVersion version = ServiceDefinitionVersion.draft("ver-1", "svc-1", 1, now);
        version.publish(now);
        definition.setCurrentVersion("ver-1", now);
        catalog.saveDefinition(definition);
        catalog.saveVersion(version);

        return root.startProcedure()
                .execute(
                        new StartProcedure.Command(
                                officer,
                                "svc-1",
                                PartyRef.person("per-1"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                "test"))
                .orElseThrow();
    }
}
