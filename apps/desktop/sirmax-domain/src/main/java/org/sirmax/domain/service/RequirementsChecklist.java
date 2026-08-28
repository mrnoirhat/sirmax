// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sirmax.domain.rules.ExpressionEvaluator;

/**
 * The visible checklist for a procedure (master prompt §17, §56): every declared requirement marked
 * applicable / satisfied / pending, so the operator never has to guess why a procedure cannot
 * advance ("Faltan 2 requisitos para continuar").
 *
 * <p>A requirement is <em>applicable</em> when it has no condition or its condition evaluates true.
 * A <em>mandatory</em>, applicable, unsatisfied requirement blocks its stage (and every later one);
 * optional requirements never block.
 */
public final class RequirementsChecklist {

    /**
     * @param def the declared requirement
     * @param applicable whether its condition holds for this procedure
     * @param satisfied whether it is currently met
     */
    public record Item(RequirementDef def, boolean applicable, boolean satisfied) {
        public boolean isPending() {
            return applicable && !satisfied;
        }

        public boolean blocks() {
            return isPending() && def.required();
        }
    }

    private final List<Item> items;

    private RequirementsChecklist(List<Item> items) {
        this.items = items;
    }

    public static RequirementsChecklist evaluate(
            List<RequirementDef> requirements, RequirementContext context) {
        List<Item> items = new ArrayList<>(requirements.size());
        for (RequirementDef def : requirements) {
            boolean applicable =
                    def.conditionExpression()
                            .map(expr -> ExpressionEvaluator.evaluate(expr, context.variables()))
                            .orElse(true);
            boolean satisfied = applicable && context.isSatisfied(def.key());
            items.add(new Item(def, applicable, satisfied));
        }
        return new RequirementsChecklist(items);
    }

    public List<Item> items() {
        return Collections.unmodifiableList(items);
    }

    public List<Item> pendingItems() {
        return items.stream().filter(Item::isPending).toList();
    }

    /** Count of applicable, unsatisfied requirements (mandatory or not). */
    public int pendingCount() {
        return (int) items.stream().filter(Item::isPending).count();
    }

    /** Count of applicable, unsatisfied <em>mandatory</em> requirements. */
    public int blockingCount() {
        return (int) items.stream().filter(Item::blocks).count();
    }

    public boolean isComplete() {
        return blockingCount() == 0;
    }

    /** Whether the procedure can move to {@code stage}: no mandatory gap at or before it. */
    public boolean canReach(RequirementStage stage) {
        return items.stream()
                .filter(Item::blocks)
                .noneMatch(i -> i.def().stage().ordinal() <= stage.ordinal());
    }

    /** Mandatory gaps that must be closed to reach {@code stage}. */
    public List<Item> blockersFor(RequirementStage stage) {
        return items.stream()
                .filter(Item::blocks)
                .filter(i -> i.def().stage().ordinal() <= stage.ordinal())
                .toList();
    }
}
