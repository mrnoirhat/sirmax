// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks a {@link ServiceDefinitionVersion} is coherent enough to publish.
 *
 * <p>Returns problem keys (i18n keys the UI resolves) rather than throwing, so a configuration
 * screen can show all of them at once. Deep validation of the JSON parts arrives with their typed
 * models (docs/adr/0007, 0008); this is the Phase 4 baseline.
 */
public final class ServiceVersionValidator {

    private ServiceVersionValidator() {}

    /**
     * @param version the version to check
     * @param serviceType the owning definition's type (for payment consistency)
     * @return an empty list if it can be published; otherwise problem keys
     */
    public static List<String> validate(ServiceDefinitionVersion version, ServiceType serviceType) {
        List<String> problems = new ArrayList<>();

        Set<String> seenKeys = new HashSet<>();
        for (RequirementDef r : version.requirements()) {
            if (!seenKeys.add(r.key())) {
                problems.add("service.validate.duplicate_requirement_key");
                break;
            }
        }

        if (serviceType == ServiceType.GRATUITO && version.requiresPayment()) {
            problems.add("service.validate.free_service_cannot_require_payment");
        }
        if (serviceType == ServiceType.CON_TASA && !version.requiresPayment()) {
            problems.add("service.validate.paid_service_must_require_payment");
        }
        if (version.requiresPayment() && version.feeRules().value().equals("[]")) {
            problems.add("service.validate.payment_requires_fee_rules");
        }

        return List.copyOf(problems);
    }
}
