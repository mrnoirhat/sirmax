// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void ofDecimalConvertsToMinorUnits() {
        Money m = Money.of("1250.00", "DOP");
        assertThat(m.minorUnits()).isEqualTo(125_000L);
        assertThat(m.toDecimal()).isEqualByComparingTo(new BigDecimal("1250.00"));
        assertThat(m.toString()).isEqualTo("DOP 1250.00");
    }

    @Test
    void addAndSubtractStayExact() {
        Money a = Money.of("10.10", "DOP");
        Money b = Money.of("0.20", "DOP");
        assertThat(a.plus(b).minorUnits()).isEqualTo(1030L);
        assertThat(a.minus(b).minorUnits()).isEqualTo(990L);
    }

    @Test
    void timesFactorRoundsHalfUpToMinorUnit() {
        Money base = Money.of("100.00", "DOP"); // 10 000 minor
        assertThat(base.times(new BigDecimal("0.185")).minorUnits()).isEqualTo(1_850L);
        assertThat(base.times(new BigDecimal("0.12345")).minorUnits()).isEqualTo(1_235L);
    }

    @Test
    void currencyMismatchIsRejected() {
        Money dop = Money.of("1.00", "DOP");
        Money usd = Money.of("1.00", "USD");
        assertThatThrownBy(() -> dop.plus(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void negativeAmountsAreAllowedForRefunds() {
        Money refund = Money.of("50.00", "DOP").negated();
        assertThat(refund.isNegative()).isTrue();
        assertThat(refund.minorUnits()).isEqualTo(-5_000L);
    }
}
