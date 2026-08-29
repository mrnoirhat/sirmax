// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.NumberingRepository;
import org.sirmax.application.port.ProcedureRepository;
import org.sirmax.application.port.RegistryRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.procedure.ProcedureEvent;
import org.sirmax.domain.procedure.ProcedureEventKind;
import org.sirmax.domain.registry.Inspection;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Result;

/**
 * Scheduling and completing a site visit (master prompt §29).
 *
 * <p>Both halves live here because they are one activity with one set of collaborators. Completing
 * a visit writes its result onto the case timeline, so the operator reading the case sees the
 * inspection outcome without opening a second screen.
 *
 * <p>Scheduling needs {@code procedure.work}; recording the verdict needs {@code procedure.decide},
 * because a passed inspection is what a permit rests on.
 */
public final class ConductInspection {

    private static final String SEQUENCE = "INSP";
    private static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();

    public record ScheduleCommand(
            Session session,
            String procedureId,
            Optional<String> inspectorUserId,
            Optional<LocalDate> scheduledDate,
            Optional<String> assetId,
            String source) {}

    public record CompleteCommand(
            Session session,
            String inspectionId,
            Inspection.Result result,
            Optional<String> findings,
            List<Inspection.ChecklistAnswer> checklist,
            Optional<LocalDate> followUpDate,
            String source) {}

    private final RegistryRepository registry;
    private final ProcedureRepository procedures;
    private final NumberingRepository numbering;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public ConductInspection(
            RegistryRepository registry,
            ProcedureRepository procedures,
            NumberingRepository numbering,
            IdGenerator ids,
            Clock clock,
            UnitOfWork unitOfWork,
            Audit audit) {
        this.registry = registry;
        this.procedures = procedures;
        this.numbering = numbering;
        this.ids = ids;
        this.clock = clock;
        this.unitOfWork = unitOfWork;
        this.audit = audit;
    }

    public Result<Inspection> schedule(ScheduleCommand c) {
        if (!c.session().can(Permission.PROCEDURE_WORK)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        Optional<Procedure> procedure = procedures.findById(c.procedureId());
        if (procedure.isEmpty()) {
            return Result.err("PROCEDURE_NOT_FOUND", "procedure.not_found");
        }
        if (procedure.get().status().isTerminal()) {
            return Result.err("PROCEDURE_CLOSED", "procedure.closed");
        }

        return Result.ok(unitOfWork.execute(() -> doSchedule(c)));
    }

    private Inspection doSchedule(ScheduleCommand c) {
        Instant now = clock.now();
        String code =
                numbering.allocate(
                        SEQUENCE, SEQUENCE, LocalDate.ofInstant(now, LOCAL_ZONE).getYear());

        Inspection inspection =
                Inspection.schedule(
                        ids.newId(),
                        code,
                        c.procedureId(),
                        c.inspectorUserId().orElse(null),
                        c.scheduledDate().orElse(null),
                        now);
        c.assetId().ifPresent(assetId -> inspection.relateToAsset(assetId, now));
        registry.save(inspection);

        procedures.appendEvent(
                ProcedureEvent.of(
                        ids.newId(),
                        c.procedureId(),
                        ProcedureEventKind.NOTE,
                        c.session().user().id(),
                        code + c.scheduledDate().map(d -> " · " + d).orElse(""),
                        now));
        audit.record(
                c.session().audit(c.source()),
                "inspection.scheduled",
                "Inspection",
                inspection.id(),
                null,
                code,
                null);
        return inspection;
    }

    public Result<Inspection> complete(CompleteCommand c) {
        if (!c.session().can(Permission.PROCEDURE_DECIDE)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        Optional<Inspection> found = registry.findInspectionById(c.inspectionId());
        if (found.isEmpty()) {
            return Result.err("INSPECTION_NOT_FOUND", "inspection.not_found");
        }
        Inspection inspection = found.get();
        if (inspection.status().isFinished()) {
            return Result.err("ALREADY_COMPLETED", "inspection.already_completed");
        }
        boolean needsFindings =
                c.result() != Inspection.Result.PASSED
                        && c.result() != Inspection.Result.NOT_APPLICABLE;
        if (needsFindings && c.findings().map(String::isBlank).orElse(true)) {
            return Result.err("FINDINGS_REQUIRED", "inspection.findings_required");
        }

        return Result.ok(unitOfWork.execute(() -> doComplete(c, inspection)));
    }

    private Inspection doComplete(CompleteCommand c, Inspection inspection) {
        Instant now = clock.now();
        inspection.recordChecklist(c.checklist(), now);
        inspection.complete(
                c.result(), c.findings().orElse(null), c.followUpDate().orElse(null), now);
        registry.save(inspection);

        procedures.appendEvent(
                ProcedureEvent.of(
                        ids.newId(),
                        inspection.procedureId(),
                        ProcedureEventKind.NOTE,
                        c.session().user().id(),
                        inspection.code() + " · " + c.result(),
                        now));
        audit.record(
                c.session().audit(c.source()),
                "inspection.completed",
                "Inspection",
                inspection.id(),
                null,
                c.result().name(),
                c.findings().orElse(null));
        return inspection;
    }
}
