// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.scene.Parent;

/**
 * Applies a {@link Theme} to the scene root by toggling the {@code sirmax-dark} style class, and
 * notifies listeners on change.
 *
 * <p>Session-scoped for now; a persisted preference arrives with the settings store (Phase 3).
 */
public final class ThemeManager {

    private static final String DARK_CLASS = "sirmax-dark";

    private final Parent root;
    private final List<Consumer<Theme>> listeners = new ArrayList<>();
    private Theme theme;

    public ThemeManager(Parent root, Theme initial) {
        this.root = Objects.requireNonNull(root, "root");
        this.theme = Objects.requireNonNull(initial, "initial");
        apply();
    }

    public Theme current() {
        return theme;
    }

    public void set(Theme next) {
        Objects.requireNonNull(next, "next");
        if (next == theme) {
            return;
        }
        theme = next;
        apply();
        for (Consumer<Theme> l : List.copyOf(listeners)) {
            l.accept(theme);
        }
    }

    public void toggle() {
        set(theme.toggled());
    }

    public void addListener(Consumer<Theme> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    private void apply() {
        root.getStyleClass().remove(DARK_CLASS);
        if (theme == Theme.DARK) {
            root.getStyleClass().add(DARK_CLASS);
        }
    }
}
