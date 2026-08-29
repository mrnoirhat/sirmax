// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.procedure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sirmax.domain.service.RequirementDef;
import org.sirmax.domain.service.RequirementKind;
import org.sirmax.domain.service.RequirementStage;

class ProcedureChecklistTest {

    private static final Instant NOW = Instant.parse("2026-03-02T14:00:00Z");

    private static ProcedureRequirementItem item(
            String key, RequirementStage stage, boolean required, String condition) {
        return ProcedureRequirementItem.from(
                "req-" + key,
                "p-1",
                new RequirementDef(
                        key,
                        key,
                        RequirementKind.DOCUMENT,
                        stage,
                        required,
                        Optional.ofNullable(condition),
                        Optional.empty()));
    }

    @Test
    void aMandatoryGapBlocksItsStageAndEveryLaterOne() {
        var cedula = item("cedula", RequirementStage.INTAKE, true, null);
        var informe = item("informe", RequirementStage.REVIEW, true, null);
        informe.markSatisfied("user-1", null, NOW);

        var checklist = ProcedureChecklist.of(List.of(cedula, informe));

        assertThat(checklist.pendingCount()).isEqualTo(1);
        assertThat(checklist.canReach(RequirementStage.INTAKE)).isFalse();
        assertThat(checklist.canReach(RequirementStage.DELIVERY)).isFalse();
        assertThat(checklist.firstBlockedStage()).contains(RequirementStage.INTAKE);
    }

    @Test
    void anEarlierStageStaysReachableWhenOnlyALaterOneIsMissing() {
        var cedula = item("cedula", RequirementStage.INTAKE, true, null);
        cedula.markSatisfied("user-1", null, NOW);
        var inspeccion = item("inspeccion", RequirementStage.APPROVAL, true, null);

        var checklist = ProcedureChecklist.of(List.of(cedula, inspeccion));

        assertThat(checklist.canReach(RequirementStage.REVIEW)).isTrue();
        assertThat(checklist.canReach(RequirementStage.APPROVAL)).isFalse();
    }

    @Test
    void aConditionalRequirementOnlyCountsWhenItsConditionHolds() {
        var titulo =
                item("titulo", RequirementStage.INTAKE, true, "tipo_solicitante == 'propietario'");

        assertThat(ProcedureChecklist.of(List.of(titulo), Map.of("tipo_solicitante", "inquilino"))
                        .pendingCount())
                .isZero();
        assertThat(ProcedureChecklist.of(List.of(titulo), Map.of("tipo_solicitante", "propietario"))
                        .pendingCount())
                .isEqualTo(1);
    }

    @Test
    void aMalformedConditionKeepsTheRequirementRatherThanDroppingIt() {
        var broken = item("raro", RequirementStage.INTAKE, true, "&& ||");

        assertThat(ProcedureChecklist.of(List.of(broken), Map.of()).pendingCount()).isEqualTo(1);
    }

    @Test
    void optionalItemsAreListedButNeverBlock() {
        var opcional = item("foto", RequirementStage.INTAKE, false, null);

        var checklist = ProcedureChecklist.of(List.of(opcional));

        assertThat(checklist.isComplete()).isTrue();
        assertThat(checklist.pendingOptional()).hasSize(1);
    }

    @Test
    void aWaivedRequirementClearsTheBlockButStaysMarkedAsWaived() {
        var cedula = item("cedula", RequirementStage.INTAKE, true, null);
        cedula.waive("user-1", "Presentó acta de nacimiento", NOW);

        var checklist = ProcedureChecklist.of(List.of(cedula));

        assertThat(checklist.isComplete()).isTrue();
        assertThat(cedula.isWaived()).isTrue();
        assertThat(cedula.note()).contains("Presentó acta de nacimiento");
    }
}
