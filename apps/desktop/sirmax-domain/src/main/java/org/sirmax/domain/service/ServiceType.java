// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.service;

/**
 * Whether a municipal service produces a charge (master prompt §12).
 *
 * <p>Not every procedure is paid: complaints, community cases and service follow-ups are typically
 * {@link #GRATUITO}.
 */
public enum ServiceType {
    /** No charge. */
    GRATUITO,
    /** Always produces a fee. */
    CON_TASA,
    /** Produces a fee only when a configured condition holds. */
    TASA_CONDICIONAL,
    /** Paid outside SIRMAX (e.g. another institution collects). */
    PAGO_EXTERNO
}
