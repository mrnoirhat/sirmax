// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.nav;

/**
 * The top-level destinations of the desktop shell.
 *
 * <p>Task-oriented entries come first (master prompt §35); department and administration navigation
 * are secondary. Feature phases mount real views against these keys; until then most resolve to a
 * labelled placeholder.
 */
public enum RouteKey {
    HOME,
    DASHBOARD,
    PROCEDURES,
    BILLING,
    CASH,
    DOCUMENTS,
    CITIZENS,
    DEPARTMENTS,
    SETTINGS,
    REPORTS,
    SEARCH
}
