// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.workflow;

import java.util.Objects;
import java.util.Optional;

/**
 * An edge out of a {@link WorkflowStep}.
 *
 * @param kind which transition this is
 * @param toStepKey the target step key; empty means the procedure ends (approved/rejected/cancelled)
 * @param condition optional restricted expression; the transition is only offered when it holds
 */
public record Transition(
        TransitionKind kind, Optional<String> toStepKey, Optional<String> condition) {

    public Transition {
        Objects.requireNonNull(kind, "kind");
        toStepKey = normalize(toStepKey);
        condition = normalize(condition);
    }

    public static Transition advance(String toStepKey) {
        return new Transition(TransitionKind.ADVANCE, Optional.of(toStepKey), Optional.empty());
    }

    public static Transition terminal(TransitionKind kind) {
        return new Transition(kind, Optional.empty(), Optional.empty());
    }

    public static Transition to(String toStepKey) {
        return new Transition(TransitionKind.ADVANCE, Optional.of(toStepKey), Optional.empty());
    }

    public boolean isTerminal() {
        return toStepKey.isEmpty();
    }

    public Transition when(String expression) {
        return new Transition(kind, toStepKey, Optional.ofNullable(expression));
    }

    private static Optional<String> normalize(Optional<String> v) {
        if (v == null || v.isEmpty()) {
            return Optional.empty();
        }
        String s = v.get().strip();
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }
}
