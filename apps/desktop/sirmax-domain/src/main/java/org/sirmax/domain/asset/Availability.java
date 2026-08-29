// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.asset;

/**
 * Whether an asset can be granted right now (master prompt §6 — "visual availability status for
 * cemetery spaces", and the same question for market stalls).
 */
public enum Availability {
    AVAILABLE,
    OCCUPIED,
    /** Held for a specific applicant, pending their paperwork or payment. */
    RESERVED,
    /** Out of service: under repair, in dispute, or withdrawn from offer. */
    UNAVAILABLE;

    public boolean canBeGranted() {
        return this == AVAILABLE;
    }
}
