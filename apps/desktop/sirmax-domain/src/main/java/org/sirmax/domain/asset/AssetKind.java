// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.asset;

/**
 * What kind of place a {@link MunicipalAsset} is (master prompt §25, §6, §7).
 *
 * <p>One enum instead of five tables, because a cemetery niche, a market stall and a municipal
 * parcel differ in what you *do* with them, not in what has to be recorded about them: a code, a
 * location, an area, who holds it, and what contracts touch it.
 */
public enum AssetKind {
    /** A land parcel — the cadastre's unit (§25). */
    PARCEL,
    BUILDING,
    CEMETERY,
    CEMETERY_SECTION,
    /** A grave, niche or ossuary space (§6). */
    CEMETERY_PLOT,
    MARKET,
    /** A stall or casilla inside a market (§7). */
    MARKET_STALL,
    KIOSK,
    /** A stretch of public space that can be permitted for temporary use (§9). */
    PUBLIC_SPACE,
    ROAD,
    OTHER;

    /** {@code true} for kinds that exist to contain others rather than be held directly. */
    public boolean isContainer() {
        return this == CEMETERY || this == CEMETERY_SECTION || this == MARKET;
    }

    /** {@code true} for kinds a citizen can hold under an agreement (§26). */
    public boolean isGrantable() {
        return this == PARCEL
                || this == BUILDING
                || this == CEMETERY_PLOT
                || this == MARKET_STALL
                || this == KIOSK
                || this == PUBLIC_SPACE;
    }
}
