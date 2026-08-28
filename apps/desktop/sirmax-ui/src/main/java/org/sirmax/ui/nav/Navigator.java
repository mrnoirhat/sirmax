// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.nav;

import java.util.function.Consumer;

/**
 * Drives which top-level view the shell content area shows.
 *
 * <p>Deliberately free of JavaFX types so navigation behaviour (history, listeners, guards) is
 * unit-testable without a toolkit. The shell view subscribes with {@link #addListener} and swaps its
 * content node accordingly.
 */
public interface Navigator {

    RouteKey current();

    void navigate(RouteKey target);

    boolean canGoBack();

    /** Go to the previous route; no-op if there is none. */
    void back();

    /** Register a listener invoked (synchronously) after every route change, with the new route. */
    void addListener(Consumer<RouteKey> listener);
}
