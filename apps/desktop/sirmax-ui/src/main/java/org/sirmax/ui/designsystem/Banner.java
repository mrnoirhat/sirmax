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

    private final Label icon = new Label();
    private final Label title = new Label();
    private final Label body = new Label();
    private Severity severity;

    public Banner(Severity severity, String titleKey, String messageKey) {
        this();
        show(severity, titleKey, messageKey);
    }

    /**
     * An empty banner whose content is set later with {@link #show}. Views that surface a validation
     * result — "faltan 2 requisitos", "posible duplicado" — reuse one banner instead of rebuilding
     * the node and losing its place in the layout.
     */
    public Banner() {
        super(10);
        setAlignment(Pos.TOP_LEFT);
        getStyleClass().add(Styles.BANNER);

        title.getStyleClass().add(Styles.BANNER_TITLE);
        body.setWrapText(true);
        body.setVisible(false);
        body.setManaged(false);

        VBox text = new VBox(2, title, body);
        HBox.setHgrow(text, Priority.ALWAYS);
        getChildren().addAll(icon, text);

        setVisible(false);
        setManaged(false);
    }

    /** Replace the content and make the banner visible. {@code messageKey} may be null. */
    public void show(Severity newSeverity, String titleKey, String messageKey, Object... args) {
        if (severity != null) {
            getStyleClass().remove(severity.styleClass);
        }
        severity = newSeverity;
        getStyleClass().add(severity.styleClass);

        icon.setText(severity.glyph);
        title.setText(Messages.get(titleKey, args));

        boolean hasBody = messageKey != null;
        body.setText(hasBody ? Messages.get(messageKey) : "");
        body.setVisible(hasBody);
        body.setManaged(hasBody);

        setVisible(true);
        setManaged(true);
    }

    public void hide() {
        setVisible(false);
        setManaged(false);
    }

    /** Exposed for tests: the text currently displayed, or empty when hidden. */
    public String currentTitle() {
        return isVisible() ? title.getText() : "";
    }
}
