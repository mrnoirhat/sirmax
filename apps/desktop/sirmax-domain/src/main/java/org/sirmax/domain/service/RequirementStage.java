// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.service;

/** The workflow stage by which a {@link RequirementDef} must be satisfied (master prompt §17). */
public enum RequirementStage {
    INTAKE,
    REVIEW,
    APPROVAL,
    DECISION,
    DELIVERY
}
