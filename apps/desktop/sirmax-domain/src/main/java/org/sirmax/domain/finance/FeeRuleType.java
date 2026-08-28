// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.finance;

/** How a {@link FeeRule} turns a {@link FeeInput} into an amount (master prompt §19, docs/adr/0008). */
public enum FeeRuleType {
    /** A fixed amount. */
    FIXED,
    /** {@code quantity × unitRate}. */
    QUANTITY_X_RATE,
    /** {@code area × ratePerArea}. */
    AREA_BASED,
    /** {@code durationDays × ratePerDay}. */
    DURATION_BASED,
    /** Amount looked up by the input's category. */
    CATEGORY_BASED,
    /** Amount looked up by the input's location. */
    LOCATION_BASED,
    /** Progressive tiers over a numeric quantity. */
    TIERED,
    /** {@code periods × amountPerPeriod} (monthly/periodic fees). */
    PERIODIC
}
