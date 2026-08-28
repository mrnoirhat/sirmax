// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.nav;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Plain-Java {@link Navigator}: current route + a bounded back-stack + synchronous listeners.
 *
 * <p>Not thread-safe; the UI drives it from the JavaFX application thread.
 */
public final class ShellNavigator implements Navigator {

    private static final int MAX_HISTORY = 50;

    private final Deque<RouteKey> back = new ArrayDeque<>();
    private final List<Consumer<RouteKey>> listeners = new ArrayList<>();
    private RouteKey current;

    public ShellNavigator(RouteKey initial) {
        this.current = Objects.requireNonNull(initial, "initial");
    }

    @Override
    public RouteKey current() {
        return current;
    }

    @Override
    public void navigate(RouteKey target) {
        Objects.requireNonNull(target, "target");
        if (target == current) {
            return;
        }
        back.push(current);
        while (back.size() > MAX_HISTORY) {
            back.removeLast();
        }
        current = target;
        notifyListeners();
    }

    @Override
    public boolean canGoBack() {
        return !back.isEmpty();
    }

    @Override
    public void back() {
        if (back.isEmpty()) {
            return;
        }
        current = back.pop();
        notifyListeners();
    }

    @Override
    public void addListener(Consumer<RouteKey> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    private void notifyListeners() {
        for (Consumer<RouteKey> l : List.copyOf(listeners)) {
            l.accept(current);
        }
    }
}
