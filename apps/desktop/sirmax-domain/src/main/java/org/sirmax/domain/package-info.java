// SPDX-License-Identifier: AGPL-3.0-or-later

/**
 * The SIRMAX domain model: the configurable municipal service-and-records platform's shared spine.
 *
 * <p><strong>Purity rules</strong> (enforced by {@code sirmax-architecture-tests}):
 *
 * <ul>
 *   <li>no dependency on JavaFX, JDBC ({@code java.sql}), infrastructure or networking;
 *   <li>no I/O;
 *   <li>no literal user-facing strings — carry {@link org.sirmax.shared.i18n.MessageKey} instead.
 * </ul>
 *
 * <p>Sub-packages (added from Phase 3 on) hold the core areas — identity, service, procedure,
 * document, finance — and, from Phase 7, the specialty modules (registry, certificate, urban,
 * cadastre, cemetery, market, permit, mobility, ops, community), each plugging into the same
 * procedure / document / finance / audit spine rather than defining its own architecture.
 */
package org.sirmax.domain;
