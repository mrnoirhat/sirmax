// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.app;

import java.util.Optional;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.sirmax.application.security.Session;
import org.sirmax.application.usecase.Authenticate;
import org.sirmax.application.usecase.ProvisionInitialAdmin;
import org.sirmax.shared.Result;
import org.sirmax.ui.designsystem.Buttons;
import org.sirmax.ui.designsystem.Cards;
import org.sirmax.ui.designsystem.FormField;
import org.sirmax.ui.designsystem.Styles;
import org.sirmax.ui.designsystem.Typography;
import org.sirmax.ui.i18n.Messages;

/**
 * The gate in front of the shell: sign in, or — on a brand-new database — create the first
 * administrator and the municipality record (master prompt §53, safe defaults).
 *
 * <p>The same screen serves both, because they are the same moment for the person installing
 * SIRMAX. Which one it shows is decided by whether any user exists, never by a setting.
 */
public final class LoginView extends StackPane {

    private final AppServices services;
    private final Consumer<Session> onSignedIn;
    private final boolean firstRun;

    private final TextField username = new TextField();
    private final PasswordField password = new PasswordField();
    private final TextField displayName = new TextField();
    private final TextField municipality = new TextField();
    private final PasswordField confirm = new PasswordField();
    private final Label error = new Label();

    public LoginView(AppServices services, Consumer<Session> onSignedIn) {
        this.services = services;
        this.onSignedIn = onSignedIn;
        this.firstRun = services.needsInitialSetup();

        getStyleClass().add(Styles.SHELL);
        setPadding(new Insets(48));
        getChildren().add(firstRun ? buildSetupCard() : buildSignInCard());
        setAlignment(Pos.CENTER);
    }

    /** {@code true} when this is a fresh install and the card is creating the first admin. */
    public boolean isFirstRun() {
        return firstRun;
    }

    private VBox buildSignInCard() {
        username.setPromptText(Messages.get("login.username.prompt"));
        password.setOnAction(e -> signIn());

        VBox card =
                Cards.card(
                        Typography.title("login.title"),
                        Typography.muted("login.subtitle"),
                        new FormField("login.username", username),
                        new FormField("login.password", password),
                        errorLabel(),
                        Buttons.primary("login.submit", this::signIn));
        // A StackPane stretches its children unless told otherwise; without this the card grows
        // to the full window height and the sign-in button ends up floating above empty space.
        card.setMaxSize(420, javafx.scene.layout.Region.USE_PREF_SIZE);
        return card;
    }

    private VBox buildSetupCard() {
        municipality.setPromptText(Messages.get("setup.municipality.prompt"));
        confirm.setOnAction(e -> provision());

        VBox card =
                Cards.card(
                        Typography.title("setup.title"),
                        Typography.muted("setup.subtitle"),
                        new FormField("setup.municipality", municipality),
                        new FormField("setup.display_name", displayName),
                        new FormField("login.username", username),
                        new FormField("login.password", password, "setup.password.hint"),
                        new FormField("setup.confirm", confirm),
                        errorLabel(),
                        Buttons.primary("setup.submit", this::provision));
        card.setMaxSize(460, javafx.scene.layout.Region.USE_PREF_SIZE);
        return card;
    }

    private Label errorLabel() {
        error.getStyleClass().add(Styles.FIELD_ERROR);
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);
        return error;
    }

    private void signIn() {
        clearError();
        Result<Session> result =
                services
                        .authenticate()
                        .execute(
                                new Authenticate.Command(
                                        username.getText(),
                                        password.getText().toCharArray(),
                                        "desktop.login"));
        password.clear();
        if (result instanceof Result.Err<Session> err) {
            showError(err.messageKey());
            return;
        }
        onSignedIn.accept(result.orElseThrow());
    }

    private void provision() {
        clearError();
        if (!password.getText().equals(confirm.getText())) {
            showError("setup.passwords_differ");
            return;
        }
        Result<?> provisioned =
                services
                        .provisionInitialAdmin()
                        .execute(
                                new ProvisionInitialAdmin.Command(
                                        Messages.get(
                                                "setup.organization_name", municipality.getText()),
                                        municipality.getText(),
                                        "DO",
                                        username.getText(),
                                        displayName.getText(),
                                        password.getText().toCharArray()));
        if (provisioned instanceof Result.Err<?> err) {
            showError(err.messageKey());
            return;
        }
        // Sign the new administrator straight in: making them retype what they just chose would be
        // asking the operator to prove something the system already knows.
        signIn();
    }

    private void showError(String messageKey) {
        error.setText(Messages.get(messageKey));
        error.setVisible(true);
        error.setManaged(true);
    }

    private void clearError() {
        error.setVisible(false);
        error.setManaged(false);
    }

    /** Exposed for tests: the field values currently entered. */
    Optional<String> enteredUsername() {
        return Optional.ofNullable(username.getText()).filter(s -> !s.isBlank());
    }
}
