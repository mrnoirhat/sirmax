// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.shared.Money;

class AgreementTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final LocalDate START = LocalDate.of(2026, 2, 1);

    private static Money dop(String amount) {
        return Money.of(amount, "DOP");
    }

    private static Agreement stallAssignment() {
        Agreement agreement =
                Agreement.draft(
                        "ag-1",
                        "CONT-2026-000001",
                        AgreementKind.STALL_ASSIGNMENT,
                        "asset-1",
                        PartyRef.person("per-1"),
                        START,
                        dop("1500.00"),
                        Agreement.BillingFrequency.MONTHLY,
                        NOW);
        agreement.activate(NOW);
        return agreement;
    }

    @Test
    void oneModelCarriesEveryKindOfArrangement() {
        for (AgreementKind kind : AgreementKind.values()) {
            Agreement agreement =
                    Agreement.draft(
                            "ag-" + kind,
                            "CONT-" + kind,
                            kind,
                            "asset-1",
                            PartyRef.person("per-1"),
                            START,
                            dop("100.00"),
                            Agreement.BillingFrequency.ANNUAL,
                            NOW);
            agreement.activate(NOW);
            assertThat(agreement.isInForceOn(START)).isTrue();
        }
    }

    @Test
    void annualAmountFollowsTheBillingFrequency() {
        Agreement monthly = stallAssignment();
        assertThat(monthly.annualAmount()).isEqualTo(dop("18000.00"));

        monthly.setAmount(dop("6000.00"), Agreement.BillingFrequency.QUARTERLY, NOW);
        assertThat(monthly.annualAmount()).isEqualTo(dop("24000.00"));

        monthly.setAmount(dop("500.00"), Agreement.BillingFrequency.ONCE, NOW);
        assertThat(monthly.annualAmount().isZero()).isTrue();
        assertThat(monthly.periodAmount().isZero()).isTrue();
    }

    @Test
    void aTransferClosesTheOldContractAndChainsTheNewOne() {
        Agreement original = stallAssignment();

        Agreement successor =
                original.transferTo(
                        "ag-2",
                        "CONT-2026-000002",
                        PartyRef.person("per-2"),
                        LocalDate.of(2026, 6, 1),
                        NOW);

        assertThat(original.status()).isEqualTo(Agreement.Status.TRANSFERRED);
        assertThat(original.terminatedAt()).contains(NOW);
        assertThat(successor.status()).isEqualTo(Agreement.Status.ACTIVE);
        assertThat(successor.holder()).isEqualTo(PartyRef.person("per-2"));
        assertThat(successor.transferredFromId()).contains("ag-1");
        // the terms carry over
        assertThat(successor.amount()).isEqualTo(original.amount());
        assertThat(successor.kind()).isEqualTo(original.kind());
    }

    @Test
    void onlyAnActiveContractCanBeTransferredOrRenewed() {
        Agreement agreement = stallAssignment();
        agreement.terminate("Falta de pago", NOW);

        assertThatThrownBy(
                        () ->
                                agreement.transferTo(
                                        "ag-2", "CONT-2", PartyRef.person("per-2"), START, NOW))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> agreement.renew(LocalDate.of(2027, 1, 1), NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void terminationNeedsAReasonAndIsFinal() {
        Agreement agreement = stallAssignment();

        assertThatThrownBy(() -> agreement.terminate("   ", NOW))
                .isInstanceOf(IllegalArgumentException.class);

        agreement.terminate("Falta de pago reiterada", NOW);
        assertThat(agreement.status().isFinished()).isTrue();
        assertThatThrownBy(() -> agreement.terminate("otra vez", NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aRenewalMustActuallyExtendTheTerm() {
        Agreement agreement = stallAssignment();
        agreement.setTerm(START, LocalDate.of(2026, 12, 31), true, NOW);

        assertThatThrownBy(() -> agreement.renew(LocalDate.of(2026, 6, 30), NOW))
                .isInstanceOf(IllegalArgumentException.class);

        agreement.renew(LocalDate.of(2027, 12, 31), NOW);
        assertThat(agreement.endDate()).contains(LocalDate.of(2027, 12, 31));
    }

    @Test
    void aNonRenewableContractRefusesRenewal() {
        Agreement agreement = stallAssignment();
        agreement.setTerm(START, LocalDate.of(2026, 12, 31), false, NOW);

        assertThatThrownBy(() -> agreement.renew(LocalDate.of(2027, 12, 31), NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not renewable");
    }

    @Test
    void anIndefiniteConcessionNeverLapses() {
        Agreement concession =
                Agreement.draft(
                        "ag-9",
                        "CONT-9",
                        AgreementKind.CONCESSION,
                        "nicho-1",
                        PartyRef.person("per-1"),
                        START,
                        dop("0.00"),
                        Agreement.BillingFrequency.NONE,
                        NOW);
        concession.activate(NOW);

        assertThat(concession.hasLapsedBy(LocalDate.of(2099, 1, 1))).isFalse();
        assertThat(concession.isInForceOn(LocalDate.of(2099, 1, 1))).isTrue();
    }
}
