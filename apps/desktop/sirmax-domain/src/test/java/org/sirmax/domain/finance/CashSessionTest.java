// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.sirmax.shared.Money;

class CashSessionTest {

    private static final Instant NOW = Instant.parse("2026-05-04T08:00:00Z");

    private static Money dop(String amount) {
        return Money.of(amount, "DOP");
    }

    private static CashSession open() {
        return CashSession.open("cs-1", "CAJA-2026-000001", "u-1", "dep-1", dop("2000.00"), NOW);
    }

    @Test
    void expectedCashIsTheFloatPlusCollectionsMinusRefunds() {
        CashSession session = open();

        assertThat(session.expectedCash(dop("15000.00"), dop("500.00")))
                .isEqualTo(dop("16500.00"));
    }

    @Test
    void aBalancedDrawerClosesAtZeroDifference() {
        CashSession session = open();
        Money expected = session.expectedCash(dop("15000.00"), Money.zero("DOP"));

        Money difference = session.close(dop("17000.00"), expected, null, NOW);

        assertThat(difference.isZero()).isTrue();
        assertThat(session.status()).isEqualTo(CashSession.Status.CLOSED);
        assertThat(session.countedTotal()).contains(dop("17000.00"));
    }

    @Test
    void aShortDrawerRecordsTheDifferenceRatherThanHidingIt() {
        CashSession session = open();
        Money expected = session.expectedCash(dop("15000.00"), Money.zero("DOP"));

        Money difference = session.close(dop("16800.00"), expected, "Faltan 200", NOW);

        assertThat(difference).isEqualTo(dop("-200.00"));
        assertThat(difference.isNegative()).isTrue();
        assertThat(session.notes()).contains("Faltan 200");
    }

    @Test
    void anOverDrawerAlsoShowsUp() {
        CashSession session = open();
        Money expected = session.expectedCash(dop("15000.00"), Money.zero("DOP"));

        assertThat(session.close(dop("17050.00"), expected, null, NOW)).isEqualTo(dop("50.00"));
    }

    @Test
    void aClosedSessionCannotBeClosedTwice() {
        CashSession session = open();
        session.close(dop("2000.00"), dop("2000.00"), null, NOW);

        assertThatThrownBy(() -> session.close(dop("2000.00"), dop("2000.00"), null, NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void countingInAnotherCurrencyIsRefused() {
        CashSession session = open();

        assertThatThrownBy(
                        () ->
                                session.close(
                                        Money.of("100.00", "USD"), dop("2000.00"), null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
