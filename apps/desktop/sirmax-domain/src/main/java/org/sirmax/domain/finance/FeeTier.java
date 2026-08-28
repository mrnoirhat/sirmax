// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.finance;

/**
 * One band of a {@link FeeRuleType#TIERED} rule: units at or below {@code upToQuantity} are charged
 * at {@code unitRateMinor}. The last tier of a rule should use {@link Long#MAX_VALUE} for "and above".
 *
 * @param upToQuantity inclusive upper bound of this band, in whole units
 * @param unitRateMinor price per unit within this band, in the currency's minor unit
 */
public record FeeTier(long upToQuantity, long unitRateMinor) {

    public FeeTier {
        if (upToQuantity <= 0) {
            throw new IllegalArgumentException("upToQuantity must be > 0");
        }
        if (unitRateMinor < 0) {
            throw new IllegalArgumentException("unitRateMinor must be >= 0");
        }
    }
}
