// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.sirmax.application.security.Session;
import org.sirmax.ui.app.AppServices;
import org.sirmax.ui.app.LoginView;
import org.sirmax.ui.app.UiSession;
import org.sirmax.ui.brand.BrandMark;
import org.sirmax.ui.i18n.Messages;
import org.sirmax.ui.nav.RouteKey;
import org.sirmax.ui.nav.ShellNavigator;
import org.sirmax.ui.shell.KeyboardShortcuts;
import org.sirmax.ui.shell.ShellView;
import org.sirmax.ui.theme.Theme;

/**
 * The JavaFX {@link Application} for the SIRMAX desktop client.
 *
 * <p>Starts at the {@link LoginView} — sign in, or create the municipality and the first
 * administrator on a fresh database — and swaps the scene root for the {@link ShellView} once
 * someone is signed in. The shell is built <em>after</em> sign-in so every view can assume a
 * session and read permissions from it.
 *
 * <p>The application graph is handed in by {@code org.sirmax.app.Launcher} through
 * {@link #services(AppServices)}; without one, the shell still renders as the Design System demo.
 */
public final class SirmaxApplication extends Application {

    private static AppServices services;

    private final UiSession session = new UiSession();
    private Scene scene;

    /** Set by the composition root before {@link Application#launch}. */
    public static void services(AppServices appServices) {
        services = appServices;
    }

    /** Comfortable on a modern monitor; shrunk below when the screen is smaller. */
    private static final double PREFERRED_WIDTH = 1200;

    private static final double PREFERRED_HEIGHT = 780;

    @Override
    public void start(Stage stage) {
        javafx.geometry.Rectangle2D screen = javafx.stage.Screen.getPrimary().getVisualBounds();
        // Municipal counters run on old hardware — 1280x720 is common, and 780 is taller than
        // that. Opening a window bigger than the screen puts the primary button off the bottom
        // edge, which on the first-run setup screen means the install cannot be completed.
        double width = Math.min(PREFERRED_WIDTH, screen.getWidth());
        double height = Math.min(PREFERRED_HEIGHT, screen.getHeight());

        scene = new Scene(loginOrShell(), width, height);
        var stylesheet = SirmaxApplication.class.getResource("/org/sirmax/ui/theme/sirmax.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setTitle(Messages.get("app.title"));
        BrandMark.apply(stage);
        stage.setMinWidth(Math.min(1000, screen.getWidth()));
        stage.setMinHeight(Math.min(640, screen.getHeight()));
        stage.setScene(scene);
        // Centre on the visual bounds, which exclude the taskbar. Letting the window default to
        // (0,0) on a small screen hides the title bar behind it.
        stage.setX(screen.getMinX() + (screen.getWidth() - width) / 2);
        stage.setY(screen.getMinY() + (screen.getHeight() - height) / 2);
        stage.show();
    }

    private Parent loginOrShell() {
        if (services == null) {
            return buildShell(); // Design System demo: no database, no login
        }
        return new LoginView(services, this::onSignedIn);
    }

    private void onSignedIn(Session signedIn) {
        session.signIn(signedIn);
        scene.setRoot(buildShell());
    }

    private ShellView buildShell() {
        ShellNavigator navigator = new ShellNavigator(RouteKey.HOME);
        ShellView shell = new ShellView(navigator, initialTheme(), services, session);
        KeyboardShortcuts.install(scene, shell);
        return shell;
    }

    /** Optional {@code -Dsirmax.theme=dark} for demos/screenshots; defaults to light. */
    private static Theme initialTheme() {
        return "dark".equalsIgnoreCase(System.getProperty("sirmax.theme", "light").trim())
                ? Theme.DARK
                : Theme.LIGHT;
    }
}
