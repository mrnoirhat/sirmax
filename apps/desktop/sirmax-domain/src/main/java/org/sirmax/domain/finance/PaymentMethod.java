// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.finance;

/**
 * How money reached the municipality (master prompt §59A.5).
 *
 * <p>{@link #OTHER} is the extension point: a municipality that collects through a channel not
 * listed here records it as OTHER with a reference, rather than waiting for a code change. A
 * genuinely new named method is a schema change, because the cash drawer has to know about it.
 */
public enum PaymentMethod {
    CASH,
    BANK_TRANSFER,
    CARD,
    CHECK,
    OTHER;

    /** Only cash moves the physical drawer, so only cash counts toward the closing count. */
    public boolean affectsCashDrawer() {
        return this == CASH;
    }

    /** Non-cash methods should carry a transfer/cheque/authorization number. */
    public boolean expectsReference() {
        return this != CASH;
    }
}
