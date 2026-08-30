// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.shell;

import javafx.application.Platform;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.sirmax.ui.i18n.Messages;
import org.sirmax.ui.nav.Navigator;
import org.sirmax.ui.nav.RouteKey;
import org.sirmax.ui.theme.Theme;
import org.sirmax.ui.theme.ThemeManager;

/**
 * The application menu bar: Archivo / Ver / Ayuda.
 *
 * <p>Kept lean — menu items mirror the keyboard shortcuts and the theme toggle so a mouse-only
 * operator can reach everything.
 */
public final class AppMenuBar extends MenuBar {

    public AppMenuBar(
            Navigator navigator,
            ThemeManager themeManager,
            Runnable showShortcuts,
            Runnable showStyleGuide) {

        Menu file = new Menu(Messages.get("menu.file"));
        MenuItem quit = item("menu.file.quit", Platform::exit);
        quit.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
        file.getItems().add(quit);

        Menu view = new Menu(Messages.get("menu.view"));
        MenuItem home = item("menu.view.home", () -> navigator.navigate(RouteKey.HOME));
        home.setAccelerator(new KeyCodeCombination(KeyCode.HOME, KeyCombination.ALT_DOWN));
        view.getItems()
                .addAll(
                        home,
                        item("menu.view.dashboard", () -> navigator.navigate(RouteKey.DASHBOARD)),
                        darkToggle(themeManager));

        Menu help = new Menu(Messages.get("menu.help"));
        MenuItem shortcuts = item("menu.help.shortcuts", showShortcuts);
        shortcuts.setAccelerator(new KeyCodeCombination(KeyCode.F1));
        MenuItem styleGuide = item("menu.help.styleguide", showStyleGuide);
        MenuItem about = item("menu.help.about", AppMenuBar::showAbout);
        styleGuide.setAccelerator(
                new KeyCodeCombination(
                        KeyCode.G, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        help.getItems().addAll(shortcuts, styleGuide, new SeparatorMenuItem(), about);

        getMenus().addAll(file, view, help);
    }

    /**
     * Version and licence.
     *
     * <p>The version is the first thing a support request needs and the last thing anyone can find
     * — «Ayuda → Acerca de» is where people look, so it has to be there. It is read from the jar's
     * manifest, so it cannot drift from what was actually built.
     */
    private static void showAbout() {
        String version =
                java.util.Optional.ofNullable(AppMenuBar.class.getPackage().getImplementationVersion())
                        .orElseGet(() -> Messages.get("about.version_unknown"));
        javafx.scene.control.Alert alert =
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(Messages.get("menu.help.about"));
        alert.setHeaderText(Messages.get("app.title"));
        alert.setContentText(Messages.get("about.body", version));
        alert.getDialogPane().getStylesheets().addAll(
                AppMenuBar.class.getResource("/org/sirmax/ui/theme/sirmax.css").toExternalForm());
        alert.showAndWait();
    }

    private static MenuItem item(String labelKey, Runnable action) {
        MenuItem item = new MenuItem(Messages.get(labelKey));
        item.setOnAction(e -> action.run());
        return item;
    }

    private static CheckMenuItem darkToggle(ThemeManager themeManager) {
        CheckMenuItem dark = new CheckMenuItem(Messages.get("menu.view.dark"));
        dark.setSelected(themeManager.current() == Theme.DARK);
        dark.setOnAction(e -> themeManager.set(dark.isSelected() ? Theme.DARK : Theme.LIGHT));
        themeManager.addListener(t -> dark.setSelected(t == Theme.DARK));
        return dark;
    }
}
