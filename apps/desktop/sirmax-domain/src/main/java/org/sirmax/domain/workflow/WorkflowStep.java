// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.workflow;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One step of a {@link WorkflowDefinition} (docs/adr/0007).
 *
 * @param key stable key within the workflow, e.g. {@code "revision_tecnica"}
 * @param label operator-facing label (administrator-authored data)
 * @param type step kind
 * @param requiredPermission optional permission key the actor must hold to work this step
 * @param slaDays optional target days for this step alone (0 / empty = none)
 * @param transitions the edges out of this step
 */
public record WorkflowStep(
        String key,
        String label,
        StepType type,
        Optional<String> requiredPermission,
        int slaDays,
        List<Transition> transitions) {

    public WorkflowStep {
        key = requireKey(key);
        label = requireText(label, "label");
        Objects.requireNonNull(type, "type");
        requiredPermission = normalize(requiredPermission);
        if (slaDays < 0) {
            throw new IllegalArgumentException("slaDays must be >= 0");
        }
        transitions = transitions == null ? List.of() : List.copyOf(transitions);
    }

    public static WorkflowStep task(String key, String label, String toNext) {
        return new WorkflowStep(
                key, label, StepType.TASK, Optional.empty(), 0, List.of(Transition.advance(toNext)));
    }

    public boolean isPaymentCheckpoint() {
        return type == StepType.PAYMENT_CHECKPOINT;
    }

    public Optional<Transition> transition(TransitionKind kind) {
        return transitions.stream().filter(t -> t.kind() == kind).findFirst();
    }

    private static String requireKey(String key) {
        String k = requireText(key, "key").toLowerCase(java.util.Locale.ROOT);
        if (!k.matches("[a-z0-9_]{1,40}")) {
            throw new IllegalArgumentException("key must be 1–40 chars of a–z, 0–9 or '_'");
        }
        return k;
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
