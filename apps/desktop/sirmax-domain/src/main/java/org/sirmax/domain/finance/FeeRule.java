// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.finance;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.shared.Money;

/**
 * An immutable, dated rule for calculating one charge (master prompt §19, docs/adr/0008).
 *
 * <p>Rules are never edited in place — a change is a new rule with a new effective range, so old
 * procedures keep their historical amounts. All money is integer minor units in {@code currencyCode};
 * never floating point.
 *
 * @param id identifier
 * @param type how the amount is derived
 * @param chargeType revenue taxonomy for the resulting line
 * @param concept operator-facing line label (administrator-authored data, not program text)
 * @param currencyCode ISO-4217 code, e.g. {@code "DOP"}
 * @param amountMinor fixed amount (FIXED) or per-period amount (PERIODIC)
 * @param unitRateMinor per-unit rate (QUANTITY_X_RATE)
 * @param ratePerAreaMinor per-m² rate (AREA_BASED)
 * @param ratePerDayMinor per-day rate (DURATION_BASED)
 * @param byKey key → amountMinor lookup (CATEGORY_BASED, LOCATION_BASED)
 * @param tiers progressive bands (TIERED), ascending by {@code upToQuantity}
 * @param effectiveFrom first date this rule applies
 * @param effectiveTo last date this rule applies (empty = open-ended)
 * @param legalReference optional ordinance / resolution reference
 */
public record FeeRule(
        String id,
        FeeRuleType type,
        ChargeType chargeType,
        String concept,
        String currencyCode,
        long amountMinor,
        long unitRateMinor,
        long ratePerAreaMinor,
        long ratePerDayMinor,
        Map<String, Long> byKey,
        List<FeeTier> tiers,
        LocalDate effectiveFrom,
        Optional<LocalDate> effectiveTo,
        Optional<String> legalReference) {

    public FeeRule {
        id = requireText(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(chargeType, "chargeType");
        concept = requireText(concept, "concept");
        currencyCode = requireCurrency(currencyCode);
        byKey = byKey == null ? Map.of() : Map.copyOf(byKey);
        tiers = tiers == null ? List.of() : List.copyOf(tiers);
        Objects.requireNonNull(effectiveFrom, "effectiveFrom");
        effectiveTo = effectiveTo == null ? Optional.empty() : effectiveTo;
        legalReference = normalize(legalReference);

        if (effectiveTo.isPresent() && effectiveTo.get().isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("effectiveTo must not be before effectiveFrom");
        }
        if (type == FeeRuleType.TIERED && tiers.isEmpty()) {
            throw new IllegalArgumentException("TIERED rule needs at least one tier");
        }
        if ((type == FeeRuleType.CATEGORY_BASED || type == FeeRuleType.LOCATION_BASED)
                && byKey.isEmpty()) {
            throw new IllegalArgumentException(type + " rule needs a non-empty byKey map");
        }
    }

    /** Whether this rule applies on {@code date}. */
    public boolean isEffectiveOn(LocalDate date) {
        boolean startedByThen = !date.isBefore(effectiveFrom);
        boolean notEndedYet = effectiveTo.map(end -> !date.isAfter(end)).orElse(true);
        return startedByThen && notEndedYet;
    }

    Money money(long minor) {
        return new Money(minor, currency());
    }

    java.util.Currency currency() {
        return java.util.Currency.getInstance(currencyCode);
    }

    // ── convenience factories for the common cases ──

    public static FeeRule fixed(
            String id, ChargeType chargeType, String concept, String currency,
            long amountMinor, LocalDate from) {
        return new FeeRule(
                id, FeeRuleType.FIXED, chargeType, concept, currency, amountMinor,
                0, 0, 0, Map.of(), List.of(), from, Optional.empty(), Optional.empty());
    }

    public static FeeRule perUnit(
            String id, ChargeType chargeType, String concept, String currency,
            long unitRateMinor, LocalDate from) {
        return new FeeRule(
                id, FeeRuleType.QUANTITY_X_RATE, chargeType, concept, currency, 0,
                unitRateMinor, 0, 0, Map.of(), List.of(), from, Optional.empty(), Optional.empty());
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String v = value.strip();
        if (v.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return v;
    }

    private static String requireCurrency(String code) {
        String c = requireText(code, "currencyCode").toUpperCase(java.util.Locale.ROOT);
        java.util.Currency.getInstance(c); // throws if invalid
        return c;
    }

    private static Optional<String> normalize(Optional<String> v) {
        if (v == null || v.isEmpty()) {
            return Optional.empty();
        }
        String s = v.get().strip();
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }
}
