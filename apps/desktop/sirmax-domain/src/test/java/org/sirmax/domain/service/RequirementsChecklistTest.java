// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RequirementsChecklistTest {

    private static RequirementDef req(
            String key, boolean required, RequirementStage stage, String condition) {
        return new RequirementDef(
                key,
                key,
                RequirementKind.DOCUMENT,
                stage,
                required,
                Optional.ofNullable(condition),
                Optional.empty());
    }

    private static RequirementContext ctx(Set<String> satisfied, Map<String, Object> vars) {
        return new RequirementContext() {
            @Override
            public boolean isSatisfied(String key) {
                return satisfied.contains(key);
            }

            @Override
            public Map<String, Object> variables() {
                return vars;
            }
        };
    }

    @Test
    void countsPendingAndBlockingSeparately() {
        List<RequirementDef> defs =
                List.of(
                        req("cedula", true, RequirementStage.INTAKE, null),
                        req("planos", true, RequirementStage.REVIEW, null),
                        req("foto", false, RequirementStage.INTAKE, null));

        RequirementsChecklist cl =
                RequirementsChecklist.evaluate(defs, ctx(Set.of("cedula"), Map.of()));

        assertThat(cl.pendingCount()).isEqualTo(2); // planos + foto
        assertThat(cl.blockingCount()).isEqualTo(1); // planos (mandatory)
        assertThat(cl.isComplete()).isFalse();
        assertThat(cl.canReach(RequirementStage.INTAKE)).isTrue(); // nothing mandatory pending at intake
        assertThat(cl.canReach(RequirementStage.REVIEW)).isFalse(); // planos blocks review
        assertThat(cl.blockersFor(RequirementStage.DELIVERY)).hasSize(1);
    }

    @Test
    void conditionalRequirementOnlyAppliesWhenItsExpressionHolds() {
        List<RequirementDef> defs =
                List.of(
                        req("cedula", true, RequirementStage.INTAKE, null),
                        req("permiso_ambiental", true, RequirementStage.REVIEW, "tipo == 'INDUSTRIAL'"));

        RequirementsChecklist commercial =
                RequirementsChecklist.evaluate(
                        defs, ctx(Set.of("cedula"), Map.of("tipo", "COMERCIAL")));
        assertThat(commercial.isComplete()).isTrue();
        assertThat(commercial.items().get(1).applicable()).isFalse();

        RequirementsChecklist industrial =
                RequirementsChecklist.evaluate(
                        defs, ctx(Set.of("cedula"), Map.of("tipo", "INDUSTRIAL")));
        assertThat(industrial.blockingCount()).isEqualTo(1);
        assertThat(industrial.pendingItems().get(0).def().key()).isEqualTo("permiso_ambiental");
    }

    @Test
    void everythingSatisfiedIsComplete() {
        List<RequirementDef> defs =
                List.of(
                        req("cedula", true, RequirementStage.INTAKE, null),
                        req("pago", true, RequirementStage.APPROVAL, null));
        RequirementsChecklist cl =
                RequirementsChecklist.evaluate(defs, ctx(Set.of("cedula", "pago"), Map.of()));
        assertThat(cl.isComplete()).isTrue();
        assertThat(cl.canReach(RequirementStage.DELIVERY)).isTrue();
        assertThat(cl.pendingItems()).isEmpty();
    }
}
