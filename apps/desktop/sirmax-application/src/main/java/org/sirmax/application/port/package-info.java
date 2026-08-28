// SPDX-License-Identifier: AGPL-3.0-or-later

/**
 * Ports: interfaces the application layer needs and the infrastructure layer implements.
 *
 * <p>Keeping these here (not in infrastructure) is what lets a future HTTP API reuse the domain and
 * application layers by swapping adapters (see {@code ARCHITECTURE.md} §11).
 */
package org.sirmax.application.port;
