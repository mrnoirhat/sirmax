// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.nav;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NavItemTest {

    @Test
    void defaultsLeadWithHomeAndGroupTasksBeforeAdmin() {
        var items = NavItem.defaults();

        assertThat(items).isNotEmpty();
        assertThat(items.get(0).key()).isEqualTo(RouteKey.HOME);

        int lastTasks = -1;
        int firstAdmin = Integer.MAX_VALUE;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).section() == NavItem.Section.TASKS) {
                lastTasks = i;
            } else if (items.get(i).section() == NavItem.Section.ADMIN) {
                firstAdmin = Math.min(firstAdmin, i);
            }
        }
        assertThat(lastTasks).isLessThan(firstAdmin);
    }

    @Test
    void everyItemHasADistinctRoute() {
        var routes = NavItem.defaults().stream().map(NavItem::key).toList();
        assertThat(routes).doesNotHaveDuplicates();
    }

    @Test
    void constructorRejectsNulls() {
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class, () -> new NavItem(null, "x", NavItem.Section.TASKS));
    }
}
