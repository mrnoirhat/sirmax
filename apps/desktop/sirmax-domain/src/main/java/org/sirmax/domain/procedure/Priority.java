// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.procedure;

/** Operator-set urgency used to order worklists (master prompt §57). */
public enum Priority {
    LOW,
    NORMAL,
    HIGH,
    URGENT;

    /** Descending weight so queues can sort URGENT first without a comparator table. */
    public int weight() {
        return switch (this) {
            case URGENT -> 3;
            case HIGH -> 2;
            case NORMAL -> 1;
            case LOW -> 0;
        };
    }
}
