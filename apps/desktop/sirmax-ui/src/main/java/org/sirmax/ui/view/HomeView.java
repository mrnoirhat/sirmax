// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.view;

import java.util.List;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.sirmax.ui.designsystem.Styles;
import org.sirmax.ui.designsystem.Typography;
import org.sirmax.ui.i18n.Messages;
import org.sirmax.ui.nav.Navigator;
import org.sirmax.ui.nav.RouteKey;

/**
 * The task-first home screen: "¿Qué necesitas hacer?" with large task cards (master prompt §33, §35).
 *
 * <p>Each card routes to the destination where that task begins. Feature phases replace the target
 * views; the routing stays the same.
 */
public final class HomeView implements SirmaxView {

    private final VBox root;

    public HomeView(Navigator navigator) {
        FlowPane tasks = new FlowPane(16, 16);
        for (Task t : Task.values()) {
            tasks.getChildren().add(card(t.labelKey, () -> navigator.navigate(t.target)));
        }
        tasks.setPrefWrapLength(760);

        root = new VBox(22, Typography.display("home.question"), tasks);
    }

    @Override
    public RouteKey route() {
        return RouteKey.HOME;
    }

    @Override
    public String titleKey() {
        return "nav.home";
    }

    @Override
    public Parent node() {
        return root;
    }

    private static Button card(String labelKey, Runnable action) {
        Button b = new Button(Messages.get(labelKey));
        b.getStyleClass().add(Styles.TASK_CARD);
        b.setPrefSize(236, 92);
        b.setWrapText(true);
        b.setOnAction(e -> action.run());
        return b;
    }

    private enum Task {
        NEW_PROCEDURE("home.task.new_procedure", RouteKey.PROCEDURES),
        ISSUE_CERTIFICATE("home.task.issue_certificate", RouteKey.DOCUMENTS),
        REGISTER_PAYMENT("home.task.register_payment", RouteKey.BILLING),
        REGISTER_DOCUMENT("home.task.register_document", RouteKey.DOCUMENTS),
        NEW_REQUEST("home.task.new_request", RouteKey.PROCEDURES),
        MANAGE_AGREEMENT("home.task.manage_agreement", RouteKey.DEPARTMENTS);

        final String labelKey;
        final RouteKey target;

        Task(String labelKey, RouteKey target) {
            this.labelKey = labelKey;
            this.target = target;
        }
    }

    /** Kept for tests / future reference: the task order the home screen renders. */
    public static List<String> taskLabelKeys() {
        return java.util.Arrays.stream(Task.values()).map(t -> t.labelKey).toList();
    }
}
