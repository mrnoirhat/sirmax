// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.finance;

/**
 * The kind of municipal charge (master prompt §21). A configurable taxonomy rather than a single
 * "tax" concept, so other countries' revenue models fit without rework.
 */
public enum ChargeType {
    IMPUESTO,
    ARBITRIO,
    TASA,
    CONTRIBUCION,
    CARGO_SERVICIO,
    ARRENDAMIENTO,
    RECARGO
}
