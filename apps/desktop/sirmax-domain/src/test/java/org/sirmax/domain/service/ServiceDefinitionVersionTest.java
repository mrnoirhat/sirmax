// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sirmax.domain.finance.ChargeType;
import org.sirmax.domain.finance.FeeRule;

class ServiceDefinitionVersionTest {

    private static final Instant NOW = Instant.parse("2026-04-01T08:00:00Z");

    private ServiceDefinitionVersion draft() {
        return ServiceDefinitionVersion.draft("v1", "svc1", 1, NOW);
    }

    @Test
    void draftStartsEmptyAndEditable() {
        ServiceDefinitionVersion v = draft();
        assertThat(v.status()).isEqualTo(ServiceStatus.DRAFT);
        assertThat(v.requirements()).isEmpty();
        assertThat(v.requiresPayment()).isFalse();
        assertThat(v.publishedAt()).isEmpty();
        assertThat(v.feeRules()).isEmpty();
        assertThat(v.workflow().isEmpty()).isTrue();
        assertThat(v.formSchema().isEmpty()).isTrue();
    }

    private static FeeRule fixedRule() {
        return FeeRule.fixed(
                "r1", ChargeType.TASA, "Trámite", "DOP", 50_000, LocalDate.parse("2026-01-01"));
    }

    @Test
    void draftAcceptsConfiguration() {
        ServiceDefinitionVersion v = draft();
        v.setRequirements(
                List.of(
                        RequirementDef.mandatoryDocument("cedula", "Cédula", RequirementStage.INTAKE),
                        RequirementDef.mandatoryDocument(
                                "prueba_residencia", "Prueba de residencia", RequirementStage.INTAKE)));
        v.setRequiresPayment(true);
        v.setSla(Sla.businessDays(3));
        v.setFeeRules(List.of(fixedRule()));

        assertThat(v.requirements()).hasSize(2);
        assertThat(v.sla().targetDays()).isEqualTo(3);
        assertThat(v.requiresPayment()).isTrue();
        assertThat(v.feeRules()).hasSize(1);
    }

    @Test
    void publishMakesItActiveImmutableAndTimestamped() {
        ServiceDefinitionVersion v = draft();
        v.setRequiresPayment(true);
        v.publish(NOW);

        assertThat(v.status()).isEqualTo(ServiceStatus.ACTIVE);
        assertThat(v.publishedAt()).contains(NOW);
        assertThatThrownBy(() -> v.setRequiresPayment(false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("immutable");
        assertThatThrownBy(() -> v.setRequirements(List.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void lifecycleTransitionsAreGuarded() {
        ServiceDefinitionVersion v = draft();
        assertThatThrownBy(v::deactivate).isInstanceOf(IllegalStateException.class);

        v.publish(NOW);
        v.deactivate();
        assertThat(v.status()).isEqualTo(ServiceStatus.INACTIVE);
        v.reactivate();
        assertThat(v.status()).isEqualTo(ServiceStatus.ACTIVE);

        v.archive();
        assertThat(v.status()).isEqualTo(ServiceStatus.ARCHIVED);
        assertThatThrownBy(() -> v.publish(NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void copyAsNewDraftClonesContentButNotStatus() {
        ServiceDefinitionVersion v1 = draft();
        v1.setRequirements(
                List.of(RequirementDef.mandatoryDocument("cedula", "Cédula", RequirementStage.INTAKE)));
        v1.setRequiresPayment(true);
        v1.publish(NOW);

        ServiceDefinitionVersion v2 = v1.copyAsNewDraft("v2", 2, NOW.plusSeconds(86400));
        assertThat(v2.status()).isEqualTo(ServiceStatus.DRAFT);
        assertThat(v2.versionNumber()).isEqualTo(2);
        assertThat(v2.requirements()).hasSize(1);
        assertThat(v2.requiresPayment()).isTrue();
        assertThat(v2.publishedAt()).isEmpty();

        assertThatThrownBy(() -> v1.copyAsNewDraft("vX", 1, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
