// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * An exact monetary amount: an integer number of minor units (e.g. cents) plus an ISO-4217 currency.
 *
 * <p>SIRMAX never uses binary floating point for money (master prompt §2.3, §59A.1). All financial
 * arithmetic — subtotal, discount, surcharge, total, balance, change — flows through this type.
 *
 * <p>Persisted as {@code *_minor INTEGER} + {@code currency TEXT(3)} (see {@code DATABASE.md}).
 *
 * @param minorUnits amount in the currency's minor unit; may be negative (refunds, adjustments)
 * @param currency ISO-4217 currency
 */
public record Money(long minorUnits, Currency currency) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(currency, "currency");
    }

    /** Zero in the given currency. */
    public static Money zero(Currency currency) {
        return new Money(0L, currency);
    }

    /** Zero in the given ISO-4217 code (e.g. {@code "DOP"}, {@code "USD"}). */
    public static Money zero(String currencyCode) {
        return zero(Currency.getInstance(currencyCode));
    }

    /** From a whole/decimal amount (e.g. {@code 1250.00}) using the currency's default fraction digits. */
    public static Money of(BigDecimal amount, Currency currency) {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        int digits = Math.max(currency.getDefaultFractionDigits(), 0);
        BigDecimal scaled = amount.movePointRight(digits).setScale(0, RoundingMode.HALF_UP);
        return new Money(scaled.longValueExact(), currency);
    }

    public static Money of(String amount, String currencyCode) {
        return of(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    /** Amount as a decimal in major units (e.g. {@code 1250.00}). For display/PDF, not for math. */
    public BigDecimal toDecimal() {
        int digits = Math.max(currency.getDefaultFractionDigits(), 0);
        return BigDecimal.valueOf(minorUnits, digits);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(minorUnits, other.minorUnits), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.subtractExact(minorUnits, other.minorUnits), currency);
    }

    /** Multiply by an integer quantity (e.g. line quantity). */
    public Money times(long quantity) {
        return new Money(Math.multiplyExact(minorUnits, quantity), currency);
    }

    /**
     * Multiply by a decimal factor (e.g. a rate or percentage), rounding half-up to the minor unit.
     * Rounding policy is explicit and documented (see {@code DATABASE.md} §4).
     */
    public Money times(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor");
        BigDecimal result =
                BigDecimal.valueOf(minorUnits).multiply(factor).setScale(0, RoundingMode.HALF_UP);
        return new Money(result.longValueExact(), currency);
    }

    public boolean isZero() {
        return minorUnits == 0L;
    }

    public boolean isNegative() {
        return minorUnits < 0L;
    }

    public boolean isPositive() {
        return minorUnits > 0L;
    }

    public Money negated() {
        return new Money(Math.negateExact(minorUnits), currency);
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return Long.compare(minorUnits, other.minorUnits);
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: " + currency.getCurrencyCode() + " vs "
                            + other.currency.getCurrencyCode());
        }
    }

    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + toDecimal().toPlainString();
    }
}
