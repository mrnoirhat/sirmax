// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sirmax.shared.JsonDoc;

class ServiceVersionValidatorTest {

    private static final Instant NOW = Instant.parse("2026-04-10T00:00:00Z");

    private ServiceDefinitionVersion draft() {
        return ServiceDefinitionVersion.draft("v1", "s1", 1, NOW);
    }

    @Test
    void freeServiceMustNotRequirePayment() {
        ServiceDefinitionVersion v = draft();
        v.setRequiresPayment(true);
        assertThat(ServiceVersionValidator.validate(v, ServiceType.GRATUITO))
                .contains("service.validate.free_service_cannot_require_payment");
    }

    @Test
    void paidServiceMustRequirePaymentAndHaveFeeRules() {
        ServiceDefinitionVersion v = draft();
        assertThat(ServiceVersionValidator.validate(v, ServiceType.CON_TASA))
                .contains("service.validate.paid_service_must_require_payment");

        v.setRequiresPayment(true); // still fee_rules == "[]"
        assertThat(ServiceVersionValidator.validate(v, ServiceType.CON_TASA))
                .contains("service.validate.payment_requires_fee_rules");

        v.setFeeRules(JsonDoc.of("[{\"type\":\"FIXED\"}]"));
        assertThat(ServiceVersionValidator.validate(v, ServiceType.CON_TASA)).isEmpty();
    }

    @Test
    void duplicateRequirementKeysAreRejected() {
        ServiceDefinitionVersion v = draft();
        v.setRequirements(
                List.of(
                        RequirementDef.mandatoryDocument("cedula", "Cédula", RequirementStage.INTAKE),
                        RequirementDef.mandatoryDocument("cedula", "Otra", RequirementStage.REVIEW)));
        assertThat(ServiceVersionValidator.validate(v, ServiceType.GRATUITO))
                .contains("service.validate.duplicate_requirement_key");
    }

    @Test
    void aWellFormedFreeServiceHasNoProblems() {
        ServiceDefinitionVersion v = draft();
        v.setRequirements(
                List.of(RequirementDef.mandatoryDocument("cedula", "Cédula", RequirementStage.INTAKE)));
        assertThat(ServiceVersionValidator.validate(v, ServiceType.GRATUITO)).isEmpty();
    }
}
