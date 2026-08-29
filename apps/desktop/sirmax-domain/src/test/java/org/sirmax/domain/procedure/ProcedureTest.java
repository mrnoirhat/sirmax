// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.procedure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.sirmax.domain.common.PartyRef;

class ProcedureTest {

    private static final Instant NOW = Instant.parse("2026-03-02T14:00:00Z");

    private static Procedure open() {
        return Procedure.open(
                "p-1",
                "TRM-2026-000001",
                "svc-1",
                "ver-1",
                PartyRef.person("per-1"),
                "recepcion",
                LocalDate.of(2026, 3, 10),
                NOW);
    }

    @Test
    void aNewCaseStartsOpenAtTheFirstStep() {
        Procedure p = open();

        assertThat(p.status()).isEqualTo(ProcedureStatus.OPEN);
        assertThat(p.priority()).isEqualTo(Priority.NORMAL);
        assertThat(p.currentStepKey()).contains("recepcion");
        assertThat(p.outcome()).isEmpty();
        assertThat(p.closedAt()).isEmpty();
    }

    @Test
    void assigningToAnOperatorPutsTheCaseInProgress() {
        Procedure p = open();

        p.assign("dep-1", "user-9", NOW);

        assertThat(p.status()).isEqualTo(ProcedureStatus.IN_PROGRESS);
        assertThat(p.assignedUserId()).contains("user-9");
        assertThat(p.departmentId()).contains("dep-1");
    }

    @Test
    void blockingAndResumingTracksWhoTheCaseIsWaitingOn() {
        Procedure p = open();

        p.blockOnRequirements(NOW);
        assertThat(p.status().isBlocked()).isTrue();

        p.resume(NOW);
        assertThat(p.status()).isEqualTo(ProcedureStatus.IN_PROGRESS);
    }

    @Test
    void rejectingClosesTheCaseAndRequiresAReason() {
        Procedure p = open();

        assertThatThrownBy(() -> p.decide(ProcedureOutcome.REJECTED, "  ", NOW))
                .isInstanceOf(IllegalArgumentException.class);

        p.decide(ProcedureOutcome.REJECTED, "Falta el plano sellado", NOW);

        assertThat(p.status()).isEqualTo(ProcedureStatus.REJECTED);
        assertThat(p.status().isTerminal()).isTrue();
        assertThat(p.closedAt()).contains(NOW);
        assertThat(p.currentStepKey()).isEmpty();
        assertThat(p.outcomeReason()).contains("Falta el plano sellado");
    }

    @Test
    void approvalLeavesTheCaseOpenForDelivery() {
        Procedure p = open();

        p.decide(ProcedureOutcome.APPROVED, null, NOW);

        assertThat(p.status()).isEqualTo(ProcedureStatus.APPROVED);
        assertThat(p.status().isTerminal()).isFalse();
        assertThat(p.closedAt()).isEmpty();
    }

    @Test
    void aTerminalCaseRefusesFurtherWorkUntilItIsReopened() {
        Procedure p = open();
        p.close(NOW);

        assertThatThrownBy(() -> p.assign("dep-2", "user-3", NOW))
                .isInstanceOf(IllegalStateException.class);

        p.reopen("revision", NOW);

        assertThat(p.status()).isEqualTo(ProcedureStatus.IN_PROGRESS);
        assertThat(p.currentStepKey()).contains("revision");
        assertThat(p.outcome()).isEmpty();
    }

    @Test
    void overdueOnlyCountsWhileTheCaseIsStillOpen() {
        Procedure p = open();

        assertThat(p.isOverdue(LocalDate.of(2026, 3, 9))).isFalse();
        assertThat(p.isOverdue(LocalDate.of(2026, 3, 11))).isTrue();

        p.close(NOW);
        assertThat(p.isOverdue(LocalDate.of(2026, 3, 11))).isFalse();
    }
}
