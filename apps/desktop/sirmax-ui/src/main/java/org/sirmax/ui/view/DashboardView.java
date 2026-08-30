// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.view;

import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.sirmax.application.port.ProcedureQuery;
import org.sirmax.domain.security.Permission;
import org.sirmax.ui.app.AppServices;
import org.sirmax.ui.app.UiSession;
import org.sirmax.ui.designsystem.Banner;
import org.sirmax.ui.designsystem.Cards;
import org.sirmax.ui.designsystem.Styles;
import org.sirmax.ui.designsystem.Typography;
import org.sirmax.ui.nav.Navigator;
import org.sirmax.ui.nav.RouteKey;

/**
 * The caseload at a glance (master prompt §35).
 *
 * <p>Four counts, each of which is a question someone actually asks on arriving: what is overdue,
 * what is nobody's, what is mine, and how much is open at all. Every tile navigates to the worklist,
 * because a number that cannot be opened is trivia.
 *
 * <p>The counts are read from the repository. This screen previously showed four hard-coded
 * figures — a leftover from the design-system demo — which is worse than showing nothing: invented
 * numbers on a dashboard are indistinguishable from real ones, and someone eventually plans a
 * morning around them.
 */
public final class DashboardView implements SirmaxView {

    /** Counting is a query; this caps how many rows it will pull to count them. */
    private static final int SCAN_LIMIT = 2000;

    private final AppServices services;
    private final UiSession session;
    private final Navigator navigator;

    private final FlowPane tiles = new FlowPane(14, 14);
    private final Banner denied = new Banner();
    private final VBox root = new VBox(18);

    /** The Design System demo shell has no application graph; the tiles then stay empty. */
    public DashboardView() {
        this(null, new UiSession(), null);
    }

    public DashboardView(AppServices services, UiSession session, Navigator navigator) {
        this.services = services;
        this.session = session;
        this.navigator = navigator;

        root.getChildren()
                .addAll(
                        Typography.display("dashboard.title"),
                        denied,
                        Typography.subtitle("dashboard.attention"),
                        tiles);
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
        refresh();
        return root;
    }

    /** Recount. Cheap enough to do on every visit, and stale counts are worse than a short wait. */
    public void refresh() {
        tiles.getChildren().clear();

        if (services == null) {
            return;
        }
        if (!session.can(Permission.PROCEDURE_READ)) {
            denied.show(Banner.Severity.INFO, "dashboard.forbidden", "dashboard.forbidden.hint");
            return;
        }
        denied.hide();

        String userId = session.current().map(s -> s.user().id()).orElse(null);

        tiles.getChildren()
                .addAll(
                        tile("dashboard.queue.overdue", count(ProcedureQuery.overdue(SCAN_LIMIT))),
                        tile(
                                "dashboard.queue.unassigned",
                                count(ProcedureQuery.unassigned(SCAN_LIMIT))),
                        tile(
                                "dashboard.queue.mine",
                                userId == null
                                        ? 0
                                        : count(ProcedureQuery.assignedTo(userId, SCAN_LIMIT))),
                        tile("dashboard.queue.open", count(ProcedureQuery.openWork(SCAN_LIMIT))));
    }

    private long count(ProcedureQuery query) {
        return services.procedures().countSearch(query);
    }

    private VBox tile(String labelKey, long count) {
        Label number = new Label(Long.toString(count));
        number.getStyleClass().add(Styles.DISPLAY);

        VBox card = Cards.card(number, Typography.body(labelKey));
        card.getStyleClass().add(Styles.TILE);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(180);

        if (navigator != null) {
            card.setCursor(javafx.scene.Cursor.HAND);
            card.setOnMouseClicked(e -> navigator.navigate(RouteKey.PROCEDURES));
        }
        return card;
    }

    /** Exposed for tests: the counts currently on screen, in tile order. */
    public List<Long> counts() {
        return tiles.getChildren().stream()
                .map(node -> ((Label) ((VBox) node).getChildren().getFirst()).getText())
                .map(Long::valueOf)
                .toList();
    }
}
