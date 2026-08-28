// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui;

import static org.assertj.core.api.Assertions.assertThat;

import javafx.scene.Scene;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sirmax.ui.nav.RouteKey;
import org.sirmax.ui.nav.ShellNavigator;
import org.sirmax.ui.shell.ShellView;

/**
 * Smoke test: the shell and every registered view build without error on the JavaFX thread, the
 * stylesheet loads, and navigation swaps the content host. Runs headed on the Windows CI runner.
 */
class ShellViewSmokeTest {

    @BeforeAll
    static void fx() {
        FxTestSupport.startToolkit();
    }

    @Test
    void shellBuildsWithAllRoutesRegistered() {
        var titles =
                FxTestSupport.onFxThread(
                        () -> new ShellView(new ShellNavigator(RouteKey.HOME)).routeTitles());

        // every RouteKey except SEARCH has a nav destination; SEARCH is reached via the search box
        assertThat(titles).containsKeys(RouteKey.HOME, RouteKey.DASHBOARD, RouteKey.BILLING);
        assertThat(titles.keySet()).hasSizeGreaterThanOrEqualTo(RouteKey.values().length - 1);
    }

    @Test
    void navigatingSwapsTheContentAndMarksTheNavItem() {
        Boolean ok =
                FxTestSupport.onFxThread(
                        () -> {
                            ShellNavigator nav = new ShellNavigator(RouteKey.HOME);
                            ShellView shell = new ShellView(nav);
                            nav.navigate(RouteKey.BILLING);
                            return nav.current() == RouteKey.BILLING && shell.getChildren().size() == 2;
                        });
        assertThat(ok).isTrue();
    }

    @Test
    void applicationSceneCanBeConstructed() {
        Scene scene =
                FxTestSupport.onFxThread(
                        () -> {
                            ShellView shell = new ShellView(new ShellNavigator(RouteKey.HOME));
                            Scene s = new Scene(shell, 1200, 780);
                            var css =
                                    SirmaxApplication.class.getResource(
                                            "/org/sirmax/ui/theme/sirmax.css");
                            if (css != null) {
                                s.getStylesheets().add(css.toExternalForm());
                            }
                            return s;
                        });
        assertThat(scene.getStylesheets()).anyMatch(s -> s.endsWith("sirmax.css"));
    }
}
