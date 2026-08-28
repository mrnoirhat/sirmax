// SPDX-License-Identifier: AGPL-3.0-or-later

/**
 * The SIRMAX desktop presentation layer (JavaFX).
 *
 * <p>Holds the application shell, the design system (theme, typography, reusable controls, the
 * loading / empty / error / success states), navigation and view-models. JavaFX types appear here
 * and nowhere else in the codebase; this package must not touch {@code java.sql}.
 *
 * <p>Navigation is <em>task-first</em>: the home screen asks "¿Qué necesitas hacer?" and department
 * navigation is secondary (see {@code docs/ux/ux-map.md}).
 */
package org.sirmax.ui;
