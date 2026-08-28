// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.finance;

import java.util.ArrayList;
import java.util.List;
import org.sirmax.shared.Money;

/**
 * Turns a service version's {@link FeeRule}s and a {@link FeeInput} into a {@link Charge}
 * (master prompt §19, docs/adr/0008).
 *
 * <p>Only rules effective on {@link FeeInput#onDate()} are applied; each produces one line. All money
 * is integer minor units — never floating point. Manual overrides live at the use-case level and
 * require {@code fee.override} + a reason (not here).
 */
public final class FeeCalculator {

    private FeeCalculator() {}

    public static Charge calculate(List<FeeRule> rules, FeeInput input) {
        List<ChargeLine> lines = new ArrayList<>();
        String currency = null;

        for (FeeRule rule : rules) {
            if (!rule.isEffectiveOn(input.onDate())) {
                continue;
            }
            if (currency == null) {
                currency = rule.currencyCode();
            } else if (!currency.equals(rule.currencyCode())) {
                throw new IllegalArgumentException(
                        "Fee rules of one service must share a currency: "
                                + currency
                                + " vs "
                                + rule.currencyCode());
            }
            lines.add(lineFor(rule, input));
        }

        return new Charge(currency == null ? defaultCurrency(rules) : currency, lines);
    }

    private static ChargeLine lineFor(FeeRule rule, FeeInput input) {
        return switch (rule.type()) {
            case FIXED ->
                    ChargeLine.of(rule.concept(), rule.chargeType(), 1, rule.money(rule.amountMinor()));
            case QUANTITY_X_RATE ->
                    ChargeLine.of(
                            rule.concept(),
                            rule.chargeType(),
                            input.quantity(),
                            rule.money(rule.unitRateMinor()));
            case PERIODIC ->
                    ChargeLine.of(
                            rule.concept(),
                            rule.chargeType(),
                            input.quantity(),
                            rule.money(rule.amountMinor()));
            case AREA_BASED ->
                    ChargeLine.of(
                            rule.concept(),
                            rule.chargeType(),
                            require(input.areaSqM(), rule, "areaSqM"),
                            rule.money(rule.ratePerAreaMinor()));
            case DURATION_BASED ->
                    ChargeLine.of(
                            rule.concept(),
                            rule.chargeType(),
                            require(input.durationDays(), rule, "durationDays"),
                            rule.money(rule.ratePerDayMinor()));
            case CATEGORY_BASED ->
                    keyedLine(rule, requireKey(input.category(), rule, "category"));
            case LOCATION_BASED ->
                    keyedLine(rule, requireKey(input.location(), rule, "location"));
            case TIERED -> tieredLine(rule, input.quantity());
        };
    }

    private static ChargeLine keyedLine(FeeRule rule, String key) {
        Long amount = rule.byKey().get(key);
        if (amount == null) {
            throw new IllegalArgumentException(
                    "Fee rule " + rule.id() + " has no amount for key '" + key + "'");
        }
        return ChargeLine.of(rule.concept(), rule.chargeType(), 1, rule.money(amount));
    }

    private static ChargeLine tieredLine(FeeRule rule, long quantity) {
        long remaining = quantity;
        long previousBound = 0;
        long totalMinor = 0;
        for (FeeTier tier : rule.tiers()) {
            if (remaining <= 0) {
                break;
            }
            long bandSize = tier.upToQuantity() - previousBound;
            long inBand = Math.min(remaining, Math.max(bandSize, 0));
            totalMinor = Math.addExact(totalMinor, Math.multiplyExact(inBand, tier.unitRateMinor()));
            remaining -= inBand;
            previousBound = tier.upToQuantity();
        }
        // one line whose total is the progressive sum
        Money zero = Money.zero(rule.currency());
        return new ChargeLine(
                rule.concept(), rule.chargeType(), 1, rule.money(totalMinor), zero, zero);
    }

    private static long require(java.util.Optional<Long> value, FeeRule rule, String field) {
        return value.orElseThrow(
                () ->
                        new IllegalArgumentException(
                                rule.type() + " rule " + rule.id() + " needs input '" + field + "'"));
    }

    private static String requireKey(java.util.Optional<String> value, FeeRule rule, String field) {
        return value.filter(s -> !s.isBlank())
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        rule.type()
                                                + " rule "
                                                + rule.id()
                                                + " needs input '"
                                                + field
                                                + "'"));
    }

    private static String defaultCurrency(List<FeeRule> rules) {
        return rules.isEmpty() ? "DOP" : rules.get(0).currencyCode();
    }
}
