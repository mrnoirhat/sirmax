// SPDX-License-Identifier: AGPL-3.0-or-later

/**
 * The SIRMAX application layer: use cases and the ports they depend on.
 *
 * <p>A <em>use case</em> orchestrates domain objects and ports to fulfil one operator intent
 * ("open a procedure", "issue an invoice", "register a payment"). A <em>port</em> is an interface
 * declared here and implemented by {@code sirmax-infrastructure} — repositories, the clock, the
 * audit sink, the printer, the PDF renderer, the backup target, and so on.
 *
 * <p><strong>Rules</strong> (enforced by {@code sirmax-architecture-tests}): no JavaFX, no {@code
 * java.sql}. User-facing text is carried as {@link org.sirmax.shared.i18n.MessageKey}. Expected,
 * user-facing outcomes are returned as {@link org.sirmax.shared.Result}, not thrown.
 */
package org.sirmax.application;
