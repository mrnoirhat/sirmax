// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.nav;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShellNavigatorTest {

    @Test
    void startsAtInitialRoute() {
        assertThat(new ShellNavigator(RouteKey.HOME).current()).isEqualTo(RouteKey.HOME);
    }

    @Test
    void navigateChangesRouteAndNotifiesListeners() {
        ShellNavigator nav = new ShellNavigator(RouteKey.HOME);
        List<RouteKey> seen = new ArrayList<>();
        nav.addListener(seen::add);

        nav.navigate(RouteKey.BILLING);

        assertThat(nav.current()).isEqualTo(RouteKey.BILLING);
        assertThat(seen).containsExactly(RouteKey.BILLING);
    }

    @Test
    void navigatingToTheCurrentRouteIsANoOp() {
        ShellNavigator nav = new ShellNavigator(RouteKey.HOME);
        List<RouteKey> seen = new ArrayList<>();
        nav.addListener(seen::add);

        nav.navigate(RouteKey.HOME);

        assertThat(seen).isEmpty();
        assertThat(nav.canGoBack()).isFalse();
    }

    @Test
    void backReturnsToThePreviousRoute() {
        ShellNavigator nav = new ShellNavigator(RouteKey.HOME);
        nav.navigate(RouteKey.PROCEDURES);
        nav.navigate(RouteKey.BILLING);

        assertThat(nav.canGoBack()).isTrue();
        nav.back();
        assertThat(nav.current()).isEqualTo(RouteKey.PROCEDURES);
        nav.back();
        assertThat(nav.current()).isEqualTo(RouteKey.HOME);
        assertThat(nav.canGoBack()).isFalse();
    }

    @Test
    void backOnEmptyHistoryIsSafe() {
        ShellNavigator nav = new ShellNavigator(RouteKey.HOME);
        nav.back();
        assertThat(nav.current()).isEqualTo(RouteKey.HOME);
    }

    @Test
    void historyIsBounded() {
        ShellNavigator nav = new ShellNavigator(RouteKey.HOME);
        RouteKey[] all = RouteKey.values();
        for (int i = 0; i < 200; i++) {
            nav.navigate(all[i % all.length]);
        }
        int hops = 0;
        while (nav.canGoBack() && hops < 1000) {
            nav.back();
            hops++;
        }
        assertThat(hops).isLessThanOrEqualTo(50);
    }
}
