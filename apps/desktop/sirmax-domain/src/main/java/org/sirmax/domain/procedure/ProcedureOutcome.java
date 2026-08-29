// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.procedure;

/** How a {@link Procedure} ended (master prompt §28 — decisions and approvals). */
public enum ProcedureOutcome {
    APPROVED,
    REJECTED,
    CANCELLED,
    DELIVERED
}
