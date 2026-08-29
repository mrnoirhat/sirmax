// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.catalog;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.domain.finance.FeeRule;
import org.sirmax.domain.service.RequirementDef;
import org.sirmax.domain.service.ServiceType;
import org.sirmax.domain.service.Sla;
import org.sirmax.domain.service.Validity;
import org.sirmax.domain.workflow.WorkflowDefinition;

/**
 * An editable municipal-service template (master prompt §55).
 *
 * <p>Carries the same typed configuration a {@code ServiceDefinitionVersion} holds. {@link
 * org.sirmax.application.usecase.SeedServiceCatalog} copies it onto a fresh {@code DRAFT} version;
 * nothing here is immutable once seeded.
 *
 * @param code stable service code (unique in the catalog, case-insensitive)
 * @param name operator-facing name
 * @param categoryCode the {@link ServiceCategoryTemplate#code()} this belongs to
 * @param description short description, may be blank
 * @param serviceType charge model
 * @param requiresPayment whether a procedure of this service must be paid before delivery
 * @param requirements declared requirements (staged, possibly conditional)
 * @param workflow the workflow (closed-vocabulary, validated by {@code WorkflowValidator})
 * @param feeRules fee rules; a seeded amount is a placeholder the municipality sets
 * @param sla turnaround target
 * @param validity how long the output stays valid, and whether it renews
 * @param numberingSequenceCode default output-document numbering sequence, may be empty
 * @param municipalOverrideAllowed whether a municipality may diverge from this template
 * @param notes seed note shown on the draft version
 */
public record ServiceTemplate(
        String code,
        String name,
        String categoryCode,
        Optional<String> description,
        ServiceType serviceType,
        boolean requiresPayment,
        List<RequirementDef> requirements,
        WorkflowDefinition workflow,
        List<FeeRule> feeRules,
        Sla sla,
        Validity validity,
        Optional<String> numberingSequenceCode,
        boolean municipalOverrideAllowed,
        Optional<String> notes) {

    public ServiceTemplate {
        code = requireText(code, "code").toUpperCase(java.util.Locale.ROOT);
        name = requireText(name, "name");
        categoryCode = requireText(categoryCode, "categoryCode").toUpperCase(java.util.Locale.ROOT);
        description = normalize(description);
        Objects.requireNonNull(serviceType, "serviceType");
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        Objects.requireNonNull(workflow, "workflow");
        feeRules = feeRules == null ? List.of() : List.copyOf(feeRules);
        sla = sla == null ? Sla.none() : sla;
        validity = validity == null ? Validity.permanent() : validity;
        numberingSequenceCode = normalize(numberingSequenceCode);
        notes = normalize(notes);
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String v = value.strip();
        if (v.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return v;
    }

    private static Optional<String> normalize(Optional<String> v) {
        if (v == null || v.isEmpty()) {
            return Optional.empty();
        }
        String s = v.get().strip();
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }
}
