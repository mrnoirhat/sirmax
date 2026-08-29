// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.util.HashMap;
import java.util.Map;
import org.sirmax.application.port.ProcedureRepository;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.procedure.ProcedureRequirementItem;

/**
 * Builds the variable map that conditional requirements and workflow guards evaluate against
 * (master prompt §17, §18).
 *
 * <p>The vocabulary is deliberately small and stable, because service administrators write
 * expressions against it by hand:
 *
 * <ul>
 *   <li>every form field, by its key — {@code tipo_solicitante == 'propietario'}
 *   <li>{@code requisito_<key>} — whether that checklist line is satisfied
 *   <li>{@code estado}, {@code prioridad} — the case's own status and priority
 *   <li>{@code paid}, {@code partiallyPaid}, {@code partialPaymentAllowed} — payment guards, which
 *       the billing phase fills in; absent means "not paid", which is the safe reading
 * </ul>
 *
 * <p>Form values arrive as strings; numeric-looking ones are also offered as numbers so a rule can
 * write {@code area > 100} without quoting.
 */
final class ProcedureVariables {

    private ProcedureVariables() {}

    static Map<String, Object> of(Procedure procedure, ProcedureRepository procedures) {
        return of(procedure, procedures, Map.of());
    }

    /** {@code overrides} wins over stored state — used to evaluate a change before saving it. */
    static Map<String, Object> of(
            Procedure procedure, ProcedureRepository procedures, Map<String, Object> overrides) {
        Map<String, Object> vars = new HashMap<>();

        for (Map.Entry<String, String> e : procedures.findFormValues(procedure.id()).entrySet()) {
            vars.put(e.getKey(), coerce(e.getValue()));
        }
        for (ProcedureRequirementItem item : procedures.findRequirements(procedure.id())) {
            vars.put("requisito_" + item.requirementKey(), item.isSatisfied());
        }

        vars.put("estado", procedure.status().name());
        vars.put("prioridad", procedure.priority().name());
        vars.putAll(overrides);
        return Map.copyOf(vars);
    }

    /** A numeric-looking string also reads as a number; "true"/"false" read as booleans. */
    private static Object coerce(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.strip();
        if (v.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (v.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        try {
            return Double.valueOf(v);
        } catch (NumberFormatException notANumber) {
            return raw;
        }
    }
}
