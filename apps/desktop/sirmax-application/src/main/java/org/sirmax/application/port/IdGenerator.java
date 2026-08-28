// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

/**
 * Generates entity identifiers.
 *
 * <p>A port so use cases stay deterministic under test; the infrastructure adapter produces
 * time-ordered UUIDv7 strings.
 */
@FunctionalInterface
public interface IdGenerator {

    String newId();
}
