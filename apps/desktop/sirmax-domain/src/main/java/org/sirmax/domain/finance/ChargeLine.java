// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.finance;

import java.util.Objects;
import org.sirmax.shared.Money;

/**
 * One line of a {@link Charge}: {@code unitPrice × quantity − discount + surcharge}.
 *
 * <p>All money is in the same currency. Discount/surcharge default to zero via the factory.
 */
public record ChargeLine(
        String concept,
        ChargeType chargeType,
        long quantity,
        Money unitPrice,
        Money discount,
        Money surcharge) {

    public ChargeLine {
        Objects.requireNonNull(concept, "concept");
        Objects.requireNonNull(chargeType, "chargeType");
        Objects.requireNonNull(unitPrice, "unitPrice");
        Objects.requireNonNull(discount, "discount");
        Objects.requireNonNull(surcharge, "surcharge");
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be >= 0");
        }
        if (!unitPrice.currency().equals(discount.currency())
                || !unitPrice.currency().equals(surcharge.currency())) {
            throw new IllegalArgumentException("ChargeLine money must share one currency");
        }
    }

    public static ChargeLine of(String concept, ChargeType type, long quantity, Money unitPrice) {
        Money zero = Money.zero(unitPrice.currency());
        return new ChargeLine(concept, type, quantity, unitPrice, zero, zero);
    }

    public Money lineTotal() {
        return unitPrice.times(quantity).minus(discount).plus(surcharge);
    }
}
