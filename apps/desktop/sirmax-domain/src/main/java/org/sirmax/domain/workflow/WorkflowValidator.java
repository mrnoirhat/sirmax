// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.workflow;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Structural checks on a {@link WorkflowDefinition} before a service version is published
 * (docs/adr/0007). Returns problem keys (i18n) rather than throwing, so a configuration screen can
 * show all issues at once.
 */
public final class WorkflowValidator {

    private WorkflowValidator() {}

    public static List<String> validate(WorkflowDefinition wf) {
        List<String> problems = new ArrayList<>();
        if (wf.isEmpty()) {
            return List.of(); // an empty workflow is valid (service with no internal steps)
        }

        Map<String, WorkflowStep> byKey = wf.byKey();
        if (byKey.size() != wf.steps().size()) {
            problems.add("workflow.validate.duplicate_step_key");
        }
        if (!byKey.containsKey(wf.firstStepKey())) {
            problems.add("workflow.validate.first_step_missing");
        }

        for (WorkflowStep step : wf.steps()) {
            for (Transition t : step.transitions()) {
                if (!t.isTerminal() && !byKey.containsKey(t.toStepKey().orElse(""))) {
                    problems.add("workflow.validate.transition_target_missing");
                }
            }
            boolean hasForward =
                    step.transitions().stream()
                            .anyMatch(t -> !t.isTerminal() || isEnding(t.kind()));
            if (!hasForward) {
                problems.add("workflow.validate.step_has_no_exit");
            }
        }

        if (byKey.containsKey(wf.firstStepKey()) && !allReachable(wf, byKey)) {
            problems.add("workflow.validate.unreachable_step");
        }

        return List.copyOf(new HashSet<>(problems)).stream().sorted().toList();
    }

    private static boolean isEnding(TransitionKind kind) {
        return kind == TransitionKind.APPROVE
                || kind == TransitionKind.REJECT
                || kind == TransitionKind.CANCEL;
    }

    private static boolean allReachable(WorkflowDefinition wf, Map<String, WorkflowStep> byKey) {
        Set<String> seen = new HashSet<>();
        Queue<String> q = new ArrayDeque<>();
        q.add(wf.firstStepKey());
        seen.add(wf.firstStepKey());
        while (!q.isEmpty()) {
            WorkflowStep s = byKey.get(q.poll());
            if (s == null) {
                continue;
            }
            for (Transition t : s.transitions()) {
                t.toStepKey()
                        .filter(byKey::containsKey)
                        .filter(seen::add)
                        .ifPresent(q::add);
            }
        }
        return seen.size() == byKey.size();
    }
}
