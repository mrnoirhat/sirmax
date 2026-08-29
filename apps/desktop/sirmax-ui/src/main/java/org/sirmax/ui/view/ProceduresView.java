// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.view;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.beans.property.SimpleStringProperty;
import org.sirmax.application.port.ProcedureQuery;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.procedure.ProcedureStatus;
import org.sirmax.domain.security.Permission;
import org.sirmax.ui.app.AppServices;
import org.sirmax.ui.app.UiSession;
import org.sirmax.ui.designsystem.Buttons;
import org.sirmax.ui.designsystem.DataTable;
import org.sirmax.ui.designsystem.Styles;
import org.sirmax.ui.designsystem.Typography;
import org.sirmax.ui.i18n.Messages;
import org.sirmax.ui.nav.Navigator;
import org.sirmax.ui.nav.RouteKey;

/**
 * The worklist (master prompt §57): what is on this operator's plate, in the order it should be
 * dealt with.
 *
 * <p>Four saved filters cover the questions an operator actually asks — "mine", "unassigned",
 * "overdue", "everything open" — because a generic filter builder is a tool for reporting, not for
 * a counter. Rows open the case; there is no separate "view" column to hunt for.
 */
public final class ProceduresView implements SirmaxView {

    private static final int PAGE_SIZE = 100;

    /**
     * The saved queues an operator switches between, in the order they are offered.
     *
     * <p>"All open work" leads and is the default: in a small municipal office assignment is often
     * not used at all, so opening on "mine" would show an empty screen that reads as a broken app.
     */
    private enum Queue {
        ALL("procedures.queue.all"),
        MINE("procedures.queue.mine"),
        UNASSIGNED("procedures.queue.unassigned"),
        OVERDUE("procedures.queue.overdue");

        final String labelKey;

        Queue(String labelKey) {
            this.labelKey = labelKey;
        }
    }

    private final AppServices services;
    private final UiSession session;
    private final Navigator navigator;

    private final TableView<Procedure> table = new TableView<>();
    private final TextField search = new TextField();
    private final Label summary = new Label();
    private final VBox root = new VBox(16);

    private Queue queue = Queue.ALL;

    public ProceduresView(AppServices services, UiSession session, Navigator navigator) {
        this.services = services;
        this.session = session;
        this.navigator = navigator;
        build();
    }

    @Override
    public RouteKey route() {
        return RouteKey.PROCEDURES;
    }

    @Override
    public String titleKey() {
        return "nav.procedures";
    }

    @Override
    public Parent node() {
        refresh();
        return root;
    }

    private void build() {
        search.setPromptText(Messages.get("procedures.search.prompt"));
        search.setOnAction(e -> refresh());
        HBox.setHgrow(search, Priority.ALWAYS);

        HBox toolbar = new HBox(8, buildQueueToggles(), spacer(), search);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        summary.getStyleClass().add(Styles.MUTED);
        buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox header = new HBox(12, Typography.title("procedures.title"), spacer());
        if (session.can(Permission.PROCEDURE_WORK)) {
            header.getChildren()
                    .add(
                            Buttons.primary(
                                    "procedures.new", () -> navigator.navigate(RouteKey.PROCEDURE_NEW)));
        }
        header.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(header, toolbar, summary, table);
    }

    private HBox buildQueueToggles() {
        ToggleGroup group = new ToggleGroup();
        HBox box = new HBox(4);
        for (Queue q : Queue.values()) {
            ToggleButton button = new ToggleButton(Messages.get(q.labelKey));
            button.setToggleGroup(group);
            button.setSelected(q == queue);
            button.setOnAction(
                    e -> {
                        queue = q;
                        button.setSelected(true); // a toggle group must never end up with none active
                        refresh();
                    });
            box.getChildren().add(button);
        }
        return box;
    }

    private void buildTable() {
        DataTable.styled(table);

        table.getColumns().addAll(List.of(
                column("procedures.column.code", 130, Procedure::code),
                column(
                        "procedures.column.service",
                        220,
                        p -> serviceName(p.serviceDefinitionId())),
                column(
                        "procedures.column.status",
                        150,
                        p -> Messages.get("procedure.status." + p.status().name().toLowerCase(java.util.Locale.ROOT))),
                column(
                        "procedures.column.step",
                        140,
                        p -> p.currentStepKey().orElse("—")),
                column(
                        "procedures.column.due",
                        120,
                        ProceduresView::dueText)));

        table.setRowFactory(
                t -> {
                    var row = new javafx.scene.control.TableRow<Procedure>();
                    row.setOnMouseClicked(
                            e -> {
                                if (e.getClickCount() == 2 && !row.isEmpty()) {
                                    navigator.navigate(
                                            RouteKey.PROCEDURE_DETAIL, row.getItem().id());
                                }
                            });
                    return row;
                });
    }

    private TableColumn<Procedure, String> column(
            String headerKey, double width, java.util.function.Function<Procedure, String> value) {
        TableColumn<Procedure, String> col = new TableColumn<>(Messages.get(headerKey));
        col.setPrefWidth(width);
        col.setCellValueFactory(c -> new SimpleStringProperty(value.apply(c.getValue())));
        return col;
    }

    /** Overdue cases say so in words; a colour alone would not survive a monochrome print. */
    private static String dueText(Procedure p) {
        return p.dueDate()
                .map(
                        due ->
                                p.isOverdue(LocalDate.now())
                                        ? Messages.get("procedures.overdue", due.toString())
                                        : due.toString())
                .orElse("—");
    }

    private String serviceName(String serviceDefinitionId) {
        return services.serviceCatalog()
                .findDefinitionById(serviceDefinitionId)
                .map(d -> d.name())
                .orElse(serviceDefinitionId);
    }

    /** Re-run the active query. Called on every mount so the queue is never stale. */
    public void refresh() {
        ProcedureQuery query = buildQuery();
        List<Procedure> rows = services.procedures().search(query);
        table.setItems(FXCollections.observableArrayList(rows));
        summary.setText(Messages.get("common.count.results", rows.size()));
    }

    private ProcedureQuery buildQuery() {
        Optional<String> text =
                Optional.ofNullable(search.getText()).filter(s -> !s.isBlank());
        String userId = session.current().map(s -> s.user().id()).orElse(null);
        return new ProcedureQuery(
                text,
                List.of(),
                Optional.empty(),
                queue == Queue.MINE ? Optional.ofNullable(userId) : Optional.empty(),
                Optional.empty(),
                queue == Queue.OVERDUE,
                queue == Queue.UNASSIGNED,
                PAGE_SIZE,
                0);
    }

    /** Exposed for tests: how many rows the current queue holds. */
    public int rowCount() {
        return table.getItems().size();
    }

    /** Exposed for tests: the statuses currently listed. */
    public List<ProcedureStatus> listedStatuses() {
        return table.getItems().stream().map(Procedure::status).toList();
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }
}
