// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.sirmax.ui.shell.ShellView;

/**
 * The JavaFX {@link Application} for the SIRMAX desktop client.
 *
 * <p>Phase 1 renders the shell only (top bar, task navigation, task-first home). Feature modules
 * are mounted into the shell's content area from Phase 2 on. The real entry point is {@code
 * org.sirmax.app.Launcher} in {@code sirmax-app}, which builds the dependency graph and calls
 * {@link Application#launch}.
 */
public final class SirmaxApplication extends Application {

    public static final String APP_TITLE = "SIRMAX — La gestión municipal, simplificada";

    @Override
    public void start(Stage stage) {
        ShellView shell = new ShellView();

        Scene scene = new Scene(shell, 1180, 760);
        var stylesheet = SirmaxApplication.class.getResource("/org/sirmax/ui/theme/sirmax.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setTitle(APP_TITLE);
        stage.setMinWidth(960);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }
}
