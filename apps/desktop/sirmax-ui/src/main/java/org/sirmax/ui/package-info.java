// SPDX-License-Identifier: AGPL-3.0-or-later

/**
 * The SIRMAX desktop presentation layer (JavaFX, programmatic — no FXML; see
 * {@code docs/adr/0013-ui-programmatic-javafx.md}).
 *
 * <ul>
 *   <li>{@code i18n} — {@link org.sirmax.ui.i18n.Messages}: all user-facing text via a
 *       {@code ResourceBundle} (Spanish base). No literal strings in code.
 *   <li>{@code designsystem} — theme + reusable components (buttons, cards, banners, toasts,
 *       dialogs, form fields, tables, and the loading/empty/error/success {@code StatefulContent}).
 *   <li>{@code nav} — {@link org.sirmax.ui.nav.Navigator}: framework-free, unit-testable routing.
 *   <li>{@code view} — top-level screens, task-first (the home screen asks "¿Qué necesitas hacer?").
 *   <li>{@code shell} — {@link org.sirmax.ui.shell.ShellView} and the keyboard shortcuts.
 * </ul>
 *
 * <p>JavaFX types appear here and nowhere else in the codebase; this package must not touch
 * {@code java.sql}.
 */
package org.sirmax.ui;
