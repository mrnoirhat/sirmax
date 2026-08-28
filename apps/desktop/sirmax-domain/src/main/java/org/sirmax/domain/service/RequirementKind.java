// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.service;

/** What satisfies a {@link RequirementDef} (master prompt §17). */
public enum RequirementKind {
    DOCUMENT,
    FIELD,
    IDENTITY_VERIFICATION,
    PAYMENT,
    INSPECTION,
    APPROVAL,
    SIGNATURE,
    EXTERNAL_REFERENCE,
    SUPPORTING_EVIDENCE
}
