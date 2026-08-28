// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.service;

import java.util.Map;

/**
 * What a procedure provides so a {@link RequirementsChecklist} can be evaluated: which requirement
 * keys are currently satisfied, and the variables its conditional expressions read.
 *
 * <p>The real implementation (backed by uploaded documents, captured fields and payment state)
 * arrives with the procedure aggregate in Phase 5; the checklist logic here does not depend on it.
 */
public interface RequirementContext {

    /** Whether the requirement identified by {@code key} is currently satisfied. */
    boolean isSatisfied(String key);

    /** Typed values a requirement's {@code conditionExpression} may reference. */
    Map<String, Object> variables();

    /** A context where nothing is satisfied and no variables are set. */
    static RequirementContext empty() {
        return new RequirementContext() {
            @Override
            public boolean isSatisfied(String key) {
                return false;
            }

            @Override
            public Map<String, Object> variables() {
                return Map.of();
            }
        };
    }
}
