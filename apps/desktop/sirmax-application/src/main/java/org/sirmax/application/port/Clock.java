// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.time.Instant;

/**
 * The current instant, as a port so use cases stay deterministic under test.
 *
 * <p>Infrastructure provides a system-clock adapter; tests provide a fixed one.
 */
@FunctionalInterface
public interface Clock {

    Instant now();
}
