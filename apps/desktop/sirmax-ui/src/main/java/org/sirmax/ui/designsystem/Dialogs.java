// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.designsystem;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import org.sirmax.ui.i18n.Messages;

/**
 * Confirmation and message dialogs with plain-language, meaningful wording (master prompt §12, §78).
 *
 * <p>Confirmations name the concrete action rather than asking "¿Estás seguro?".
 */
public final class Dialogs {

    private Dialogs() {}

    /**
     * A confirmation dialog. {@code confirmKey} is the label of the primary (confirming) button and
     * should name the action, e.g. "Anular factura".
     *
     * @return {@code true} if the operator confirmed
     */
    public static boolean confirm(Window owner, String titleKey, String messageKey, String confirmKey) {
        Alert alert = base(Alert.AlertType.CONFIRMATION, owner, titleKey, messageKey);

        ButtonType confirm = new ButtonType(Messages.get(confirmKey), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel =
                new ButtonType(Messages.get("action.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(cancel, confirm);

        return alert.showAndWait().filter(bt -> bt == confirm).isPresent();
    }

    public static void info(Window owner, String titleKey, String messageKey) {
        base(Alert.AlertType.INFORMATION, owner, titleKey, messageKey).showAndWait();
    }

    public static void error(Window owner, String titleKey, String messageKey) {
        base(Alert.AlertType.ERROR, owner, titleKey, messageKey).showAndWait();
    }

    private static Alert base(Alert.AlertType type, Window owner, String titleKey, String messageKey) {
        Alert alert = new Alert(type);
        alert.initOwner(owner);
        alert.setTitle(Messages.get(titleKey));
        alert.setHeaderText(null);
        alert.setContentText(Messages.get(messageKey));
        alert.getDialogPane().getStyleClass().add(Styles.DIALOG);
        var css = Dialogs.class.getResource("/org/sirmax/ui/theme/sirmax.css");
        if (css != null) {
            alert.getDialogPane().getStylesheets().add(css.toExternalForm());
        }
        return alert;
    }
}
