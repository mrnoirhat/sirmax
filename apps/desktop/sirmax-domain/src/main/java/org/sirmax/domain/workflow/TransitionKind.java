// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.workflow;

/**
 * The fixed set of workflow transitions (docs/adr/0007). There is no arbitrary scripting — a step
 * only offers transitions of these kinds.
 */
public enum TransitionKind {
    /** Move forward to the next step. */
    ADVANCE,
    /** Approve at an APPROVAL/REVIEW step. */
    APPROVE,
    /** Reject (usually terminal). */
    REJECT,
    /** Send back to an earlier step for the applicant/operator to fix something. */
    RETURN_FOR_CORRECTION,
    /** Reassign the current step (stays on the same step; department/user changes). */
    REASSIGN,
    /** Cancel the whole procedure. */
    CANCEL
}
