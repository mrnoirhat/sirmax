// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.procedure;

/**
 * Where a {@link Procedure} stands (master prompt §16).
 *
 * <p>The workflow configured on the service version drives the *step*; this enum is the coarse
 * status the operator, the queues and the dashboard read. Terminal states never move again except
 * through an explicit reopen.
 */
public enum ProcedureStatus {
    /** Started by the wizard but not yet submitted. */
    DRAFT,
    OPEN,
    IN_PROGRESS,
    /** Blocked: a mandatory requirement for the current stage is missing (§56). */
    WAITING_REQUIREMENTS,
    /** Blocked: the workflow reached a payment checkpoint and the invoice is not settled. */
    WAITING_PAYMENT,
    APPROVED,
    REJECTED,
    DELIVERED,
    CLOSED,
    CANCELLED;

    /** {@code true} when no further work is expected without an explicit reopen. */
    public boolean isTerminal() {
        return this == REJECTED || this == DELIVERED || this == CLOSED || this == CANCELLED;
    }

    /** {@code true} when the case is waiting on the citizen or on money, not on the operator. */
    public boolean isBlocked() {
        return this == WAITING_REQUIREMENTS || this == WAITING_PAYMENT;
    }

    /** {@code true} when the case still belongs in an operator's worklist (§57). */
    public boolean isOpenWork() {
        return !isTerminal();
    }
}
