// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.util.Optional;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.ProcedureRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.procedure.ProcedureEvent;
import org.sirmax.domain.procedure.ProcedureEventKind;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Result;

/**
 * Appends a note to the case timeline (master prompt §16 "Notes").
 *
 * <p>Notes are append-only: correcting one means writing another. That is what makes the timeline
 * usable as a record of what the office actually did.
 */
public final class AddProcedureNote implements UseCase<AddProcedureNote.Command, ProcedureEvent> {

    public record Command(Session session, String procedureId, String text, String source) {}

    private final ProcedureRepository procedures;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public AddProcedureNote(
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
    public Result<ProcedureEvent> execute(Command c) {
        if (!c.session().can(Permission.PROCEDURE_WORK)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        if (c.text() == null || c.text().isBlank()) {
            return Result.err("EMPTY_NOTE", "procedure.note_empty");
        }
        Optional<Procedure> procedure = procedures.findById(c.procedureId());
        if (procedure.isEmpty()) {
            return Result.err("PROCEDURE_NOT_FOUND", "procedure.not_found");
        }

        return Result.ok(
                unitOfWork.execute(
                        () -> {
                            ProcedureEvent event =
                                    ProcedureEvent.of(
                                            ids.newId(),
                                            c.procedureId(),
                                            ProcedureEventKind.NOTE,
                                            c.session().user().id(),
                                            c.text().strip(),
                                            clock.now());
                            procedures.appendEvent(event);
                            audit.record(
                                    c.session().audit(c.source()),
                                    "procedure.note_added",
                                    "Procedure",
                                    c.procedureId());
                            return event;
                        }));
    }
}
