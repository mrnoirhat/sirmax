// SPDX-License-Identifier: AGPL-3.0-or-later

/**
 * Infrastructure adapters: the concrete implementations of the ports declared in {@code
 * org.sirmax.application.port}.
 *
 * <p>This is the <strong>only</strong> module permitted to depend on {@code java.sql}, JavaFX-free
 * external toolkits, the network, the filesystem and OS printing. Swapping SQLite for a future
 * server API means replacing adapters here without touching the domain or application layers.
 *
 * <p>Phase 1 ships the clock and a SQLite connection factory; repositories, the migration runner,
 * the PDF renderer, the Windows printer adapter and the Google Drive backup target arrive in later
 * phases (see {@code ROADMAP.md}).
 */
package org.sirmax.infrastructure;
