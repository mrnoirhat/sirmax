// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sirmax.domain.service.RequirementDef;
import org.sirmax.domain.service.RequirementStage;
import org.sirmax.domain.service.ServiceCategory;
import org.sirmax.domain.service.ServiceDefinition;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.domain.service.ServiceStatus;
import org.sirmax.domain.service.ServiceType;
import org.sirmax.domain.service.Sla;
import org.sirmax.shared.JsonDoc;

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
        v1.setFeeRules(JsonDoc.of("[{\"type\":\"FIXED\",\"amountMinor\":50000}]"));
        repo.saveVersion(v1);

        ServiceDefinitionVersion loaded = repo.findVersionById("v1").orElseThrow();
        assertThat(loaded.requiresPayment()).isTrue();
        assertThat(loaded.sla().targetDays()).isEqualTo(3);
        assertThat(loaded.sla().escalationThreshold()).contains(5);
        assertThat(loaded.requirements()).hasSize(2);
        assertThat(loaded.requirements().get(0).key()).isEqualTo("cedula");
        assertThat(loaded.requirements().get(0).label()).isEqualTo("Cédula");
        assertThat(loaded.feeRules().value()).contains("50000");
        assertThat(loaded.status()).isEqualTo(ServiceStatus.DRAFT);
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
