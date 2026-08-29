// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.sirmax.ui.FxTestSupport;
import org.sirmax.ui.nav.RouteKey;
import org.sirmax.ui.nav.ShellNavigator;
import org.sirmax.ui.shell.ShellView;
import org.sirmax.ui.theme.Theme;

/**
 * Renders the real views to PNG for the landing page and the documentation.
 *
 * <p>Uses {@link Scene#snapshot} rather than capturing the desktop. A screen capture depends on the
 * window manager, the compositor and the screen size, and on this project's own machine it produced
 * images that did not match what the application actually renders — which is worse than no
 * screenshot, because it looks authoritative. A scene snapshot is what JavaFX itself drew.
 *
 * <p>Off by default: it writes files outside the build directory, so it runs only when asked.
 *
 * <pre>{@code
 * ./gradlew :sirmax-ui:test --tests "*ScreenshotGenerator*" -Dsirmax.screenshots=true
 * }</pre>
 */
@EnabledIfSystemProperty(named = "sirmax.screenshots", matches = "true")
class ScreenshotGenerator {

    private static final double WIDTH = 1200;
    private static final double HEIGHT = 780;

    private static final Path OUTPUT =
            Path.of("..", "..", "landing", "public", "screenshots").normalize();

    @BeforeAll
    static void fx() {
        FxTestSupport.startToolkit();
    }

    @Test
    void firstRunSetup() throws IOException {
        capture("sirmax-primer-arranque.png", new LoginView(new StubServices(true), s -> {}));
    }

    @Test
    void signIn() throws IOException {
        capture("sirmax-inicio-sesion.png", new LoginView(new StubServices(false), s -> {}));
    }

    @Test
    void shell() throws IOException {
        // The Design System shell, with no application graph behind it: the feature views need a
        // database, and a screenshot of seeded demo data would be advertising, not documentation.
        capture("sirmax-shell.png", new ShellView(new ShellNavigator(RouteKey.HOME), Theme.LIGHT));
    }

    private void capture(String fileName, javafx.scene.Parent view) throws IOException {
        WritableImage image =
                FxTestSupport.onFxThread(
                        () -> {
                            Scene scene = new Scene(view, WIDTH, HEIGHT);
                            var stylesheet =
                                    LoginView.class.getResource("/org/sirmax/ui/theme/sirmax.css");
                            if (stylesheet != null) {
                                scene.getStylesheets().add(stylesheet.toExternalForm());
                            }
                            view.applyCss();
                            view.layout();
                            return scene.snapshot(null);
                        });

        Files.createDirectories(OUTPUT);
        File target = OUTPUT.resolve(fileName).toFile();
        javax.imageio.ImageIO.write(
                javafx.embed.swing.SwingFXUtils.fromFXImage(image, null), "png", target);
        System.out.println("Wrote " + target.getAbsolutePath());
    }
}
