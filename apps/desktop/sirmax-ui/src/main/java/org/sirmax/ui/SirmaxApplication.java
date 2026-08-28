// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.sirmax.ui.i18n.Messages;
import org.sirmax.ui.nav.RouteKey;
import org.sirmax.ui.nav.ShellNavigator;
import org.sirmax.ui.shell.KeyboardShortcuts;
import org.sirmax.ui.shell.ShellView;
import org.sirmax.ui.theme.Theme;

/**
 * The JavaFX {@link Application} for the SIRMAX desktop client.
 *
 * <p>Phase 2 renders the shell: task-first navigation, the Design System, and the
 * loading/empty/error/success states, with keyboard shortcuts. Feature modules mount their views
 * against {@link RouteKey}s from Phase 3 on. The real entry point is {@code org.sirmax.app.Launcher}
 * in {@code sirmax-app}.
 */
public final class SirmaxApplication extends Application {

    @Override
    public void start(Stage stage) {
        ShellNavigator navigator = new ShellNavigator(RouteKey.HOME);
        ShellView shell = new ShellView(navigator, initialTheme());

        Scene scene = new Scene(shell, 1200, 780);
        var stylesheet = SirmaxApplication.class.getResource("/org/sirmax/ui/theme/sirmax.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        KeyboardShortcuts.install(scene, shell);

        stage.setTitle(Messages.get("app.title"));
        stage.setMinWidth(1000);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    /** Optional {@code -Dsirmax.theme=dark} for demos/screenshots; defaults to light. */
    private static Theme initialTheme() {
        return "dark".equalsIgnoreCase(System.getProperty("sirmax.theme", "light").trim())
                ? Theme.DARK
                : Theme.LIGHT;
    }
}
