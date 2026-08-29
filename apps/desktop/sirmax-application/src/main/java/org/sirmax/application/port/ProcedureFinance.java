// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

/**
 * Tells the workflow engine whether a case's money is settled, so a {@code PAYMENT_CHECKPOINT} step
 * can hold the case until it is (master prompt §18, §20).
 *
 * <p>This is a read-only view deliberately narrower than the billing repository: the procedure side
 * needs three booleans, not the invoice model. Billing owns the implementation.
 */
public interface ProcedureFinance {

    /**
     * @param invoiced an invoice has been issued for the case
     * @param paid the balance is settled
     * @param partiallyPaid something has been collected but a balance remains
     */
    record PaymentState(boolean invoiced, boolean paid, boolean partiallyPaid) {

        public static PaymentState notInvoiced() {
            return new PaymentState(false, false, false);
        }
    }

    PaymentState stateOf(String procedureId);

    /**
     * The state of the world before billing exists: nothing is invoiced, so a payment checkpoint
     * correctly refuses to advance. Used by the desktop client only until the billing adapter is
     * wired in, and by unit tests of non-financial flows.
     */
    static ProcedureFinance unbilled() {
        return procedureId -> PaymentState.notInvoiced();
    }
}
