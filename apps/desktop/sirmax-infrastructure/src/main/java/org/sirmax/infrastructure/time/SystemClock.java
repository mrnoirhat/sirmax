// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.time;

import java.time.Instant;
import org.sirmax.application.port.Clock;

/** {@link Clock} backed by the JVM system UTC clock. */
public final class SystemClock implements Clock {

    private final java.time.Clock delegate = java.time.Clock.systemUTC();

    @Override
    public Instant now() {
        return delegate.instant();
    }
}
