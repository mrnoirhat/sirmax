// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.view;

import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.sirmax.ui.designsystem.StatefulContent;
import org.sirmax.ui.designsystem.Typography;
import org.sirmax.ui.nav.RouteKey;

/**
 * A clearly-labelled "not built yet" screen for routes whose feature lands in a later phase
 * (master prompt §1.2 — never disguise the unfinished as complete).
 */
public final class PlaceholderView implements SirmaxView {

    private final RouteKey route;
    private final String titleKey;
    private final VBox root;

    public PlaceholderView(RouteKey route, String titleKey) {
        this.route = route;
        this.titleKey = titleKey;

        StatefulContent body = new StatefulContent(new Region());
        body.showEmpty("placeholder.title", "placeholder.detail");

        root = new VBox(18, Typography.display(titleKey), body);
    }

    @Override
    public RouteKey route() {
        return route;
    }

    @Override
    public String titleKey() {
        return titleKey;
    }

    @Override
    public Parent node() {
        return root;
    }
}
