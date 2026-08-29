// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.ProcedureRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.procedure.ProcedureChecklist;
import org.sirmax.domain.procedure.ProcedureEvent;
import org.sirmax.domain.procedure.ProcedureEventKind;
import org.sirmax.domain.procedure.ProcedureRequirementItem;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Result;

/**
 * Ticks, unticks or waives one line of a case's requirement checklist (master prompt §56).
 *
 * <p>Waiving needs {@code procedure.decide}: accepting a case without a mandatory document is a
 * supervisory act, and it always carries a reason that lands in the timeline and the audit log.
 *
 * <p>Returns the recomputed checklist so the caller can render "Faltan N requisitos" without a
 * second round-trip.
 */
public final class UpdateProcedureRequirement
        implements UseCase<UpdateProcedureRequirement.Command, ProcedureChecklist> {

    /** What the operator did to the checklist line. */
    public enum Action {
        SATISFY,
        UNSATISFY,
        WAIVE
    }

    public record Command(
            Session session,
            String procedureId,
            String requirementKey,
            Action action,
            Optional<String> note,
            String source) {}

    private final ProcedureRepository procedures;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public UpdateProcedureRequirement(
            ProcedureRepository procedures,
            IdGenerator ids,
            Clock clock,
            UnitOfWork unitOfWork,
            Audit audit) {
        this.procedures = procedures;
        this.ids = ids;
        this.clock = clock;
        this.unitOfWork = unitOfWork;
        this.audit = audit;
    }

    @Override
    public Result<ProcedureChecklist> execute(Command c) {
        if (!c.session().can(Permission.PROCEDURE_WORK)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        if (c.action() == Action.WAIVE && !c.session().can(Permission.PROCEDURE_DECIDE)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }

        Optional<Procedure> procedure = procedures.findById(c.procedureId());
        if (procedure.isEmpty()) {
            return Result.err("PROCEDURE_NOT_FOUND", "procedure.not_found");
        }
        if (procedure.get().status().isTerminal()) {
            return Result.err("PROCEDURE_CLOSED", "procedure.closed");
        }

        Optional<ProcedureRequirementItem> item =
                procedures.findRequirement(c.procedureId(), c.requirementKey());
        if (item.isEmpty()) {
            return Result.err("REQUIREMENT_NOT_FOUND", "procedure.requirement_not_found");
        }
        if (c.action() == Action.WAIVE && c.note().map(String::isBlank).orElse(true)) {
            return Result.err("REASON_REQUIRED", "procedure.waiver_needs_reason");
        }

        return Result.ok(unitOfWork.execute(() -> doUpdate(c, procedure.get(), item.get())));
    }

    private ProcedureChecklist doUpdate(
            Command c, Procedure procedure, ProcedureRequirementItem item) {
        Instant now = clock.now();
        String userId = c.session().user().id();

        switch (c.action()) {
            case SATISFY -> item.markSatisfied(userId, c.note().orElse(null), now);
            case UNSATISFY -> item.markMissing(now);
            case WAIVE -> item.waive(userId, c.note().orElse(null), now);
        }
        procedures.saveRequirement(item);

        List<ProcedureRequirementItem> all = procedures.findRequirements(procedure.id());
        ProcedureChecklist checklist =
                ProcedureChecklist.of(all, ProcedureVariables.of(procedure, procedures));

        // Keep the coarse status honest: a case blocked only on paperwork resumes as soon as the
        // last mandatory gap closes, and re-blocks if the operator unticks one.
        if (checklist.isComplete()) {
            procedure.resume(now);
        } else if (!procedure.status().isBlocked()) {
            procedure.blockOnRequirements(now);
        }
        procedures.save(procedure);

        procedures.appendEvent(
                ProcedureEvent.of(
                        ids.newId(),
                        procedure.id(),
                        ProcedureEventKind.REQUIREMENT_UPDATED,
                        userId,
                        item.label() + " — " + c.action(),
                        now));

        audit.record(
                c.session().audit(c.source()),
                "procedure.requirement." + c.action().name().toLowerCase(java.util.Locale.ROOT),
                "Procedure",
                procedure.id(),
                null,
                null,
                c.note().orElse(null));
        return checklist;
    }
}
