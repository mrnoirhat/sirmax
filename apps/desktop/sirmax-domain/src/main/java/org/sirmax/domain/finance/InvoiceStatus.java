// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.finance;

/**
 * Invoice lifecycle (master prompt §59A.2).
 *
 * <p>Once an invoice leaves {@link #DRAFT} it must not be silently edited in a way that changes its
 * financial history; corrections use controlled void / refund / adjustment mechanisms.
 */
public enum InvoiceStatus {
    DRAFT,
    ISSUED,
    PARTIALLY_PAID,
    PAID,
    VOIDED,
    REFUNDED;

    public boolean isEditable() {
        return this == DRAFT;
    }

    public boolean isFinanciallyFrozen() {
        return this != DRAFT;
    }
}
