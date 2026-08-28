// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.service;

/**
 * Lifecycle of a {@link ServiceDefinitionVersion} (docs/adr/0006).
 *
 * <pre>
 *   DRAFT ──publish──▶ ACTIVE ──deactivate──▶ INACTIVE ──reactivate──▶ ACTIVE
 *     │                  │                       │
 *     └──────────────────┴───────────────────────┴──▶ ARCHIVED  (terminal)
 * </pre>
 *
 * A version's configuration is editable only while {@link #DRAFT}; once published it is immutable and
 * procedures keep referencing the exact version they were opened with.
 */
public enum ServiceStatus {
    DRAFT,
    ACTIVE,
    INACTIVE,
    ARCHIVED;

    public boolean isEditable() {
        return this == DRAFT;
    }

    public boolean isUsable() {
        return this == ACTIVE;
    }

    public boolean isTerminal() {
        return this == ARCHIVED;
    }
}
