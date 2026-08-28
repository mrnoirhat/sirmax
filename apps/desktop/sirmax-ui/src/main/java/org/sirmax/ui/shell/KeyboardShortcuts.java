// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.shell;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.sirmax.ui.designsystem.Dialogs;

/**
 * Registers the shell's global keyboard shortcuts on a {@link Scene}.
 *
 * <ul>
 *   <li>{@code Ctrl+K} — focus the global search
 *   <li>{@code Alt+Home} — go to the home screen
 *   <li>{@code F1} — show the shortcuts help
 * </ul>
 */
public final class KeyboardShortcuts {

    private KeyboardShortcuts() {}

    public static void install(Scene scene, ShellView shell) {
        var accel = scene.getAccelerators();
        accel.put(
                new KeyCodeCombination(KeyCode.K, KeyCombination.CONTROL_DOWN), shell::focusSearch);
        accel.put(new KeyCodeCombination(KeyCode.HOME, KeyCombination.ALT_DOWN), shell::goHome);
        accel.put(
                new KeyCodeCombination(KeyCode.F1),
                () ->
                        Dialogs.info(
                                scene.getWindow(), "shortcuts.title", "shortcuts.help"));
    }
}
