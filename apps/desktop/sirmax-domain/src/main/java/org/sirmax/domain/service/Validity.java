// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.service;

import java.util.OptionalInt;

/**
 * How long the output of this service stays valid, and whether it can be renewed (master prompt §5,
 * urban-planning / permit modules).
 *
 * @param validForDays validity of the issued document/permit; empty means "does not expire"
 * @param renewable whether a renewal procedure is offered
 */
public record Validity(OptionalInt validForDays, boolean renewable) {

    public Validity {
        if (validForDays == null) {
            validForDays = OptionalInt.empty();
        }
        if (validForDays.isPresent() && validForDays.getAsInt() <= 0) {
            throw new IllegalArgumentException("validForDays must be > 0 when present");
        }
    }

    public static Validity permanent() {
        return new Validity(OptionalInt.empty(), false);
    }

    public static Validity ofDays(int days, boolean renewable) {
        return new Validity(OptionalInt.of(days), renewable);
    }

    public boolean expires() {
        return validForDays.isPresent();
    }
}
