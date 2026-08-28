// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.service;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Service-level target for how long a procedure of this service should take (master prompt §30).
 *
 * @param targetDays target turnaround; {@code 0} means "no SLA"
 * @param basis whether {@code targetDays} counts business or calendar days
 * @param escalationThresholdDays optional threshold at which the case escalates
 */
public record Sla(int targetDays, Basis basis, OptionalInt escalationThresholdDays) {

    public enum Basis {
        BUSINESS_DAYS,
        CALENDAR_DAYS
    }

    public Sla {
        if (targetDays < 0) {
            throw new IllegalArgumentException("targetDays must be >= 0");
        }
        if (basis == null) {
            basis = Basis.BUSINESS_DAYS;
        }
        if (escalationThresholdDays == null) {
            escalationThresholdDays = OptionalInt.empty();
        }
        if (escalationThresholdDays.isPresent() && escalationThresholdDays.getAsInt() < 0) {
            throw new IllegalArgumentException("escalationThresholdDays must be >= 0");
        }
    }

    public static Sla none() {
        return new Sla(0, Basis.BUSINESS_DAYS, OptionalInt.empty());
    }

    public static Sla businessDays(int days) {
        return new Sla(days, Basis.BUSINESS_DAYS, OptionalInt.empty());
    }

    public boolean isDefined() {
        return targetDays > 0;
    }

    public Optional<Integer> escalationThreshold() {
        return escalationThresholdDays.isPresent()
                ? Optional.of(escalationThresholdDays.getAsInt())
                : Optional.empty();
    }
}
