// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.app;

import org.sirmax.application.port.Clock;
import org.sirmax.infrastructure.time.SystemClock;

/**
 * Hand-wired dependency graph for the desktop client.
 *
 * <p>Phase 1 wires only the {@link Clock}. Each later phase adds the adapters it needs
 * (repositories, audit sink, unit of work, PDF renderer, printer, backup target) and the use cases
 * that consume them. There is deliberately no DI container.
 */
public final class CompositionRoot {

    private final Clock clock;

    private CompositionRoot(Clock clock) {
        this.clock = clock;
    }

    public static CompositionRoot bootstrap() {
        Clock clock = new SystemClock();
        return new CompositionRoot(clock);
    }

    public Clock clock() {
        return clock;
    }
}
