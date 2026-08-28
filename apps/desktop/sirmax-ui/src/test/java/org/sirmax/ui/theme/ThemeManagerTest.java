// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.theme;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sirmax.ui.FxTestSupport;

class ThemeManagerTest {

    @BeforeAll
    static void fx() {
        FxTestSupport.startToolkit();
    }

    @Test
    void toggledFlipsBetweenLightAndDark() {
        assertThat(Theme.LIGHT.toggled()).isEqualTo(Theme.DARK);
        assertThat(Theme.DARK.toggled()).isEqualTo(Theme.LIGHT);
    }

    @Test
    void applyingDarkAddsTheStyleClassAndBackRemovesIt() {
        FxTestSupport.onFxThread(
                () -> {
                    Region root = new Region();
                    ThemeManager tm = new ThemeManager(root, Theme.LIGHT);
                    assertThat(root.getStyleClass()).doesNotContain("sirmax-dark");

                    tm.toggle();
                    assertThat(tm.current()).isEqualTo(Theme.DARK);
                    assertThat(root.getStyleClass()).contains("sirmax-dark");

                    tm.set(Theme.LIGHT);
                    assertThat(root.getStyleClass()).doesNotContain("sirmax-dark");
                    return null;
                });
    }

    @Test
    void listenersFireOnChangeOnly() {
        FxTestSupport.onFxThread(
                () -> {
                    ThemeManager tm = new ThemeManager(new Region(), Theme.LIGHT);
                    List<Theme> seen = new ArrayList<>();
                    tm.addListener(seen::add);

                    tm.set(Theme.LIGHT); // no-op
                    tm.set(Theme.DARK);
                    tm.set(Theme.DARK); // no-op

                    assertThat(seen).containsExactly(Theme.DARK);
                    return null;
                });
    }
}
