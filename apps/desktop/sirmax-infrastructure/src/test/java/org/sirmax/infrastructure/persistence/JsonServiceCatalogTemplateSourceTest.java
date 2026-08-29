// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.sirmax.application.catalog.ServiceCatalogTemplates;
import org.sirmax.application.catalog.ServiceTemplate;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.domain.service.ServiceStatus;
import org.sirmax.domain.service.ServiceVersionValidator;
import org.sirmax.domain.workflow.WorkflowValidator;

/** The shipped Dominican Republic seed bundle must parse and every template must be publishable. */
class JsonServiceCatalogTemplateSourceTest {

    private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");

    private final ServiceCatalogTemplates bundle = new JsonServiceCatalogTemplateSource().load();

    @Test
    void loadsTheDominicanBundle() {
        assertThat(bundle.country()).isEqualTo("DO");
        assertThat(bundle.version()).isEqualTo(1);
        assertThat(bundle.categories()).hasSize(12);
        assertThat(bundle.services()).hasSizeGreaterThanOrEqualTo(90);
    }

    @Test
    void everyServiceReferencesADeclaredCategory() {
        Set<String> categoryCodes =
                bundle.categories().stream()
                        .map(c -> c.code().toLowerCase())
                        .collect(Collectors.toSet());
        for (ServiceTemplate t : bundle.services()) {
            assertThat(categoryCodes)
                    .as("category of %s", t.code())
                    .contains(t.categoryCode().toLowerCase());
        }
    }

    @Test
    void serviceCodesAreUnique() {
        Set<String> seen = new HashSet<>();
        for (ServiceTemplate t : bundle.services()) {
            assertThat(seen.add(t.code().toLowerCase())).as("duplicate code %s", t.code()).isTrue();
        }
    }

    @Test
    void everyWorkflowIsStructurallyValid() {
        for (ServiceTemplate t : bundle.services()) {
            assertThat(WorkflowValidator.validate(t.workflow()))
                    .as("workflow of %s", t.code())
                    .isEmpty();
        }
    }

    @Test
    void everyTemplateProducesAPublishableDraft() {
        for (ServiceTemplate t : bundle.services()) {
            ServiceDefinitionVersion v = draftFrom(t);
            assertThat(v.status()).isEqualTo(ServiceStatus.DRAFT);
            assertThat(ServiceVersionValidator.validate(v, t.serviceType()))
                    .as("validation of %s", t.code())
                    .isEmpty();
        }
    }

    /** Mirrors {@code SeedServiceCatalog}: copy the template onto a fresh v1 DRAFT. */
    private static ServiceDefinitionVersion draftFrom(ServiceTemplate t) {
        ServiceDefinitionVersion v =
                ServiceDefinitionVersion.draft("v-" + t.code(), "def-" + t.code(), 1, NOW);
        v.setRequiresPayment(t.requiresPayment());
        v.setRequirements(t.requirements());
        v.setWorkflow(t.workflow());
        v.setFeeRules(t.feeRules());
        v.setSla(t.sla());
        v.setValidity(t.validity());
        t.numberingSequenceCode().ifPresent(v::setNumberingSequenceCode);
        return v;
    }
}
