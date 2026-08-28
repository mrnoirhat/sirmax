// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.shell;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * Scene-level shortcuts that do not correspond to a menu item.
 *
 * <p>{@code Ctrl+K} focuses the global search. Everything else (Alt+Home, F1, Ctrl+Shift+G, Ctrl+Q)
 * is owned by {@link AppMenuBar} so the accelerator and the menu label stay in one place.
 */
public final class KeyboardShortcuts {

    private KeyboardShortcuts() {}

    public static void install(Scene scene, ShellView shell) {
        scene.getAccelerators()
                .put(
                        new KeyCodeCombination(KeyCode.K, KeyCombination.CONTROL_DOWN),
                        shell::focusSearch);
    }
}
