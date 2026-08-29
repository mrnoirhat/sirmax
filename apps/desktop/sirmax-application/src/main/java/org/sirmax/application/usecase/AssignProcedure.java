// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.util.Optional;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.ProcedureRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.port.UserRepository;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.procedure.Priority;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.procedure.ProcedureEvent;
import org.sirmax.domain.procedure.ProcedureEventKind;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Result;

/**
 * Routes a case to a department and/or an operator, and optionally re-prioritizes it (master prompt
 * §57 — queues are what an operator actually works from).
 *
 * <p>Passing an empty {@code assignedUserId} un-assigns the case, returning it to the shared queue.
 */
public final class AssignProcedure implements UseCase<AssignProcedure.Command, Procedure> {

    public record Command(
            Session session,
            String procedureId,
            Optional<String> departmentId,
            Optional<String> assignedUserId,
            Optional<Priority> priority,
            String source) {}

    private final ProcedureRepository procedures;
    private final UserRepository users;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public AssignProcedure(
            ProcedureRepository procedures,
            UserRepository users,
            IdGenerator ids,
            Clock clock,
            UnitOfWork unitOfWork,
            Audit audit) {
        this.procedures = procedures;
        this.users = users;
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
        Optional<Procedure> found = procedures.findById(c.procedureId());
        if (found.isEmpty()) {
            return Result.err("PROCEDURE_NOT_FOUND", "procedure.not_found");
        }
        if (found.get().status().isTerminal()) {
            return Result.err("PROCEDURE_CLOSED", "procedure.closed");
        }
        if (c.assignedUserId().isPresent()
                && users.findById(c.assignedUserId().get()).isEmpty()) {
            return Result.err("USER_NOT_FOUND", "user.not_found");
        }

        return Result.ok(unitOfWork.execute(() -> doAssign(c, found.get())));
    }

    private Procedure doAssign(Command c, Procedure procedure) {
        Instant now = clock.now();
        procedure.assign(
                c.departmentId().orElse(procedure.departmentId().orElse(null)),
                c.assignedUserId().orElse(null),
                now);
        c.priority().ifPresent(p -> procedure.setPriority(p, now));
        procedures.save(procedure);

        procedures.appendEvent(
                ProcedureEvent.of(
                        ids.newId(),
                        procedure.id(),
                        ProcedureEventKind.ASSIGNED,
                        c.session().user().id(),
                        c.assignedUserId().orElse("—"),
                        now));
        audit.record(
                c.session().audit(c.source()), "procedure.assigned", "Procedure", procedure.id());
        return procedure;
    }
}
