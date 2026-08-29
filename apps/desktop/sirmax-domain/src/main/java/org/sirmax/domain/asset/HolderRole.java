// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.asset;

/**
 * How a party relates to an asset (master prompt §23, §25, §26).
 *
 * <p>Ownership and possession are different facts and both need recording: a municipal parcel is
 * owned by the municipality and held by a lessee, and a cemetery plot's concessionaire is not its
 * occupant.
 */
public enum HolderRole {
    OWNER,
    CO_OWNER,
    LESSEE,
    CONCESSIONAIRE,
    /** Physically present without a title — recorded for appropriation cases (§25). */
    OCCUPANT,
    REPRESENTATIVE,
    HEIR;

    /** {@code true} for roles that come from a contract rather than from a title. */
    public boolean derivesFromAgreement() {
        return this == LESSEE || this == CONCESSIONAIRE;
    }
}
