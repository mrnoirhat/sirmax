// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.identity;

/**
 * Kind of identifying document. The Dominican set is the initial catalog; the country adapter can
 * add more without touching the core (master prompt §37).
 */
public enum IdentificationType {
    CEDULA,
    RNC,
    PASSPORT,
    RESIDENT_ID,
    OTHER
}
