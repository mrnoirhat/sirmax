// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WorkflowEngineTest {

    /** Intake → Review → Payment(checkpoint) → Issue(document output). */
    private static WorkflowDefinition sample() {
        WorkflowStep intake =
                new WorkflowStep(
                        "intake", "Recepción", StepType.TASK, Optional.empty(), 1,
                        List.of(Transition.advance("review"), Transition.terminal(TransitionKind.CANCEL)));
        WorkflowStep review =
                new WorkflowStep(
                        "review", "Revisión", StepType.REVIEW, Optional.of("procedure.work"), 3,
                        List.of(
                                new Transition(TransitionKind.APPROVE, Optional.of("payment"), Optional.empty()),
                                new Transition(
                                        TransitionKind.RETURN_FOR_CORRECTION,
                                        Optional.of("intake"),
                                        Optional.empty()),
                                Transition.terminal(TransitionKind.REJECT)));
        WorkflowStep payment =
                new WorkflowStep(
                        "payment", "Pago", StepType.PAYMENT_CHECKPOINT, Optional.empty(), 0,
                        List.of(Transition.advance("issue")));
        WorkflowStep issue =
                new WorkflowStep(
                        "issue", "Emisión", StepType.DOCUMENT_OUTPUT, Optional.empty(), 0,
                        List.of(Transition.terminal(TransitionKind.APPROVE)));
        return new WorkflowDefinition("intake", List.of(intake, review, payment, issue));
    }

    @Test
    void definitionValidates() {
        assertThat(WorkflowValidator.validate(sample())).isEmpty();
    }

    @Test
    void validatorCatchesDanglingTransitionAndUnreachableStep() {
        WorkflowStep a =
                new WorkflowStep(
                        "a", "A", StepType.TASK, Optional.empty(), 0,
                        List.of(Transition.advance("ghost")));
        WorkflowStep orphan =
                new WorkflowStep(
                        "orphan", "O", StepType.TASK, Optional.empty(), 0,
                        List.of(Transition.terminal(TransitionKind.APPROVE)));
        List<String> problems =
                WorkflowValidator.validate(new WorkflowDefinition("a", List.of(a, orphan)));
        assertThat(problems)
                .contains(
                        "workflow.validate.transition_target_missing",
                        "workflow.validate.unreachable_step");
    }

    @Test
    void availableTransitionsRespectStepAndPaymentGuard() {
        WorkflowDefinition wf = sample();

        assertThat(
                        WorkflowEngine.availableTransitions(wf, "intake", Map.of()).stream()
                                .map(Transition::kind))
                .containsExactly(TransitionKind.ADVANCE, TransitionKind.CANCEL);

        // payment checkpoint: ADVANCE hidden until paid
        assertThat(WorkflowEngine.availableTransitions(wf, "payment", Map.of())).isEmpty();
        assertThat(WorkflowEngine.availableTransitions(wf, "payment", Map.of("paid", true)))
                .hasSize(1);
        assertThat(
                        WorkflowEngine.availableTransitions(
                                wf,
                                "payment",
                                Map.of("partiallyPaid", true, "partialPaymentAllowed", true)))
                .hasSize(1);
    }

    @Test
    void nextResolvesTheTargetOrTerminal() {
        WorkflowDefinition wf = sample();
        assertThat(WorkflowEngine.next(wf, "intake", TransitionKind.ADVANCE, Map.of()))
                .contains("review");
        assertThat(WorkflowEngine.next(wf, "review", TransitionKind.APPROVE, Map.of()))
                .contains("payment");
        assertThat(WorkflowEngine.next(wf, "review", TransitionKind.REJECT, Map.of())).isEmpty();
        assertThat(WorkflowEngine.next(wf, "payment", TransitionKind.ADVANCE, Map.of("paid", true)))
                .contains("issue");

        assertThatThrownBy(
                        () -> WorkflowEngine.next(wf, "payment", TransitionKind.ADVANCE, Map.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void conditionalTransitionOnlyOfferedWhenExpressionHolds() {
        WorkflowStep triage =
                new WorkflowStep(
                        "triage", "Triaje", StepType.TASK, Optional.empty(), 0,
                        List.of(
                                Transition.advance("fast").when("monto <= 1000"),
                                Transition.advance("full").when("monto > 1000")));
        WorkflowStep fast = WorkflowStep.task("fast", "Rápida", "done");
        WorkflowStep full = WorkflowStep.task("full", "Completa", "done");
        WorkflowStep done =
                new WorkflowStep(
                        "done", "Fin", StepType.DOCUMENT_OUTPUT, Optional.empty(), 0,
                        List.of(Transition.terminal(TransitionKind.APPROVE)));
        WorkflowDefinition wf =
                new WorkflowDefinition("triage", List.of(triage, fast, full, done));

        assertThat(WorkflowEngine.next(wf, "triage", TransitionKind.ADVANCE, Map.of("monto", 500)))
                .contains("fast");
        assertThat(WorkflowEngine.next(wf, "triage", TransitionKind.ADVANCE, Map.of("monto", 5000)))
                .contains("full");
    }
}
