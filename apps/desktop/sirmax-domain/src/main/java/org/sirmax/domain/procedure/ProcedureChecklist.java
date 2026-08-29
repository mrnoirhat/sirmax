// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.procedure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.domain.rules.ExpressionEvaluator;
import org.sirmax.domain.rules.ExpressionException;
import org.sirmax.domain.service.RequirementStage;

/**
 * Reads a procedure's materialized checklist the way the counter UI needs it (master prompt §56):
 * how many are missing, which stages are still reachable, and what exactly blocks the next one.
 *
 * <p>A conditional requirement only counts once its expression holds against the case's current form
 * answers — "copia del título" applies only when {@code tipo_solicitante == 'propietario'}. A
 * condition that cannot be evaluated is treated as applicable: it is safer to ask the citizen for a
 * document they may not need than to skip one they do.
 *
 * <p>Stage semantics mirror {@link org.sirmax.domain.service.RequirementsChecklist}: a mandatory gap
 * at a stage blocks that stage *and every later one*, because a case cannot skip forward past an
 * unmet condition. Optional items never block; they are shown so the operator can still chase them.
 */
public final class ProcedureChecklist {

    private final List<ProcedureRequirementItem> items;
    private final Map<String, Object> variables;

    private ProcedureChecklist(List<ProcedureRequirementItem> items, Map<String, Object> variables) {
        this.items = List.copyOf(Objects.requireNonNull(items, "items"));
        this.variables = Map.copyOf(Objects.requireNonNull(variables, "variables"));
    }

    /** A checklist with no form context: conditional requirements all count as applicable. */
    public static ProcedureChecklist of(List<ProcedureRequirementItem> items) {
        return new ProcedureChecklist(items, Map.of());
    }

    /** A checklist evaluated against the case's form answers. */
    public static ProcedureChecklist of(
            List<ProcedureRequirementItem> items, Map<String, Object> formVariables) {
        return new ProcedureChecklist(items, formVariables);
    }

    /** Every materialized line, applicable or not. */
    public List<ProcedureRequirementItem> items() {
        return items;
    }

    /** The lines that actually apply to this case given its current answers. */
    public List<ProcedureRequirementItem> applicableItems() {
        return items.stream().filter(this::applies).toList();
    }

    /** {@code true} when the item's condition holds (or it has none). */
    public boolean applies(ProcedureRequirementItem item) {
        Optional<String> condition = item.conditionExpression();
        if (condition.isEmpty()) {
            return true;
        }
        try {
            return ExpressionEvaluator.evaluate(condition.get(), variables);
        } catch (ExpressionException e) {
            // A malformed condition must not silently drop a requirement.
            return true;
        }
    }

    /** Applicable mandatory items neither provided nor waived, in declaration order. */
    public List<ProcedureRequirementItem> pending() {
        return items.stream().filter(i -> applies(i) && i.isPending()).toList();
    }

    /** How many mandatory requirements are still missing — the "Faltan N requisitos" count. */
    public int pendingCount() {
        return pending().size();
    }

    /** Applicable optional items not yet provided; shown, never blocking. */
    public List<ProcedureRequirementItem> pendingOptional() {
        return items.stream()
                .filter(i -> applies(i) && !i.required() && !i.isSatisfied())
                .toList();
    }

    public boolean isComplete() {
        return pending().isEmpty();
    }

    /** {@code true} when nothing mandatory is missing at {@code stage} or any earlier stage. */
    public boolean canReach(RequirementStage stage) {
        return blockersFor(stage).isEmpty();
    }

    /** The mandatory gaps that stop the case reaching {@code stage}. */
    public List<ProcedureRequirementItem> blockersFor(RequirementStage stage) {
        Objects.requireNonNull(stage, "stage");
        List<ProcedureRequirementItem> blockers = new ArrayList<>();
        for (ProcedureRequirementItem item : items) {
            if (applies(item) && item.isPending() && item.stage().ordinal() <= stage.ordinal()) {
                blockers.add(item);
            }
        }
        return List.copyOf(blockers);
    }

    /** The earliest stage that is still blocked, or empty when the checklist is complete. */
    public Optional<RequirementStage> firstBlockedStage() {
        return pending().stream()
                .map(ProcedureRequirementItem::stage)
                .min(Comparator.comparingInt(Enum::ordinal));
    }
}
