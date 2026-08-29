// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.app;

import static org.assertj.core.api.Assertions.assertThat;

import javafx.scene.Scene;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sirmax.ui.FxTestSupport;

/**
 * The first screen a municipality ever sees has to be laid out correctly.
 *
 * <p>This exists because a screenshot of the packaged application showed the setup card pushed off
 * the right edge of the window. A card that runs past the window takes its primary button with it,
 * which on the very first launch means the installation cannot be completed at all.
 */
class LoginViewLayoutTest {

    private static final double SCENE_WIDTH = 1200;
    private static final double SCENE_HEIGHT = 780;

    @BeforeAll
    static void fx() {
        FxTestSupport.startToolkit();
    }

    @Test
    void theSetupCardStaysWithinTheWindowAndIsCentred() {
        double[] geometry =
                FxTestSupport.onFxThread(
                        () -> {
                            LoginView view = new LoginView(new StubServices(true), session -> {});
                            styled(view);
                            view.applyCss();
                            view.layout();

                            Region card = (Region) view.getChildrenUnmodifiable().get(0);
                            return new double[] {
                                card.getLayoutX(), card.getWidth(), card.getLayoutY(), card.getHeight()
                            };
                        });

        double left = geometry[0];
        double width = geometry[1];

        assertThat(left).as("card left edge").isGreaterThanOrEqualTo(0);
        assertThat(left + width).as("card right edge").isLessThanOrEqualTo(SCENE_WIDTH);

        // Centred: the space either side of the card is the same, within a pixel of rounding.
        assertThat(left).as("centred horizontally").isCloseTo(SCENE_WIDTH - (left + width), within(1.0));
    }

    @Test
    void theSignInCardStaysWithinTheWindowToo() {
        double[] geometry =
                FxTestSupport.onFxThread(
                        () -> {
                            LoginView view = new LoginView(new StubServices(false), session -> {});
                            styled(view);
                            view.applyCss();
                            view.layout();

                            Region card = (Region) view.getChildrenUnmodifiable().get(0);
                            return new double[] {card.getLayoutX(), card.getWidth()};
                        });

        assertThat(geometry[0] + geometry[1]).isLessThanOrEqualTo(SCENE_WIDTH);
    }

    /**
     * A scene with the real stylesheet. Without it the test measures a layout the application never
     * renders — which is exactly how a misplaced card survives a passing test.
     */
    private static Scene styled(LoginView view) {
        Scene scene = new Scene(view, SCENE_WIDTH, SCENE_HEIGHT);
        var stylesheet = LoginView.class.getResource("/org/sirmax/ui/theme/sirmax.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        return scene;
    }

    private static org.assertj.core.data.Offset<Double> within(double tolerance) {
        return org.assertj.core.data.Offset.offset(tolerance);
    }
}
