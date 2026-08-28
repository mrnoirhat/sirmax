// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.designsystem;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.sirmax.ui.i18n.Messages;

/**
 * An inline, persistent message block (info / success / warning / danger).
 *
 * <p>An icon glyph and the title text carry the meaning; the tint is secondary so the message still
 * reads in black &amp; white (master prompt §59C).
 */
public final class Banner extends HBox {

    public enum Severity {
        INFO(Styles.INFO, "ℹ"),
        SUCCESS(Styles.SUCCESS, "✓"),
        WARNING(Styles.WARNING, "⚠"),
        DANGER(Styles.DANGER, "✕");

        final String styleClass;
        final String glyph;

        Severity(String styleClass, String glyph) {
            this.styleClass = styleClass;
            this.glyph = glyph;
        }
    }

    public Banner(Severity severity, String titleKey, String messageKey) {
        super(10);
        setAlignment(Pos.TOP_LEFT);
        getStyleClass().addAll(Styles.BANNER, severity.styleClass);

        Label icon = new Label(severity.glyph);

        Label title = new Label(Messages.get(titleKey));
        title.getStyleClass().add(Styles.BANNER_TITLE);

        VBox text = new VBox(2, title);
        if (messageKey != null) {
            Label body = new Label(Messages.get(messageKey));
            body.setWrapText(true);
            text.getChildren().add(body);
        }
        HBox.setHgrow(text, Priority.ALWAYS);

        getChildren().addAll(icon, text);
    }
}
