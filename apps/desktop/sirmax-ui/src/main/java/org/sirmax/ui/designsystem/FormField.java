// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.designsystem;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.sirmax.ui.i18n.Messages;

/**
 * A labelled form control with an optional hint and an inline error message.
 *
 * <p>Required fields and what is missing must be clear (master prompt §78): the error text appears
 * directly under the control and the field gets the {@code invalid} style.
 */
public final class FormField extends VBox {

    private final Label errorLabel = new Label();

    public FormField(String labelKey, Node control) {
        this(labelKey, control, null);
    }

    public FormField(String labelKey, Node control, String hintKey) {
        super(4);
        getStyleClass().add(Styles.FIELD);

        Label label = new Label(Messages.get(labelKey));
        label.getStyleClass().add(Styles.FIELD_LABEL);
        label.setLabelFor(control);
        getChildren().addAll(label, control);

        if (hintKey != null) {
            Label hint = new Label(Messages.get(hintKey));
            hint.getStyleClass().add(Styles.FIELD_HINT);
            hint.setWrapText(true);
            getChildren().add(hint);
        }

        errorLabel.getStyleClass().add(Styles.FIELD_ERROR);
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        getChildren().add(errorLabel);
    }

    /** Show an inline error (i18n key). Pass {@code null} to clear it. */
    public void setError(String errorKey, Object... args) {
        boolean hasError = errorKey != null;
        errorLabel.setText(hasError ? Messages.get(errorKey, args) : "");
        errorLabel.setVisible(hasError);
        errorLabel.setManaged(hasError);
        getStyleClass().remove(Styles.INVALID);
        if (hasError) {
            getStyleClass().add(Styles.INVALID);
        }
    }

    public boolean hasError() {
        return getStyleClass().contains(Styles.INVALID);
    }
}
