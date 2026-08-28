// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.finance;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * The values a procedure provides to the fee engine. Only the fields a rule needs matter; the rest
 * are ignored.
 *
 * @param onDate the date the charge is calculated for (selects the effective {@link FeeRule})
 * @param quantity whole-unit quantity for QUANTITY_X_RATE / TIERED / PERIODIC (default 1)
 * @param areaSqM area in m² for AREA_BASED
 * @param durationDays days for DURATION_BASED
 * @param category key for CATEGORY_BASED
 * @param location key for LOCATION_BASED
 */
public record FeeInput(
        LocalDate onDate,
        long quantity,
        Optional<Long> areaSqM,
        Optional<Long> durationDays,
        Optional<String> category,
        Optional<String> location) {

    public FeeInput {
        Objects.requireNonNull(onDate, "onDate");
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be >= 0");
        }
        areaSqM = areaSqM == null ? Optional.empty() : areaSqM;
        durationDays = durationDays == null ? Optional.empty() : durationDays;
        category = category == null ? Optional.empty() : category;
        location = location == null ? Optional.empty() : location;
    }

    public static FeeInput onDate(LocalDate date) {
        return new FeeInput(date, 1, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static FeeInput quantity(LocalDate date, long quantity) {
        return new FeeInput(
                date, quantity, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
}
