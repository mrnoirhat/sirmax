// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.ProcedureRepository;
import org.sirmax.application.port.ServiceCatalogRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.procedure.ProcedureEvent;
import org.sirmax.domain.procedure.ProcedureEventKind;
import org.sirmax.domain.security.Permission;
import org.sirmax.domain.service.FormField;
import org.sirmax.domain.service.FormSchema;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.shared.Result;

/**
 * Saves the answers to a service's dynamic form (master prompt §16, ADR 0006).
 *
 * <p>Validation runs against the {@link FormSchema} of the version the case was opened with:
 * unknown keys are rejected rather than silently stored, required fields must be present, and
 * NUMBER/MONEY/DATE/BOOLEAN/SELECT values must parse. The result is the list of i18n problem keys —
 * empty when the form saved cleanly.
 */
public final class SaveProcedureForm implements UseCase<SaveProcedureForm.Command, List<String>> {

    public record Command(
            Session session, String procedureId, Map<String, String> values, String source) {}

    private final ProcedureRepository procedures;
    private final ServiceCatalogRepository catalog;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public SaveProcedureForm(
            ProcedureRepository procedures,
            ServiceCatalogRepository catalog,
            IdGenerator ids,
            Clock clock,
            UnitOfWork unitOfWork,
            Audit audit) {
        this.procedures = procedures;
        this.catalog = catalog;
        this.ids = ids;
        this.clock = clock;
        this.unitOfWork = unitOfWork;
        this.audit = audit;
    }

    @Override
    public Result<List<String>> execute(Command c) {
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

        Optional<ServiceDefinitionVersion> version =
                catalog.findVersionById(procedure.get().serviceVersionId());
        if (version.isEmpty()) {
            return Result.err("VERSION_NOT_FOUND", "service.version_not_found");
        }

        List<String> problems = validate(version.get().formSchema(), c.values());
        if (!problems.isEmpty()) {
            return Result.ok(problems);
        }

        unitOfWork.execute(() -> doSave(c, procedure.get()));
        return Result.ok(List.of());
    }

    /** Public so a form editor can validate as the operator types, without saving. */
    public static List<String> validate(FormSchema schema, Map<String, String> values) {
        List<String> problems = new ArrayList<>();
        Map<String, FormField> byKey = new LinkedHashMap<>();
        for (FormField field : schema.fields()) {
            byKey.put(field.key(), field);
        }

        for (String key : values.keySet()) {
            if (!byKey.containsKey(key)) {
                problems.add("form.unknown_field:" + key);
            }
        }

        for (FormField field : schema.fields()) {
            String raw = values.get(field.key());
            boolean missing = raw == null || raw.isBlank();
            if (field.required() && missing) {
                problems.add("form.required:" + field.key());
                continue;
            }
            if (missing) {
                continue;
            }
            String value = raw.strip();
            switch (field.type()) {
                case NUMBER, MONEY -> {
                    try {
                        new java.math.BigDecimal(value);
                    } catch (NumberFormatException e) {
                        problems.add("form.not_a_number:" + field.key());
                    }
                }
                case DATE -> {
                    try {
                        java.time.LocalDate.parse(value);
                    } catch (java.time.format.DateTimeParseException e) {
                        problems.add("form.not_a_date:" + field.key());
                    }
                }
                case BOOLEAN -> {
                    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                        problems.add("form.not_a_boolean:" + field.key());
                    }
                }
                case SELECT -> {
                    boolean known =
                            field.options().stream()
                                    .anyMatch(o -> o.value().equals(value));
                    if (!known) {
                        problems.add("form.unknown_option:" + field.key());
                    }
                }
                case TEXT, TEXT_AREA, PARTY_REF, PROPERTY_REF -> {
                    // free text and references are validated by the module that consumes them
                }
            }
        }
        return List.copyOf(problems);
    }

    private Void doSave(Command c, Procedure procedure) {
        Instant now = clock.now();
        procedures.saveFormValues(procedure.id(), c.values());
        procedure.setNotes(procedure.notes().orElse(null), now); // bump updatedAt
        procedures.save(procedure);

        procedures.appendEvent(
                ProcedureEvent.of(
                        ids.newId(),
                        procedure.id(),
                        ProcedureEventKind.FORM_UPDATED,
                        c.session().user().id(),
                        c.values().size() + " campo(s)",
                        now));
        audit.record(
                c.session().audit(c.source()), "procedure.form_saved", "Procedure", procedure.id());
        return null;
    }
}
