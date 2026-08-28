// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.designsystem;

import javafx.scene.Node;
import javafx.scene.layout.VBox;

/** Surface container factories. */
public final class Cards {

    private Cards() {}

    /** A padded, bordered surface holding the given children in a vertical stack. */
    public static VBox card(Node... children) {
        VBox box = new VBox(10, children);
        box.getStyleClass().add(Styles.CARD);
        return box;
    }
}
