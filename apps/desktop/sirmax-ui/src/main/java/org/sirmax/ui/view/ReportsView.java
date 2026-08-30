// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.view;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.sirmax.application.port.ProcedureQuery;
import org.sirmax.domain.finance.Invoice;
import org.sirmax.domain.finance.InvoiceStatus;
import org.sirmax.domain.finance.Payment;
import org.sirmax.domain.finance.PaymentMethod;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.procedure.ProcedureStatus;
import org.sirmax.domain.security.Permission;
import org.sirmax.domain.service.ServiceDefinition;
import org.sirmax.shared.Money;
import org.sirmax.ui.app.AppServices;
import org.sirmax.ui.app.UiSession;
import org.sirmax.ui.designsystem.Banner;
import org.sirmax.ui.designsystem.Buttons;
import org.sirmax.ui.designsystem.Cards;
import org.sirmax.ui.designsystem.DataTable;
import org.sirmax.ui.designsystem.Enums;
import org.sirmax.ui.designsystem.FormField;
import org.sirmax.ui.designsystem.Styles;
import org.sirmax.ui.designsystem.ToastHost;
import org.sirmax.ui.designsystem.Typography;
import org.sirmax.ui.i18n.Messages;
import org.sirmax.ui.nav.RouteKey;

/**
 * Reports over a chosen period (master prompt §36).
 *
 * <p>Four questions a municipality actually asks at the end of a day, a week or a month: how much
 * came in and by what means, which services produced it, how the caseload is distributed, and how
 * much is still owed. Everything is derived from the same date range so two numbers on this screen
 * can never disagree about which period they cover.
 *
 * <p>Every table exports to CSV, because the answer to "can I have that in Excel" is always yes and
 * a report that cannot leave the application gets retyped by hand.
 *
 * <p>Amounts are summed per currency rather than into one total. An install is single-currency
 * today (§37 makes it configurable), but silently adding pesos to dollars is the kind of bug that
 * is only ever found by the person who has to explain the number.
 */
public final class ReportsView implements SirmaxView {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int SCAN_LIMIT = 5000;
    private static final ZoneId ZONE = ZoneId.systemDefault();

    /** One row of any of the summary tables: a label, a count and a money total. */
    public record Row(String label, long count, String total) {}

    private final AppServices services;
    private final UiSession session;
    private final ToastHost toasts;

    private final DatePicker from = new DatePicker();
    private final DatePicker to = new DatePicker();
    private final Label headline = new Label();
    private final Banner denied = new Banner();

    private final TableView<Row> byMethod = new TableView<>();
    private final TableView<Row> byService = new TableView<>();
    private final TableView<Row> byStatus = new TableView<>();
    private final TableView<Row> outstanding = new TableView<>();

    private final VBox root = new VBox(16);

    public ReportsView(AppServices services, UiSession session, ToastHost toasts) {
        this.services = services;
        this.session = session;
        this.toasts = toasts;
        build();
    }

    @Override
    public RouteKey route() {
        return RouteKey.REPORTS;
    }

    @Override
    public String titleKey() {
        return "nav.reports";
    }

    @Override
    public Parent node() {
        refresh();
        return root;
    }

    // ---- construction ----------------------------------------------------

    private void build() {
        headline.getStyleClass().add(Styles.TITLE);

        // Default to the current month: the period nearly every municipal report covers.
        LocalDate today = LocalDate.now();
        from.setValue(today.withDayOfMonth(1));
        to.setValue(today);

        HBox range = new HBox(12);
        range.setAlignment(Pos.CENTER_LEFT);
        range.getChildren()
                .addAll(
                        new FormField("reports.from", from),
                        new FormField("reports.to", to),
                        Buttons.primary("reports.run", this::refresh));

        styleTable(byMethod, "reports.column.method", true);
        styleTable(byService, "reports.column.service", true);
        // Cases have no money of their own — a Total column here would be a row of em-dashes
        // pretending to be data.
        styleTable(byStatus, "reports.column.status", false);
        styleTable(outstanding, "reports.column.customer", true);

        root.getChildren()
                .addAll(
                        Typography.title("reports.title"),
                        Typography.muted("reports.explain"),
                        denied,
                        Cards.card(range, headline),
                        section("reports.by_method", byMethod, "cobros-por-medio"),
                        section("reports.by_service", byService, "cobros-por-servicio"),
                        section("reports.by_status", byStatus, "tramites-por-estado"),
                        section("reports.outstanding", outstanding, "pendiente-de-cobro"));
    }

    private VBox section(String titleKey, TableView<Row> table, String fileStem) {
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getChildren().add(Buttons.secondary("reports.export", () -> exportCsv(table, fileStem)));
        return Cards.card(Typography.subtitle(titleKey), table, actions);
    }

    private void styleTable(TableView<Row> table, String labelHeaderKey, boolean withTotal) {
        DataTable.styled(table);
        table.setPrefHeight(200);
        table.getColumns()
                .addAll(
                        List.of(
                                col(labelHeaderKey, 300, Row::label),
                                col("reports.column.count", 120, r -> String.valueOf(r.count()))));
        if (withTotal) {
            table.getColumns().add(col("reports.column.total", 160, Row::total));
        }
    }

    // ---- data ------------------------------------------------------------

    /** Recompute every table for the selected range. */
    public void refresh() {
        if (!session.can(Permission.REPORT_VIEW)) {
            denied.show(Banner.Severity.WARNING, "reports.forbidden", "reports.forbidden.hint");
            clear();
            return;
        }
        denied.hide();

        LocalDate start = from.getValue();
        LocalDate end = to.getValue();
        if (start == null || end == null) {
            toasts.warning("reports.range_required");
            return;
        }
        if (end.isBefore(start)) {
            toasts.error("reports.range_inverted");
            return;
        }
        // Inclusive of the end date: a report "hasta el 31" that omits the 31st is wrong in the
        // only way anyone will notice.
        Instant fromInstant = start.atStartOfDay(ZONE).toInstant();
        Instant toInstant = end.plusDays(1).atStartOfDay(ZONE).toInstant();

        List<Invoice> invoices = invoicesIn(fromInstant, toInstant);
        List<Payment> payments = paymentsFor(invoices, fromInstant, toInstant);

        headline.setText(
                Messages.get(
                        "reports.headline",
                        DAY.format(start),
                        DAY.format(end),
                        payments.isEmpty()
                                ? Money.zero(java.util.Currency.getInstance("DOP")).toString()
                                : totalOf(payments.stream().map(Payment::amount).toList()),
                        payments.size()));

        byMethod.setItems(FXCollections.observableArrayList(collectByMethod(payments)));
        byService.setItems(FXCollections.observableArrayList(collectByService(invoices)));
        byStatus.setItems(FXCollections.observableArrayList(collectByStatus(fromInstant, toInstant)));
        outstanding.setItems(FXCollections.observableArrayList(collectOutstanding(invoices)));
    }

    private void clear() {
        headline.setText("");
        for (TableView<Row> table : List.of(byMethod, byService, byStatus, outstanding)) {
            table.setItems(FXCollections.emptyObservableList());
        }
    }

    private List<Invoice> invoicesIn(Instant fromInstant, Instant toInstant) {
        return services.billing().listInvoices(List.of(InvoiceStatus.values()), SCAN_LIMIT, 0).stream()
                .filter(i -> i.issuedAt().map(t -> within(t, fromInstant, toInstant)).orElse(false))
                .toList();
    }

    /**
     * Payments are read per invoice rather than scanned globally: the repository indexes them that
     * way, and a payment always belongs to an invoice, so nothing is missed.
     */
    private List<Payment> paymentsFor(List<Invoice> invoices, Instant fromInstant, Instant toInstant) {
        List<Payment> out = new ArrayList<>();
        for (Invoice invoice : invoices) {
            for (Payment payment : services.billing().findPaymentsByInvoice(invoice.id())) {
                if (within(payment.receivedAt(), fromInstant, toInstant)) {
                    out.add(payment);
                }
            }
        }
        return out;
    }

    private List<Row> collectByMethod(List<Payment> payments) {
        Map<PaymentMethod, List<Money>> grouped = new LinkedHashMap<>();
        for (PaymentMethod method : PaymentMethod.values()) {
            grouped.put(method, new ArrayList<>());
        }
        payments.forEach(p -> grouped.get(p.method()).add(p.amount()));

        List<Row> rows = new ArrayList<>();
        grouped.forEach(
                (method, amounts) -> {
                    if (!amounts.isEmpty()) {
                        rows.add(
                                new Row(
                                        Enums.label("payment.method", method),
                                        amounts.size(),
                                        totalOf(amounts)));
                    }
                });
        return rows;
    }

    private List<Row> collectByService(List<Invoice> invoices) {
        Map<String, List<Money>> grouped = new LinkedHashMap<>();
        for (Invoice invoice : invoices) {
            String key = invoice.serviceDefinitionId().orElse("");
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(invoice.total());
        }
        List<Row> rows = new ArrayList<>();
        grouped.forEach(
                (id, amounts) ->
                        rows.add(new Row(serviceName(id), amounts.size(), totalOf(amounts))));
        rows.sort((a, b) -> Long.compare(b.count(), a.count()));
        return rows;
    }

    private List<Row> collectByStatus(Instant fromInstant, Instant toInstant) {
        List<Row> rows = new ArrayList<>();
        for (ProcedureStatus status : ProcedureStatus.values()) {
            List<Procedure> found =
                    services.procedures()
                            .search(
                                    new ProcedureQuery(
                                            Optional.empty(),
                                            List.of(status),
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            false,
                                            false,
                                            SCAN_LIMIT,
                                            0))
                            .stream()
                            .filter(p -> within(p.openedAt(), fromInstant, toInstant))
                            .toList();
            if (!found.isEmpty()) {
                rows.add(new Row(Enums.label("procedure.status", status), found.size(), "—"));
            }
        }
        return rows;
    }

    private List<Row> collectOutstanding(List<Invoice> invoices) {
        List<Row> rows = new ArrayList<>();
        for (Invoice invoice : invoices) {
            if (invoice.balance().isPositive()) {
                rows.add(
                        new Row(
                                invoice.customerName()
                                        + " · "
                                        + invoice.number().orElse(Messages.get("reports.no_number")),
                                1,
                                invoice.balance().toString()));
            }
        }
        rows.sort((a, b) -> a.label().compareToIgnoreCase(b.label()));
        return rows;
    }

    // ---- export ----------------------------------------------------------

    private void exportCsv(TableView<Row> table, String fileStem) {
        if (table.getItems().isEmpty()) {
            toasts.warning("reports.nothing_to_export");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Messages.get("reports.export"));
        chooser.setInitialFileName(fileStem + "-" + LocalDate.now() + ".csv");
        chooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter(Messages.get("reports.csv"), "*.csv"));
        java.io.File target =
                chooser.showSaveDialog(root.getScene() == null ? null : root.getScene().getWindow());
        if (target == null) {
            return;
        }

        StringBuilder csv = new StringBuilder();
        csv.append(Messages.get("reports.column.count")).append(';');
        csv.append(Messages.get("reports.column.total")).append(';');
        csv.append(Messages.get("reports.column.service")).append('\n');
        for (Row row : table.getItems()) {
            csv.append(row.count()).append(';');
            csv.append(escape(row.total())).append(';');
            csv.append(escape(row.label())).append('\n');
        }

        try {
            // UTF-8 with a BOM: without it Excel on a Spanish Windows opens the file in the system
            // code page and every "Facturación" becomes mojibake.
            Files.write(
                    Path.of(target.toURI()),
                    ("﻿" + csv).getBytes(StandardCharsets.UTF_8));
            toasts.success("reports.exported", target.getName());
        } catch (IOException e) {
            toasts.error("reports.export_failed");
        }
    }

    /** Semicolons separate, so a value containing one is quoted; embedded quotes are doubled. */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    // ---- helpers ---------------------------------------------------------

    private static boolean within(Instant at, Instant fromInstant, Instant toInstant) {
        return !at.isBefore(fromInstant) && at.isBefore(toInstant);
    }

    /**
     * Sums per currency and joins the results. A single-currency install reads as one figure; a
     * mixed one reads as two, which is the honest answer rather than a wrong single number.
     */
    private static String totalOf(List<Money> amounts) {
        if (amounts.isEmpty()) {
            return "—";
        }
        Map<java.util.Currency, Money> sums = new LinkedHashMap<>();
        for (Money amount : amounts) {
            sums.merge(amount.currency(), amount, Money::plus);
        }
        return sums.values().stream().map(Money::toString).reduce((a, b) -> a + " · " + b).orElse("—");
    }

    private String serviceName(String serviceDefinitionId) {
        if (serviceDefinitionId.isEmpty()) {
            return Messages.get("reports.no_service");
        }
        return services.serviceCatalog()
                .findDefinitionById(serviceDefinitionId)
                .map(ServiceDefinition::name)
                .orElseGet(() -> Messages.get("reports.no_service"));
    }

    private TableColumn<Row, String> col(
            String headerKey, double width, java.util.function.Function<Row, String> value) {
        TableColumn<Row, String> c = new TableColumn<>(Messages.get(headerKey));
        c.setPrefWidth(width);
        c.setCellValueFactory(x -> new SimpleStringProperty(value.apply(x.getValue())));
        return c;
    }

    /** Exposed for tests: the collections-by-method rows currently on screen. */
    public List<Row> methodRows() {
        return List.copyOf(byMethod.getItems());
    }
}
