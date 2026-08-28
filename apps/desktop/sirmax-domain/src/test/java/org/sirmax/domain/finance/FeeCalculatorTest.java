// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sirmax.shared.Money;

class FeeCalculatorTest {

    private static final LocalDate D = LocalDate.parse("2026-05-01");

    @Test
    void fixedRuleProducesOneLine() {
        FeeRule r = FeeRule.fixed("r1", ChargeType.TASA, "Certificación de residencia", "DOP", 50_000, D);
        Charge charge = FeeCalculator.calculate(List.of(r), FeeInput.onDate(D));

        assertThat(charge.lines()).hasSize(1);
        assertThat(charge.total()).isEqualTo(Money.of("500.00", "DOP"));
        assertThat(charge.currencyCode()).isEqualTo("DOP");
    }

    @Test
    void quantityRuleMultipliesByInputQuantity() {
        FeeRule r = FeeRule.perUnit("r1", ChargeType.TASA, "Copia certificada", "DOP", 10_000, D);
        Charge charge = FeeCalculator.calculate(List.of(r), FeeInput.quantity(D, 3));

        assertThat(charge.total()).isEqualTo(Money.of("300.00", "DOP"));
        assertThat(charge.lines().get(0).quantity()).isEqualTo(3);
    }

    @Test
    void severalRulesSumIntoOneCharge() {
        FeeRule base = FeeRule.fixed("r1", ChargeType.TASA, "Trámite", "DOP", 50_000, D);
        FeeRule stamp = FeeRule.fixed("r2", ChargeType.ARBITRIO, "Sello", "DOP", 2_500, D);
        Charge charge = FeeCalculator.calculate(List.of(base, stamp), FeeInput.onDate(D));

        assertThat(charge.lines()).hasSize(2);
        assertThat(charge.total()).isEqualTo(Money.of("525.00", "DOP"));
    }

    @Test
    void onlyRulesEffectiveOnTheDateApply() {
        FeeRule old =
                new FeeRule(
                        "r1", FeeRuleType.FIXED, ChargeType.TASA, "Vieja", "DOP", 40_000, 0, 0, 0,
                        Map.of(), List.of(), LocalDate.parse("2025-01-01"),
                        Optional.of(LocalDate.parse("2025-12-31")), Optional.empty());
        FeeRule current = FeeRule.fixed("r2", ChargeType.TASA, "Vigente", "DOP", 50_000, D);

        Charge charge = FeeCalculator.calculate(List.of(old, current), FeeInput.onDate(D));
        assertThat(charge.lines()).hasSize(1);
        assertThat(charge.lines().get(0).concept()).isEqualTo("Vigente");
    }

    @Test
    void areaBasedNeedsAreaInput() {
        FeeRule r =
                new FeeRule(
                        "r1", FeeRuleType.AREA_BASED, ChargeType.TASA, "Por m²", "DOP", 0, 0, 1_500, 0,
                        Map.of(), List.of(), D, Optional.empty(), Optional.empty());

        Charge charge =
                FeeCalculator.calculate(
                        List.of(r),
                        new FeeInput(D, 1, Optional.of(120L), Optional.empty(), Optional.empty(), Optional.empty()));
        assertThat(charge.total()).isEqualTo(Money.of("1800.00", "DOP")); // 120 * 15.00

        assertThatThrownBy(() -> FeeCalculator.calculate(List.of(r), FeeInput.onDate(D)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("areaSqM");
    }

    @Test
    void categoryBasedLooksUpByKey() {
        FeeRule r =
                new FeeRule(
                        "r1", FeeRuleType.CATEGORY_BASED, ChargeType.ARBITRIO, "Letrero", "DOP", 0, 0, 0, 0,
                        Map.of("PEQUENO", 100_000L, "GRANDE", 500_000L), List.of(), D,
                        Optional.empty(), Optional.empty());

        Charge charge =
                FeeCalculator.calculate(
                        List.of(r),
                        new FeeInput(D, 1, Optional.empty(), Optional.empty(), Optional.of("GRANDE"), Optional.empty()));
        assertThat(charge.total()).isEqualTo(Money.of("5000.00", "DOP"));
    }

    @Test
    void tieredRuleChargesProgressively() {
        FeeRule r =
                new FeeRule(
                        "r1", FeeRuleType.TIERED, ChargeType.TASA, "Por tramos", "DOP", 0, 0, 0, 0,
                        Map.of(),
                        List.of(new FeeTier(10, 1_000), new FeeTier(50, 500), new FeeTier(Long.MAX_VALUE, 200)),
                        D, Optional.empty(), Optional.empty());

        // 30 units: 10*10.00 + 20*5.00 = 100.00 + 100.00 = 200.00
        Charge charge = FeeCalculator.calculate(List.of(r), FeeInput.quantity(D, 30));
        assertThat(charge.total()).isEqualTo(Money.of("200.00", "DOP"));
    }

    @Test
    void mixedCurrenciesAreRejected() {
        FeeRule dop = FeeRule.fixed("r1", ChargeType.TASA, "A", "DOP", 1000, D);
        FeeRule usd = FeeRule.fixed("r2", ChargeType.TASA, "B", "USD", 1000, D);
        assertThatThrownBy(() -> FeeCalculator.calculate(List.of(dop, usd), FeeInput.onDate(D)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");
    }
}
