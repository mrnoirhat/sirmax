// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.ProcedureFinance;
import org.sirmax.application.port.ProcedureRepository;
import org.sirmax.application.port.ServiceCatalogRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.procedure.ProcedureChecklist;
import org.sirmax.domain.procedure.ProcedureEvent;
import org.sirmax.domain.procedure.ProcedureOutcome;
import org.sirmax.domain.security.Permission;
import org.sirmax.domain.service.RequirementStage;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.domain.workflow.StepType;
import org.sirmax.domain.workflow.TransitionKind;
import org.sirmax.domain.workflow.WorkflowEngine;
import org.sirmax.domain.workflow.WorkflowStep;
import org.sirmax.shared.Result;

/**
 * Applies one workflow transition to a case (master prompt §18, §28).
 *
 * <p>Three gates run before the case moves, in this order:
 *
 * <ol>
 *   <li><b>Permission</b> — {@code procedure.work} to advance, {@code procedure.decide} to approve,
 *       reject or cancel; a step may additionally name its own required permission.
 *   <li><b>Requirements</b> — every mandatory, applicable requirement due by the *destination*
 *       step's stage must be satisfied or waived. Otherwise the case parks in
 *       {@code WAITING_REQUIREMENTS} and the caller is told how many are missing (§56).
 *   <li><b>Payment</b> — a {@code PAYMENT_CHECKPOINT} step only lets {@code ADVANCE} through once
 *       the invoice is settled (or partly settled where the service allows it).
 * </ol>
 *
 * <p>Terminal transitions close the case with the matching outcome; everything else moves it to the
 * destination step and writes a timeline entry.
 */
public final class AdvanceProcedure implements UseCase<AdvanceProcedure.Command, Procedure> {

    public record Command(
            Session session,
            String procedureId,
            TransitionKind kind,
            Optional<String> reason,
            String source) {}

    private final ProcedureRepository procedures;
    private final ServiceCatalogRepository catalog;
    private final ProcedureFinance finance;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public AdvanceProcedure(
            ProcedureRepository procedures,
            ServiceCatalogRepository catalog,
            ProcedureFinance finance,
            IdGenerator ids,
            Clock clock,
            UnitOfWork unitOfWork,
            Audit audit) {
        this.procedures = procedures;
        this.catalog = catalog;
        this.finance = finance;
        this.ids = ids;
        this.clock = clock;
        this.unitOfWork = unitOfWork;
        this.audit = audit;
    }

    @Override
    public Result<Procedure> execute(Command c) {
        if (!c.session().can(Permission.PROCEDURE_WORK)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        if (isDecision(c.kind()) && !c.session().can(Permission.PROCEDURE_DECIDE)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }

        Optional<Procedure> found = procedures.findById(c.procedureId());
        if (found.isEmpty()) {
            return Result.err("PROCEDURE_NOT_FOUND", "procedure.not_found");
        }
        Procedure procedure = found.get();
        if (procedure.status().isTerminal()) {
            return Result.err("PROCEDURE_CLOSED", "procedure.closed");
        }

        Optional<ServiceDefinitionVersion> version =
                catalog.findVersionById(procedure.serviceVersionId());
        if (version.isEmpty()) {
            return Result.err("VERSION_NOT_FOUND", "service.version_not_found");
        }
        var workflow = version.get().workflow();
        String currentStepKey = procedure.currentStepKey().orElse(null);
        if (currentStepKey == null || workflow.isEmpty()) {
            return Result.err("NO_WORKFLOW", "procedure.no_workflow");
        }
        Optional<WorkflowStep> currentStep = workflow.step(currentStepKey);
        if (currentStep.isEmpty()) {
            return Result.err("STEP_NOT_FOUND", "procedure.step_not_found");
        }
        if (currentStep.get().requiredPermission().isPresent()
                && !hasStepPermission(c.session(), currentStep.get())) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }

        Map<String, Object> variables = variablesWithPayment(procedure);
        if (WorkflowEngine.availableTransitions(workflow, currentStepKey, variables).stream()
                .noneMatch(t -> t.kind() == c.kind())) {
            return payingCheckpointBlocks(currentStep.get(), c.kind(), variables)
                    ? Result.err("PAYMENT_REQUIRED", "procedure.payment_required")
                    : Result.err("TRANSITION_NOT_AVAILABLE", "procedure.transition_unavailable");
        }

        Optional<String> destination =
                WorkflowEngine.next(workflow, currentStepKey, c.kind(), variables);

        // Requirements are checked against where the case is *going*, not where it is.
        RequirementStage targetStage =
                destination
                        .flatMap(workflow::step)
                        .map(AdvanceProcedure::stageOf)
                        .orElse(RequirementStage.DELIVERY);
        ProcedureChecklist checklist =
                ProcedureChecklist.of(
                        procedures.findRequirements(procedure.id()),
                        ProcedureVariables.of(procedure, procedures));
        if (!isCancellation(c.kind()) && !checklist.canReach(targetStage)) {
            unitOfWork.execute(
                    () -> {
                        procedure.blockOnRequirements(clock.now());
                        procedures.save(procedure);
                    });
            return Result.err("REQUIREMENTS_INCOMPLETE", "procedure.requirements_incomplete");
        }

        if (c.kind() == TransitionKind.REJECT && c.reason().map(String::isBlank).orElse(true)) {
            return Result.err("REASON_REQUIRED", "procedure.rejection_needs_reason");
        }

        return Result.ok(
                unitOfWork.execute(() -> doAdvance(c, procedure, currentStepKey, destination)));
    }

    private Procedure doAdvance(
            Command c, Procedure procedure, String fromStep, Optional<String> destination) {
        Instant now = clock.now();
        String userId = c.session().user().id();

        if (destination.isPresent()) {
            procedure.moveToStep(destination.get(), now);
            procedures.appendEvent(
                    ProcedureEvent.stepChange(
                            ids.newId(), procedure.id(), userId, fromStep, destination.get(), now));
            // Landing on an unsettled payment checkpoint parks the case there, so the worklist
            // says "pendiente de pago" rather than a generic "en proceso".
            if (isUnpaidCheckpoint(procedure, destination.get())) {
                procedure.blockOnPayment(now);
            }
        } else {
            ProcedureOutcome outcome =
                    switch (c.kind()) {
                        case APPROVE, ADVANCE -> ProcedureOutcome.APPROVED;
                        case REJECT -> ProcedureOutcome.REJECTED;
                        case CANCEL -> ProcedureOutcome.CANCELLED;
                        case RETURN_FOR_CORRECTION, REASSIGN -> null;
                    };
            if (outcome == null) {
                // A terminal RETURN/REASSIGN makes no sense; the validator rejects such workflows,
                // so treat it as a no-op move rather than inventing an outcome.
                procedure.moveToStep(fromStep, now);
            } else {
                procedure.decide(outcome, c.reason().orElse(null), now);
                procedures.appendEvent(
                        ProcedureEvent.of(
                                ids.newId(),
                                procedure.id(),
                                org.sirmax.domain.procedure.ProcedureEventKind.DECIDED,
                                userId,
                                outcome.name() + c.reason().map(r -> " — " + r).orElse(""),
                                now));
            }
        }
        procedures.save(procedure);

        audit.record(
                c.session().audit(c.source()),
                "procedure.advanced",
                "Procedure",
                procedure.id(),
                fromStep,
                destination.orElse(procedure.status().name()),
                c.reason().orElse(null));
        return procedure;
    }

    /** {@code true} when the destination step holds for money that has not arrived. */
    private boolean isUnpaidCheckpoint(Procedure procedure, String stepKey) {
        return catalog.findVersionById(procedure.serviceVersionId())
                .flatMap(v -> v.workflow().step(stepKey))
                .filter(WorkflowStep::isPaymentCheckpoint)
                .map(step -> !finance.stateOf(procedure.id()).paid())
                .orElse(false);
    }

    /** Payment facts are merged into the rule variables so guards can read {@code paid}. */
    private Map<String, Object> variablesWithPayment(Procedure procedure) {
        ProcedureFinance.PaymentState state = finance.stateOf(procedure.id());
        Map<String, Object> payment = new HashMap<>();
        payment.put("invoiced", state.invoiced());
        payment.put("paid", state.paid());
        payment.put("partiallyPaid", state.partiallyPaid());
        return ProcedureVariables.of(procedure, procedures, payment);
    }

    /** Distinguishes "the workflow has no such transition" from "you must collect the money first". */
    private static boolean payingCheckpointBlocks(
            WorkflowStep step, TransitionKind kind, Map<String, Object> variables) {
        return step.isPaymentCheckpoint()
                && kind == TransitionKind.ADVANCE
                && !Boolean.TRUE.equals(variables.get("paid"))
                && step.transition(TransitionKind.ADVANCE).isPresent();
    }

    private static RequirementStage stageOf(WorkflowStep step) {
        return switch (step.type()) {
            case TASK -> RequirementStage.INTAKE;
            case REVIEW, INSPECTION -> RequirementStage.REVIEW;
            case APPROVAL -> RequirementStage.APPROVAL;
            case PAYMENT_CHECKPOINT -> RequirementStage.DECISION;
            case DOCUMENT_OUTPUT -> RequirementStage.DELIVERY;
        };
    }

    private static boolean isDecision(TransitionKind kind) {
        return kind == TransitionKind.APPROVE
                || kind == TransitionKind.REJECT
                || kind == TransitionKind.CANCEL;
    }

    private static boolean isCancellation(TransitionKind kind) {
        return kind == TransitionKind.CANCEL;
    }

    private static boolean hasStepPermission(Session session, WorkflowStep step) {
        return step.requiredPermission()
                .flatMap(Permission::fromKey)
                .map(session::can)
                .orElse(true);
    }
}
