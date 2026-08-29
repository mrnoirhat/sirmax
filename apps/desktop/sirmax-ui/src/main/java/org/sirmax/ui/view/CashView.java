// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.view;

import java.math.BigDecimal;
import java.util.Optional;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.sirmax.application.usecase.ManageCashSession;
import org.sirmax.domain.finance.CashSession;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Money;
import org.sirmax.shared.Result;
import org.sirmax.ui.app.AppServices;
import org.sirmax.ui.app.UiSession;
import org.sirmax.ui.designsystem.Banner;
import org.sirmax.ui.designsystem.Buttons;
import org.sirmax.ui.designsystem.Cards;
import org.sirmax.ui.designsystem.FormField;
import org.sirmax.ui.designsystem.Styles;
import org.sirmax.ui.designsystem.ToastHost;
import org.sirmax.ui.designsystem.Typography;
import org.sirmax.ui.i18n.Messages;
import org.sirmax.ui.nav.RouteKey;

/**
 * The cash drawer (master prompt §20 — cash session, reconciliation).
 *
 * <p>One screen with two states: no session, so offer to open one; or a session in progress, so show
 * what it should hold and offer to close it against a count.
 *
 * <p>The close-out deliberately does <b>not</b> pre-fill the expected total into the count field.
 * Showing the answer before asking the question turns a count into a formality, and the whole point
 * of the reconciliation figure is that it is discovered, not confirmed.
 */
public final class CashView implements SirmaxView {

    /** SIRMAX ships configured for the Dominican peso; other currencies come with §37. */
    private static final String CURRENCY = "DOP";

    private final AppServices services;
    private final UiSession session;
    private final ToastHost toasts;

    private final TextField openingFloat = new TextField();
    private final TextField countedTotal = new TextField();
    private final TextField closingNotes = new TextField();
    private final Label sessionHeading = new Label();
    private final Label expectedLabel = new Label();
    private final Banner reconciliation = new Banner();
    private final VBox openBox = new VBox(10);
    private final VBox closeBox = new VBox(10);
    private final VBox root = new VBox(16);

    public CashView(AppServices services, UiSession session, ToastHost toasts) {
        this.services = services;
        this.session = session;
        this.toasts = toasts;
        build();
    }

    @Override
    public RouteKey route() {
        return RouteKey.CASH;
    }

    @Override
    public String titleKey() {
        return "nav.cash";
    }

    @Override
    public Parent node() {
        refresh();
        return root;
    }

    private void build() {
        sessionHeading.getStyleClass().add(Styles.TITLE);
        expectedLabel.getStyleClass().add(Styles.BODY);

        openingFloat.setPromptText("0.00");
        countedTotal.setPromptText("0.00");

        openBox.getChildren()
                .addAll(
                        Typography.muted("cash.open.explain"),
                        new FormField("cash.opening_float", openingFloat),
                        Buttons.primary("cash.open", this::openSession));

        closeBox.getChildren()
                .addAll(
                        expectedLabel,
                        new FormField("cash.counted_total", countedTotal, "cash.counted.hint"),
                        new FormField("cash.closing_notes", closingNotes),
                        Buttons.primary("cash.close", this::closeSession));

        root.getChildren()
                .addAll(
                        Typography.title("cash.title"),
                        sessionHeading,
                        reconciliation,
                        Cards.card(openBox),
                        Cards.card(closeBox));
    }

    /** Show whichever half of the screen matches the cashier's current state. */
    public void refresh() {
        Optional<CashSession> open =
                session.current()
                        .flatMap(s -> services.manageCashSession().openSessionFor(s.user().id()));

        boolean hasSession = open.isPresent();
        openBox.setVisible(!hasSession && session.can(Permission.CASH_SESSION_OPEN));
        openBox.setManaged(openBox.isVisible());
        closeBox.setVisible(hasSession && session.can(Permission.CASH_SESSION_CLOSE));
        closeBox.setManaged(closeBox.isVisible());

        if (hasSession) {
            CashSession cashSession = open.get();
            sessionHeading.setText(
                    Messages.get("cash.session_open", cashSession.code()));
            Money collected =
                    services.billing().cashCollectedIn(cashSession.id(), CURRENCY);
            Money refunded = services.billing().cashRefundedIn(cashSession.id(), CURRENCY);
            expectedLabel.setText(
                    Messages.get(
                            "cash.expected",
                            cashSession.openingFloat().toDecimal().toPlainString(),
                            collected.toDecimal().toPlainString(),
                            refunded.toDecimal().toPlainString()));
        } else {
            sessionHeading.setText(Messages.get("cash.no_session"));
            expectedLabel.setText("");
        }
    }

    private void openSession() {
        Optional<Money> amount = parse(openingFloat.getText());
        if (amount.isEmpty()) {
            toasts.error("cash.invalid_float");
            return;
        }
        Result<CashSession> result =
                services.manageCashSession()
                        .open(
                                new ManageCashSession.OpenCommand(
                                        session.require(),
                                        amount.get(),
                                        Optional.empty(),
                                        "desktop.cash"));
        if (result instanceof Result.Err<CashSession> err) {
            toasts.error(err.messageKey());
            return;
        }
        toasts.success("cash.opened", result.orElseThrow().code());
        openingFloat.clear();
        reconciliation.hide();
        refresh();
    }

    private void closeSession() {
        Optional<Money> counted = parse(countedTotal.getText());
        if (counted.isEmpty()) {
            toasts.error("cash.invalid_count");
            return;
        }
        Result<ManageCashSession.Closing> result =
                services.manageCashSession()
                        .close(
                                new ManageCashSession.CloseCommand(
                                        session.require(),
                                        counted.get(),
                                        Optional.ofNullable(closingNotes.getText())
                                                .filter(s -> !s.isBlank()),
                                        "desktop.cash"));
        if (result instanceof Result.Err<ManageCashSession.Closing> err) {
            toasts.error(err.messageKey());
            return;
        }

        ManageCashSession.Closing closing = result.orElseThrow();
        // A difference is reported plainly, in both directions. It is never rounded away.
        if (closing.balances()) {
            reconciliation.show(
                    Banner.Severity.SUCCESS,
                    "cash.closed_balanced",
                    null,
                    closing.counted().toDecimal().toPlainString());
        } else {
            reconciliation.show(
                    Banner.Severity.WARNING,
                    closing.difference().isNegative() ? "cash.closed_short" : "cash.closed_over",
                    "cash.difference_kept",
                    closing.difference().toDecimal().abs().toPlainString());
        }
        countedTotal.clear();
        closingNotes.clear();
        refresh();
    }

    private static Optional<Money> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Money.of(new BigDecimal(raw.strip()), java.util.Currency.getInstance(CURRENCY)));
        } catch (NumberFormatException | ArithmeticException e) {
            return Optional.empty();
        }
    }

    /** Exposed for tests: whether the screen is currently offering to close a drawer. */
    public boolean isShowingOpenSession() {
        return closeBox.isVisible();
    }
}
