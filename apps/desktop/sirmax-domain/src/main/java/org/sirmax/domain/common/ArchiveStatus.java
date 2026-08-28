// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.common;

/**
 * Lifecycle/archival status carried by many aggregates (master prompt §31).
 *
 * <p>SIRMAX never equates "delete" with archival: records with legal or financial value are
 * deactivated/archived, never physically removed.
 */
public enum ArchiveStatus {
    ACTIVE,
    COMPLETED,
    CLOSED,
    ARCHIVED,
    VOID,
    CANCELLED
}
