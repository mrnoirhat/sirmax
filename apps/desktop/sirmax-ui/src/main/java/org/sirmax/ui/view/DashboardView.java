// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.view;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.sirmax.ui.designsystem.Banner;
import org.sirmax.ui.designsystem.Cards;
import org.sirmax.ui.designsystem.Styles;
import org.sirmax.ui.designsystem.Typography;
import org.sirmax.ui.nav.RouteKey;

/**
 * The role dashboard: "What needs my attention? What do I do next? What happened today?" — not a wall
 * of charts (master prompt §51). Phase 2 shows the queue-tile layout with placeholder counts.
 */
public final class DashboardView implements SirmaxView {

    private final VBox root;

    public DashboardView() {
        FlowPane queue =
                new FlowPane(
                        14,
                        14,
                        tile("dashboard.queue.urgent", "2"),
                        tile("dashboard.queue.overdue", "4"),
                        tile("dashboard.queue.today", "7"),
                        tile("dashboard.queue.week", "12"));

        root =
                new VBox(
                        18,
                        Typography.display("dashboard.title"),
                        new Banner(Banner.Severity.INFO, "placeholder.title", "placeholder.detail"),
                        Typography.subtitle("dashboard.attention"),
                        queue);
    }

    @Override
    public RouteKey route() {
        return RouteKey.DASHBOARD;
    }

    @Override
    public String titleKey() {
        return "dashboard.title";
    }

    @Override
    public Parent node() {
        return root;
    }

    private static VBox tile(String labelKey, String count) {
        Label number = new Label(count);
        number.getStyleClass().add(Styles.DISPLAY);
        VBox card = Cards.card(number, Typography.body(labelKey));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(180);
        return card;
    }
}
