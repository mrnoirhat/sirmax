// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.view;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.sirmax.application.usecase.IssueDocument;
import org.sirmax.application.usecase.PrintDocument;
import org.sirmax.application.usecase.RefundPayment;
import org.sirmax.application.usecase.RegisterPayment;
import org.sirmax.application.usecase.VoidInvoice;
import org.sirmax.domain.finance.Invoice;
import org.sirmax.domain.finance.InvoiceStatus;
import org.sirmax.domain.finance.Payment;
import org.sirmax.domain.document.DocumentKind;
import org.sirmax.domain.document.IssuedDocument;
import org.sirmax.domain.document.PaperFormat;
import org.sirmax.domain.finance.PaymentMethod;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Money;
import org.sirmax.shared.Result;
import org.sirmax.ui.app.AppServices;
import org.sirmax.ui.app.UiSession;
import org.sirmax.ui.designsystem.Banner;
import org.sirmax.ui.designsystem.Buttons;
import org.sirmax.ui.designsystem.Cards;
import org.sirmax.ui.designsystem.DataTable;
import org.sirmax.ui.designsystem.FormField;
import org.sirmax.ui.designsystem.Styles;
import org.sirmax.ui.designsystem.ToastHost;
import org.sirmax.ui.designsystem.Typography;
import org.sirmax.ui.i18n.Messages;
import org.sirmax.ui.nav.Navigator;
import org.sirmax.ui.nav.RouteKey;

/**
 * Invoices and the counter's collection screen (master prompt §59G).
 *
 * <p>The layout follows the transaction: pick the invoice, see what is owed, take the money. The
 * amount defaults to the outstanding balance because that is what happens ninety-nine times out of a
 * hundred; a partial payment is a deliberate edit, not the default state of the form.
 *
 * <p>Change owed back is shown as its own line after a cash payment. A cashier who has to compute it
 * mentally will eventually get it wrong.
 */
public final class BillingView implements SirmaxView {

    private static final int PAGE_SIZE = 100;

    private final AppServices services;
    private final UiSession session;
    private final Navigator navigator;
    private final ToastHost toasts;

    private final TableView<Invoice> invoices = new TableView<>();
    private final TableView<Payment> payments = new TableView<>();
    private final ComboBox<PaymentMethod> method = new ComboBox<>();
    private final TextField amount = new TextField();
    private final TextField tendered = new TextField();
    private final TextField reference = new TextField();
    private final Label balanceLabel = new Label();
    private final Banner outcome = new Banner();
    private final VBox collectBox = new VBox(10);
    private final VBox root = new VBox(16);

    public BillingView(
            AppServices services, UiSession session, Navigator navigator, ToastHost toasts) {
        this.services = services;
        this.session = session;
        this.navigator = navigator;
        this.toasts = toasts;
        build();
    }

    @Override
    public RouteKey route() {
        return RouteKey.BILLING;
    }

    @Override
    public String titleKey() {
        return "nav.billing";
    }

    @Override
    public Parent node() {
        refresh();
        return root;
    }

    private void build() {
        buildInvoiceTable();
        buildPaymentTable();
        buildCollectForm();

        balanceLabel.getStyleClass().add(Styles.TITLE);

        root.getChildren()
                .addAll(
                        Typography.title("billing.title"),
                        Cards.card(Typography.subtitle("billing.invoices"), invoices),
                        outcome,
                        Cards.card(
                                Typography.subtitle("billing.collect"),
                                balanceLabel,
                                collectBox,
                                Typography.subtitle("billing.payments"),
                                payments));
    }

    private void buildInvoiceTable() {
        DataTable.styled(invoices);
        invoices.setPrefHeight(260);
        invoices.getColumns()
                .addAll(
                        List.of(
                                column("billing.column.number", 150, i -> i.number().orElse("—")),
                                column("billing.column.customer", 220, Invoice::customerName),
                                column(
                                        "billing.column.status",
                                        140,
                                        i ->
                                                Messages.get(
                                                        "invoice.status."
                                                                + i.status()
                                                                        .name()
                                                                        .toLowerCase(Locale.ROOT))),
                                column("billing.column.total", 120, i -> i.total().toString()),
                                column("billing.column.balance", 120, i -> i.balance().toString())));
        invoices.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, old, invoice) -> showInvoice(invoice));
    }

    private void buildPaymentTable() {
        DataTable.styled(payments);
        payments.setPrefHeight(160);
        payments.getColumns()
                .addAll(
                        List.of(
                                paymentColumn("billing.column.receipt", 150, Payment::code),
                                paymentColumn(
                                        "billing.column.method",
                                        140,
                                        p ->
                                                Messages.get(
                                                        "payment.method."
                                                                + p.method()
                                                                        .name()
                                                                        .toLowerCase(Locale.ROOT))),
                                paymentColumn(
                                        "billing.column.amount", 120, p -> p.amount().toString()),
                                paymentColumn(
                                        "billing.column.payment_status",
                                        120,
                                        p ->
                                                Messages.get(
                                                        "payment.status."
                                                                + p.status()
                                                                        .name()
                                                                        .toLowerCase(Locale.ROOT)))));
    }

    private void buildCollectForm() {
        method.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        method.setValue(PaymentMethod.CASH);
        method.setCellFactory(list -> methodCell());
        method.setButtonCell(methodCell());
        // Only cash has change to give, and only non-cash needs a reference number.
        method.valueProperty().addListener((obs, old, value) -> updateMethodFields(value));

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        if (session.can(Permission.PAYMENT_REGISTER)) {
            actions.getChildren().add(Buttons.primary("billing.collect.submit", this::collect));
        }
        if (session.can(Permission.PAYMENT_REFUND)) {
            actions.getChildren().add(Buttons.secondary("billing.refund", this::refund));
        }
        if (session.can(Permission.INVOICE_ISSUE)) {
            actions.getChildren()
                    .addAll(
                            Buttons.secondary(
                                    "billing.print_receipt",
                                    () -> issueAndPrint(DocumentKind.RECEIPT, PaperFormat.NARROW_80)),
                            Buttons.secondary(
                                    "billing.print_invoice",
                                    () -> issueAndPrint(DocumentKind.INVOICE, PaperFormat.LETTER)));
        }
        if (session.can(Permission.INVOICE_VOID)) {
            actions.getChildren().add(Buttons.danger("billing.void", this::voidInvoice));
        }

        collectBox.getChildren()
                .addAll(
                        new FormField("billing.method", method),
                        new FormField("billing.amount", amount, "billing.amount.hint"),
                        new FormField("billing.tendered", tendered, "billing.tendered.hint"),
                        new FormField("billing.reference", reference),
                        actions);
        updateMethodFields(PaymentMethod.CASH);
    }

    private javafx.scene.control.ListCell<PaymentMethod> methodCell() {
        return new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(PaymentMethod item, boolean empty) {
                super.updateItem(item, empty);
                setText(
                        empty || item == null
                                ? null
                                : Messages.get(
                                        "payment.method." + item.name().toLowerCase(Locale.ROOT)));
            }
        };
    }

    private void updateMethodFields(PaymentMethod value) {
        boolean cash = value != null && value.affectsCashDrawer();
        tendered.getParent().setVisible(cash);
        tendered.getParent().setManaged(cash);
        reference.getParent().setVisible(!cash);
        reference.getParent().setManaged(!cash);
    }

    private TableColumn<Invoice, String> column(
            String headerKey, double width, java.util.function.Function<Invoice, String> value) {
        TableColumn<Invoice, String> col = new TableColumn<>(Messages.get(headerKey));
        col.setPrefWidth(width);
        col.setCellValueFactory(c -> new SimpleStringProperty(value.apply(c.getValue())));
        return col;
    }

    private TableColumn<Payment, String> paymentColumn(
            String headerKey, double width, java.util.function.Function<Payment, String> value) {
        TableColumn<Payment, String> col = new TableColumn<>(Messages.get(headerKey));
        col.setPrefWidth(width);
        col.setCellValueFactory(c -> new SimpleStringProperty(value.apply(c.getValue())));
        return col;
    }

    /** Reload the unpaid invoices; a settled one drops off the counter's list. */
    public void refresh() {
        List<Invoice> open =
                services.billing()
                        .listInvoices(
                                List.of(
                                        InvoiceStatus.ISSUED,
                                        InvoiceStatus.PARTIALLY_PAID,
                                        InvoiceStatus.PAID),
                                PAGE_SIZE,
                                0);
        invoices.setItems(FXCollections.observableArrayList(open));
    }

    private void showInvoice(Invoice invoice) {
        if (invoice == null) {
            balanceLabel.setText("");
            payments.setItems(FXCollections.emptyObservableList());
            collectBox.setDisable(true);
            return;
        }
        collectBox.setDisable(invoice.isSettled());
        balanceLabel.setText(
                Messages.get("billing.balance", invoice.balance().toDecimal().toPlainString()));
        // Default to the full balance: paying it off is the overwhelmingly common case.
        amount.setText(invoice.balance().toDecimal().toPlainString());
        tendered.clear();
        reference.clear();
        payments.setItems(
                FXCollections.observableArrayList(
                        services.billing().findPaymentsByInvoice(invoice.id())));
    }

    private void collect() {
        Invoice invoice = selected();
        if (invoice == null) {
            return;
        }
        Optional<Money> value = parse(amount.getText(), invoice);
        if (value.isEmpty()) {
            toasts.error("payment.invalid_amount");
            return;
        }

        Result<RegisterPayment.Receipt> result =
                services.registerPayment()
                        .execute(
                                new RegisterPayment.Command(
                                        session.require(),
                                        invoice.id(),
                                        method.getValue(),
                                        value.get(),
                                        parse(tendered.getText(), invoice),
                                        text(reference),
                                        Optional.empty(),
                                        "desktop.billing"));
        if (result instanceof Result.Err<RegisterPayment.Receipt> err) {
            toasts.error(err.messageKey());
            return;
        }

        RegisterPayment.Receipt receipt = result.orElseThrow();
        if (receipt.change().isPositive()) {
            outcome.show(
                    Banner.Severity.INFO,
                    "billing.change_due",
                    null,
                    receipt.change().toDecimal().toPlainString());
        } else {
            outcome.show(Banner.Severity.SUCCESS, "billing.collected", null);
        }
        toasts.success("billing.receipt", receipt.payment().code());
        refresh();
    }

    /**
     * Issue the document and print it in one press (§59A.7, §59D). The operator thinks "imprimir
     * recibo", not "issue a document, then send it to a printer".
     */
    private void issueAndPrint(DocumentKind kind, PaperFormat format) {
        Invoice invoice = selected();
        if (invoice == null) {
            return;
        }
        Result<IssuedDocument> issued =
                services.issueDocument()
                        .execute(
                                new IssueDocument.Command(
                                        session.require(),
                                        invoice.id(),
                                        kind,
                                        format,
                                        Optional.empty(),
                                        "desktop.billing"));
        if (issued instanceof Result.Err<IssuedDocument> err) {
            toasts.error(err.messageKey());
            return;
        }

        Result<PrintDocument.Outcome> printed =
                services.printDocument()
                        .execute(
                                new PrintDocument.Command(
                                        session.require(),
                                        issued.orElseThrow().id(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        workstation(),
                                        "desktop.billing"));
        if (printed instanceof Result.Err<PrintDocument.Outcome> err) {
            // The document exists and keeps its number; only the paper failed. Say exactly that,
            // so the operator does not press Imprimir again expecting a fresh document.
            toasts.error(err.messageKey());
            return;
        }
        if (printed.orElseThrow().sentToPrinter()) {
            toasts.success("billing.printed", issued.orElseThrow().documentNumber());
        } else {
            toasts.info("billing.print_cancelled", issued.orElseThrow().documentNumber());
        }
    }

    /** Printer profiles can be per-machine, so the view has to say which machine it is (§59D). */
    private static String workstation() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (java.net.UnknownHostException e) {
            return "";
        }
    }

    private void refund() {
        Payment payment = payments.getSelectionModel().getSelectedItem();
        if (payment == null) {
            toasts.warning("billing.pick_payment");
            return;
        }
        prompt("billing.refund", "procedure.reason")
                .ifPresent(
                        reason -> {
                            Result<?> result =
                                    services.refundPayment()
                                            .execute(
                                                    new RefundPayment.Command(
                                                            session.require(),
                                                            payment.id(),
                                                            Optional.empty(),
                                                            reason,
                                                            "desktop.billing"));
                            report(result, "billing.refunded");
                        });
    }

    private void voidInvoice() {
        Invoice invoice = selected();
        if (invoice == null) {
            return;
        }
        prompt("billing.void", "procedure.reason")
                .ifPresent(
                        reason -> {
                            Result<?> result =
                                    services.voidInvoice()
                                            .execute(
                                                    new VoidInvoice.Command(
                                                            session.require(),
                                                            invoice.id(),
                                                            reason,
                                                            "desktop.billing"));
                            report(result, "billing.voided");
                        });
    }

    private void report(Result<?> result, String successKey) {
        if (result instanceof Result.Err<?> err) {
            toasts.error(err.messageKey());
        } else {
            toasts.success(successKey);
        }
        refresh();
    }

    private Optional<String> prompt(String titleKey, String contentKey) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(Messages.get(titleKey));
        dialog.setContentText(Messages.get(contentKey));
        return dialog.showAndWait().filter(s -> !s.isBlank());
    }

    private Invoice selected() {
        Invoice invoice = invoices.getSelectionModel().getSelectedItem();
        if (invoice == null) {
            toasts.warning("billing.pick_invoice");
        }
        return invoice;
    }

    private static Optional<Money> parse(String raw, Invoice invoice) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(
                    Money.of(new java.math.BigDecimal(raw.strip()), invoice.currency()));
        } catch (NumberFormatException | ArithmeticException e) {
            return Optional.empty();
        }
    }

    private static Optional<String> text(TextField field) {
        return Optional.ofNullable(field.getText()).filter(s -> !s.isBlank());
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    /** Exposed for tests: how many invoices the counter list holds. */
    public int invoiceCount() {
        return invoices.getItems().size();
    }
}
