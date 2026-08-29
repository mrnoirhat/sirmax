// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.designsystem;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.sirmax.ui.i18n.Messages;

/**
 * A transient-notification overlay: stacks toasts at the bottom-right and auto-dismisses them.
 *
 * <p>Meant to be added on top of the shell in a {@code StackPane}; it is mouse-transparent except on
 * the toasts themselves, so it never blocks the UI underneath.
 */
public final class ToastHost extends VBox {

    private static final Duration VISIBLE = Duration.seconds(4);

    public ToastHost() {
        super(10);
        setAlignment(Pos.BOTTOM_RIGHT);
        getStyleClass().add(Styles.TOAST_HOST);
        setPickOnBounds(false);
        setMouseTransparent(false);
    }

    public void info(String messageKey, Object... args) {
        show(Styles.INFO, messageKey, args);
    }

    public void success(String messageKey, Object... args) {
        show(Styles.SUCCESS, messageKey, args);
    }

    public void warning(String messageKey, Object... args) {
        show(Styles.WARNING, messageKey, args);
    }

    public void error(String messageKey, Object... args) {
        show(Styles.DANGER, messageKey, args);
    }

    private void show(String severityClass, String messageKey, Object... args) {
        HBox toast = new HBox(10);
        toast.getStyleClass().addAll(Styles.TOAST, severityClass);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setMaxWidth(360);

        Label text = new Label(Messages.get(messageKey, args));
        text.setWrapText(true);
        HBox.setHgrow(text, Priority.ALWAYS);

        Region spacer = new Region();
        Label close = new Label("✕");
        close.getStyleClass().add(Styles.MUTED);
        close.setOnMouseClicked(e -> getChildren().remove(toast));

        toast.getChildren().addAll(text, spacer, close);
        getChildren().add(toast);

        FadeTransition in = new FadeTransition(Duration.millis(140), toast);
        in.setFromValue(0);
        in.setToValue(1);

        PauseTransition wait = new PauseTransition(VISIBLE);

        FadeTransition out = new FadeTransition(Duration.millis(220), toast);
        out.setFromValue(1);
        out.setToValue(0);
        out.setOnFinished(e -> getChildren().remove(toast));

        new SequentialTransition(in, wait, out).play();
    }
}
