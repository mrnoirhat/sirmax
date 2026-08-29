// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.numbering;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * An independent document numbering counter (master prompt §27, §59A.3).
 *
 * <p>Produces codes like {@code TRM-2026-000001} or {@code FACT-2026-000001}: prefix, optional
 * year, zero-padded counter. Prefix, padding and yearly reset are configurable per sequence, so a
 * municipality can match the numbering its archive already uses.
 *
 * <p>Allocation mutates the counter and is meant to run inside the same transaction as the row being
 * numbered — that is what makes it concurrency-safe and non-reusing. A number handed out is spent:
 * voiding the invoice that carries it never returns it to the pool.
 */
public final class NumberingSequence {

    private final String code;
    private String prefix;
    private int padding;
    private boolean yearlyReset;
    private int periodYear;
    private long nextValue;
    private Instant updatedAt;

    public NumberingSequence(
            String code,
            String prefix,
            int padding,
            boolean yearlyReset,
            int periodYear,
            long nextValue,
            Instant updatedAt) {
        this.code = requireText(code, "code");
        this.prefix = requireText(prefix, "prefix").toUpperCase(Locale.ROOT);
        this.padding = requirePadding(padding);
        this.yearlyReset = yearlyReset;
        this.periodYear = periodYear;
        if (nextValue < 1) {
            throw new IllegalArgumentException("nextValue must be >= 1");
        }
        this.nextValue = nextValue;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** A fresh sequence starting at 1, resetting every year. */
    public static NumberingSequence create(String code, String prefix, Instant now) {
        return new NumberingSequence(code, prefix, 6, true, 0, 1L, now);
    }

    /**
     * Hand out the next code for {@code year} and advance the counter.
     *
     * <p>With {@code yearlyReset}, entering a new year restarts the counter at 1; without it, the
     * counter runs continuously and the year in the code simply tracks the issue date.
     */
    public String allocate(int year, Instant now) {
        if (yearlyReset && year != periodYear) {
            periodYear = year;
            nextValue = 1L;
        } else if (periodYear == 0) {
            periodYear = year;
        }
        long value = nextValue;
        nextValue = Math.addExact(nextValue, 1L);
        updatedAt = Objects.requireNonNull(now, "now");
        return format(year, value);
    }

    /** The code {@code value} would produce, without consuming anything (for previews). */
    public String format(int year, long value) {
        String counter = String.format(Locale.ROOT, "%0" + padding + "d", value);
        return prefix + "-" + year + "-" + counter;
    }

    /** What {@link #allocate} would return next, without consuming it. */
    public String peek(int year) {
        long value = (yearlyReset && year != periodYear) ? 1L : nextValue;
        return format(year, value);
    }

    public String code() {
        return code;
    }

    public String prefix() {
        return prefix;
    }

    public int padding() {
        return padding;
    }

    public boolean yearlyReset() {
        return yearlyReset;
    }

    public int periodYear() {
        return periodYear;
    }

    public long nextValue() {
        return nextValue;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    /**
     * Reconfigure presentation. The counter itself is never moved backwards here — reusing a spent
     * number is exactly what §27 forbids.
     */
    public void reconfigure(String newPrefix, int newPadding, boolean newYearlyReset, Instant now) {
        this.prefix = requireText(newPrefix, "prefix").toUpperCase(Locale.ROOT);
        this.padding = requirePadding(newPadding);
        this.yearlyReset = newYearlyReset;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    private static int requirePadding(int padding) {
        if (padding < 1 || padding > 12) {
            throw new IllegalArgumentException("padding must be between 1 and 12");
        }
        return padding;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String v = value.strip();
        if (v.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return v;
    }
}
