// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.asset;

/**
 * What kind of arrangement an {@link Agreement} is (master prompt §26).
 *
 * <p>All four share one lifecycle, so they are a discriminator rather than four classes. The kind
 * matters for the document that gets printed and the fee rules that apply, not for the contract's
 * behaviour.
 */
public enum AgreementKind {
    /** Rent of municipal property — "contrato inicial de arrendamiento" (§25). */
    LEASE,
    /** A long-term grant, typically a cemetery plot (§6). */
    CONCESSION,
    /** A market stall or casilla (§7). */
    STALL_ASSIGNMENT,
    /** Temporary use of public space — a stand, a event, a container (§9). */
    PUBLIC_SPACE_PERMIT,
    OTHER
}
