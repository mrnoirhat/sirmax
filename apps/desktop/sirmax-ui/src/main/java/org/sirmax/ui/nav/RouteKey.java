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
    /** The new-case wizard; reached from Home or the worklist, not from the sidebar. */
    PROCEDURE_NEW,
    /** One case, opened with its id as the route argument. */
    PROCEDURE_DETAIL,
    BILLING,
    CASH,
    DOCUMENTS,
    CITIZENS,
    DEPARTMENTS,
    /** Authoring the service catalogue: what the municipality offers and what it charges. */
    SERVICES,
    SETTINGS,
    REPORTS,
    /** Reached via the top-bar search box, not the task navigation. */
    SEARCH,
    /** Design System gallery — a developer tool, reached via Ctrl+Shift+G / the Help menu. */
    STYLEGUIDE
}
