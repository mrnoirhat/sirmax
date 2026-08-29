// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.procedure;

/**
 * The closed vocabulary of the case timeline (master prompt §16 "History", §40).
 *
 * <p>Kept separate from the audit trail: the timeline is what the operator reads on the case, the
 * audit log is the tamper-evident record of who changed what.
 */
public enum ProcedureEventKind {
    OPENED,
    REQUIREMENT_UPDATED,
    FORM_UPDATED,
    ASSIGNED,
    STEP_ADVANCED,
    NOTE,
    INVOICED,
    PAID,
    DOCUMENT_ISSUED,
    DECIDED,
    CLOSED,
    REOPENED,
    CANCELLED
}
