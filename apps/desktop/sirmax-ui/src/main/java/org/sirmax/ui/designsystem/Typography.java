// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.designsystem;

import javafx.scene.control.Label;
import org.sirmax.ui.i18n.Messages;

/** Text factories that apply the Design System typography scale. Text is an i18n key. */
public final class Typography {

    private Typography() {}

    public static Label display(String key, Object... args) {
        return styled(key, Styles.DISPLAY, args);
    }

    public static Label title(String key, Object... args) {
        return styled(key, Styles.TITLE, args);
    }

    public static Label subtitle(String key, Object... args) {
        return styled(key, Styles.SUBTITLE, args);
    }

    public static Label body(String key, Object... args) {
        Label l = styled(key, Styles.BODY, args);
        l.setWrapText(true);
        return l;
    }

    public static Label muted(String key, Object... args) {
        Label l = styled(key, Styles.MUTED, args);
        l.setWrapText(true);
        return l;
    }

    /** A plain-text label (no i18n lookup) with the muted style — for data, codes, names. */
    public static Label rawMuted(String text) {
        Label l = new Label(text);
        l.getStyleClass().add(Styles.MUTED);
        return l;
    }

    private static Label styled(String key, String styleClass, Object... args) {
        Label l = new Label(Messages.get(key, args));
        l.getStyleClass().add(styleClass);
        return l;
    }
}
