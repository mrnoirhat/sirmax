// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.workflow;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sirmax.domain.rules.ExpressionEvaluator;

/**
 * Pure transition resolution for a {@link WorkflowDefinition} (docs/adr/0007).
 *
 * <p>Stateless: given the current step key and a typed context (procedure data, requirements met,
 * fee calculated, payment registered), it says which transitions are currently offered and where a
 * chosen transition leads. Execution state (history, current step) lives on the procedure aggregate
 * in Phase 5.
 *
 * <p>Guard: a {@link StepType#PAYMENT_CHECKPOINT} step does not offer {@code ADVANCE} until the
 * context key {@code "paid"} is true (or {@code "partiallyPaid"} when the service allows it).
 */
public final class WorkflowEngine {

    private WorkflowEngine() {}

    /** The transitions offered from {@code currentStepKey} given {@code context}. */
    public static List<Transition> availableTransitions(
            WorkflowDefinition wf, String currentStepKey, Map<String, Object> context) {
        WorkflowStep step = wf.step(currentStepKey).orElse(null);
        if (step == null) {
            return List.of();
        }
        return step.transitions().stream()
                .filter(t -> conditionHolds(t, context))
                .filter(t -> !blockedByPayment(step, t, context))
                .toList();
    }

    /**
     * Resolve where a chosen transition leads.
     *
     * @return the next step key, or empty if the transition ends the procedure
     * @throws IllegalStateException if the transition is not currently available
     */
    public static Optional<String> next(
            WorkflowDefinition wf,
            String currentStepKey,
            TransitionKind kind,
            Map<String, Object> context) {
        Transition chosen =
                availableTransitions(wf, currentStepKey, context).stream()
                        .filter(t -> t.kind() == kind)
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Transition "
                                                        + kind
                                                        + " is not available from step "
                                                        + currentStepKey));
        return chosen.toStepKey();
    }

    public static boolean isTerminalStep(WorkflowDefinition wf, String stepKey) {
        return wf.step(stepKey)
                .map(s -> s.transitions().stream().allMatch(Transition::isTerminal))
                .orElse(true);
    }

    private static boolean conditionHolds(Transition t, Map<String, Object> context) {
        return t.condition()
                .map(expr -> ExpressionEvaluator.evaluate(expr, context))
                .orElse(true);
    }

    private static boolean blockedByPayment(
            WorkflowStep step, Transition t, Map<String, Object> context) {
        if (step.type() != StepType.PAYMENT_CHECKPOINT || t.kind() != TransitionKind.ADVANCE) {
            return false;
        }
        boolean paid = Boolean.TRUE.equals(context.get("paid"));
        boolean partial =
                Boolean.TRUE.equals(context.get("partiallyPaid"))
                        && Boolean.TRUE.equals(context.get("partialPaymentAllowed"));
        return !(paid || partial);
    }
}
