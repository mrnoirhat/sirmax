// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.workflow;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An ordered, data-driven workflow with a closed vocabulary (docs/adr/0007) — not a generic engine.
 *
 * <p>Held as a typed model on a published service version. Structural validity is checked by {@link
 * WorkflowValidator}; transition resolution at runtime is {@link WorkflowEngine}.
 */
public record WorkflowDefinition(String firstStepKey, List<WorkflowStep> steps) {

    public WorkflowDefinition {
        Objects.requireNonNull(firstStepKey, "firstStepKey");
        steps = steps == null ? List.of() : List.copyOf(steps);
    }

    /** An empty workflow (a service that has no internal steps). */
    public static WorkflowDefinition empty() {
        return new WorkflowDefinition("", List.of());
    }

    public boolean isEmpty() {
        return steps.isEmpty();
    }

    public Optional<WorkflowStep> step(String key) {
        return steps.stream().filter(s -> s.key().equals(key)).findFirst();
    }

    public Optional<WorkflowStep> firstStep() {
        return step(firstStepKey);
    }

    Map<String, WorkflowStep> byKey() {
        Map<String, WorkflowStep> m = new LinkedHashMap<>();
        for (WorkflowStep s : steps) {
            m.put(s.key(), s);
        }
        return m;
    }
}
