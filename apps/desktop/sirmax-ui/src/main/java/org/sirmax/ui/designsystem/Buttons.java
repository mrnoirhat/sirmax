// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.designsystem;

import javafx.scene.control.Button;
import org.sirmax.ui.i18n.Messages;

/**
 * Button factories for the four SIRMAX variants. Text is passed as an i18n key.
 *
 * <p>Every screen has one obvious primary action (master prompt §12); use {@link #primary} sparingly.
 */
public final class Buttons {

    private Buttons() {}

    public static Button primary(String labelKey, Runnable action) {
        return build(labelKey, action, Styles.PRIMARY);
    }

    public static Button secondary(String labelKey, Runnable action) {
        return build(labelKey, action);
    }

    public static Button danger(String labelKey, Runnable action) {
        return build(labelKey, action, Styles.DANGER);
    }

    public static Button ghost(String labelKey, Runnable action) {
        return build(labelKey, action, Styles.GHOST);
    }

    private static Button build(String labelKey, Runnable action, String... extraClasses) {
        Button b = new Button(Messages.get(labelKey));
        b.getStyleClass().addAll(extraClasses);
        if (action != null) {
            b.setOnAction(e -> action.run());
        }
        return b;
    }
}
