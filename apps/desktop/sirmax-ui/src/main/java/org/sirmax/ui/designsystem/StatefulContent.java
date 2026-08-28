// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.designsystem;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.sirmax.ui.i18n.Messages;

/**
 * A content host that shows exactly one of: the real content, a loading spinner, an empty state, or
 * a friendly error state (master prompt §1.1, §12, §78).
 *
 * <p>The error state shows a plain-language message with a retry action; the technical detail is
 * collapsed behind "Ver detalle técnico" and never shown to the operator by default.
 */
public final class StatefulContent extends StackPane {

    public enum State {
        CONTENT,
        LOADING,
        EMPTY,
        ERROR
    }

    private final Node content;
    private final VBox loadingView = state(Styles.SPINNER);
    private final VBox emptyView = state(null);
    private final VBox errorView = state(null);

    private Runnable onRetry = () -> {};

    public StatefulContent(Node content) {
        this.content = content;
        buildLoading();
        buildEmpty();
        buildError();
        getChildren().addAll(content, loadingView, emptyView, errorView);
        show(State.CONTENT);
    }

    public void setOnRetry(Runnable onRetry) {
        this.onRetry = onRetry == null ? () -> {} : onRetry;
    }

    public void show(State state) {
        content.setVisible(state == State.CONTENT);
        content.setManaged(state == State.CONTENT);
        toggle(loadingView, state == State.LOADING);
        toggle(emptyView, state == State.EMPTY);
        toggle(errorView, state == State.ERROR);
    }

    /** Show the empty state with a custom title/detail (i18n keys). */
    public void showEmpty(String titleKey, String detailKey) {
        replaceText(emptyView, Messages.get(titleKey), Messages.get(detailKey));
        show(State.EMPTY);
    }

    /** Show the error state; {@code technicalDetail} is optional and stays hidden until requested. */
    public void showError(String technicalDetail) {
        errorView.getChildren().removeIf(n -> "tech".equals(n.getUserData()));
        if (technicalDetail != null && !technicalDetail.isBlank()) {
            Label tech = new Label(technicalDetail);
            tech.getStyleClass().add(Styles.STATE_TECH);
            tech.setWrapText(true);
            tech.setVisible(false);
            tech.setManaged(false);
            tech.setUserData("tech");

            Button toggle = Buttons.ghost("state.error.details_show", null);
            toggle.setOnAction(
                    e -> {
                        boolean showing = !tech.isVisible();
                        tech.setVisible(showing);
                        tech.setManaged(showing);
                        toggle.setText(
                                Messages.get(
                                        showing
                                                ? "state.error.details_hide"
                                                : "state.error.details_show"));
                    });
            toggle.setUserData("tech");

            errorView.getChildren().addAll(toggle, tech);
        }
        show(State.ERROR);
    }

    private void buildLoading() {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.getStyleClass().add(Styles.SPINNER);
        spinner.setPrefSize(36, 36);
        Label label = new Label(Messages.get("state.loading"));
        label.getStyleClass().add(Styles.STATE_DETAIL);
        loadingView.getChildren().addAll(spinner, label);
    }

    private void buildEmpty() {
        replaceText(
                emptyView, Messages.get("state.empty.title"), Messages.get("state.empty.detail"));
    }

    private void buildError() {
        replaceText(
                errorView, Messages.get("state.error.title"), Messages.get("state.error.detail"));
        errorView.getChildren().add(Buttons.primary("state.error.retry", () -> onRetry.run()));
    }

    private static VBox state(String extraClass) {
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        box.setSpacing(10);
        box.getStyleClass().add(Styles.STATE);
        if (extraClass != null) {
            box.getStyleClass().add(extraClass);
        }
        box.setVisible(false);
        box.setManaged(false);
        return box;
    }

    private static void replaceText(VBox box, String title, String detail) {
        box.getChildren().clear();
        Label t = new Label(title);
        t.getStyleClass().add(Styles.STATE_TITLE);
        Label d = new Label(detail);
        d.getStyleClass().add(Styles.STATE_DETAIL);
        d.setWrapText(true);
        d.setMaxWidth(420);
        box.getChildren().addAll(t, d);
    }

    private static void toggle(Node n, boolean on) {
        n.setVisible(on);
        n.setManaged(on);
    }
}
