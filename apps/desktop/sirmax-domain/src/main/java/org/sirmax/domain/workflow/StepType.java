// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.workflow;

/** The fixed vocabulary of workflow step kinds (docs/adr/0007). */
public enum StepType {
    /** Ordinary work assigned to a role. */
    TASK,
    /** A review that can pass, reject or return for correction. */
    REVIEW,
    /** A formal approval decision. */
    APPROVAL,
    /** A field inspection. */
    INSPECTION,
    /** Blocks advancing until the associated invoice is paid. */
    PAYMENT_CHECKPOINT,
    /** Generates an official document on entry (idempotent). */
    DOCUMENT_OUTPUT
}
