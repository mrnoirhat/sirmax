// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.NumberingRepository;
import org.sirmax.application.port.ProcedureRepository;
import org.sirmax.application.port.ServiceCatalogRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.procedure.DueDates;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.procedure.ProcedureEvent;
import org.sirmax.domain.procedure.ProcedureEventKind;
import org.sirmax.domain.procedure.ProcedureRequirementItem;
import org.sirmax.domain.security.Permission;
import org.sirmax.domain.service.RequirementDef;
import org.sirmax.domain.service.ServiceDefinition;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.shared.Result;

/**
 * Opens a case for a citizen against a service (master prompt §16, §33 — "Nuevo trámite").
 *
 * <p>The case is bound to the service's currently ACTIVE version and keeps that binding for life
 * (§39): requirements, workflow and fees are read from the version the case was opened with, never
 * from whatever the service looks like later.
 *
 * <p>Opening materializes the checklist, allocates the public case number from the version's
 * numbering sequence (or the shared {@code TRM} one), derives the SLA due date and writes the first
 * timeline entry — all in one transaction, so a failure leaves no half-open case and burns no
 * number.
 */
public final class StartProcedure implements UseCase<StartProcedure.Command, Procedure> {

    /** The default sequence for cases whose service does not name one of its own (§27). */
    private static final String DEFAULT_SEQUENCE = "TRM";

    private static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();

    public record Command(
            Session session,
            String serviceDefinitionId,
            PartyRef applicant,
            Optional<String> subjectType,
            Optional<String> subjectId,
            Optional<String> departmentId,
            Optional<String> notes,
            String source) {}

    private final ProcedureRepository procedures;
    private final ServiceCatalogRepository catalog;
    private final NumberingRepository numbering;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public StartProcedure(
            ProcedureRepository procedures,
            ServiceCatalogRepository catalog,
            NumberingRepository numbering,
            IdGenerator ids,
            Clock clock,
            UnitOfWork unitOfWork,
            Audit audit) {
        this.procedures = procedures;
        this.catalog = catalog;
        this.numbering = numbering;
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

        Optional<ServiceDefinition> definition = catalog.findDefinitionById(c.serviceDefinitionId());
        if (definition.isEmpty()) {
            return Result.err("SERVICE_NOT_FOUND", "service.not_found");
        }
        if (!definition.get().isAvailable()) {
            return Result.err("SERVICE_UNAVAILABLE", "service.unavailable");
        }

        Optional<ServiceDefinitionVersion> active =
                catalog.findActiveVersion(c.serviceDefinitionId());
        if (active.isEmpty()) {
            return Result.err("NO_ACTIVE_VERSION", "service.no_active_version");
        }

        return Result.ok(unitOfWork.execute(() -> doStart(c, definition.get(), active.get())));
    }

    private Procedure doStart(
            Command c, ServiceDefinition definition, ServiceDefinitionVersion version) {
        Instant now = clock.now();
        LocalDate today = LocalDate.ofInstant(now, LOCAL_ZONE);

        String sequenceCode = version.numberingSequenceCode().orElse(DEFAULT_SEQUENCE);
        String code = numbering.allocate(sequenceCode, sequenceCode, today.getYear());

        String firstStep = version.workflow().firstStep().map(s -> s.key()).orElse(null);
        LocalDate dueDate = DueDates.dueDateFor(version.sla(), today).orElse(null);

        Procedure procedure =
                Procedure.open(
                        ids.newId(),
                        code,
                        definition.id(),
                        version.id(),
                        c.applicant(),
                        firstStep,
                        dueDate,
                        now);
        c.subjectType()
                .ifPresent(type -> procedure.setSubject(type, c.subjectId().orElse(null), now));
        c.notes().ifPresent(n -> procedure.setNotes(n, now));
        String department = c.departmentId().orElseGet(() -> definition.departmentId().orElse(null));
        if (department != null) {
            procedure.assign(department, null, now);
        }
        procedures.save(procedure);

        for (RequirementDef def : version.requirements()) {
            procedures.saveRequirement(
                    ProcedureRequirementItem.from(ids.newId(), procedure.id(), def));
        }

        procedures.appendEvent(
                ProcedureEvent.of(
                        ids.newId(),
                        procedure.id(),
                        ProcedureEventKind.OPENED,
                        c.session().user().id(),
                        definition.name(),
                        now));

        audit.record(c.session().audit(c.source()), "procedure.opened", "Procedure", procedure.id());
        return procedure;
    }
}
