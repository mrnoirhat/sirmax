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
    private String argument;

    public ShellNavigator(RouteKey initial) {
        this.current = Objects.requireNonNull(initial, "initial");
    }

    @Override
    public RouteKey current() {
        return current;
    }

    @Override
    public void navigate(RouteKey target) {
        navigate(target, null);
    }

    @Override
    public void navigate(RouteKey target, String newArgument) {
        Objects.requireNonNull(target, "target");
        // Re-navigating to the same route with a *different* argument is a real move: it is how the
        // operator jumps from one case to another without going through the list.
        if (target == current && Objects.equals(argument, newArgument)) {
            return;
        }
        back.push(current);
        while (back.size() > MAX_HISTORY) {
            back.removeLast();
        }
        current = target;
        argument = newArgument;
        notifyListeners();
    }

    @Override
    public java.util.Optional<String> argument() {
        return java.util.Optional.ofNullable(argument);
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
        argument = null;
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
