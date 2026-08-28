// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.finance;

import java.util.List;
import java.util.Objects;
import org.sirmax.shared.Money;

/**
 * The output of the fee engine for one procedure: the lines to bill and their total (master prompt
 * §19). A {@code Charge} is the input to the billing module (Phase 6), which turns it into an
 * {@code Invoice}; a fee is not an invoice.
 */
public record Charge(String currencyCode, List<ChargeLine> lines) {

    public Charge {
        Objects.requireNonNull(currencyCode, "currencyCode");
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    public static Charge empty(String currencyCode) {
        return new Charge(currencyCode, List.of());
    }

    public Money total() {
        Money sum = Money.zero(currencyCode);
        for (ChargeLine line : lines) {
            sum = sum.plus(line.lineTotal());
        }
        return sum;
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public boolean isZero() {
        return total().isZero();
    }
}
