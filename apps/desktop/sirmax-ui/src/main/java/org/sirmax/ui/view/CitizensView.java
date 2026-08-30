// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.view;

import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.identity.Person;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.ui.app.AppServices;
import org.sirmax.ui.app.UiSession;
import org.sirmax.ui.designsystem.Cards;
import org.sirmax.ui.designsystem.DataTable;
import org.sirmax.ui.designsystem.Enums;
import org.sirmax.ui.designsystem.Styles;
import org.sirmax.ui.designsystem.Typography;
import org.sirmax.ui.i18n.Messages;
import org.sirmax.ui.nav.Navigator;
import org.sirmax.ui.nav.RouteKey;

/**
 * The citizen desk (master prompt §58): find a person, then see everything the municipality has
 * done with them.
 *
 * <p>Selecting a citizen fills the history panel with their cases, newest first, each one openable.
 * That is the whole screen: one search box, one list, one history — the counter's actual question is
 * "what is going on with this person", not "let me browse the person table".
 */
public final class CitizensView implements SirmaxView {

    private static final int PAGE_SIZE = 50;
    private static final int HISTORY_SIZE = 30;

    private final AppServices services;
    private final UiSession session;
    private final Navigator navigator;

    private final TextField search = new TextField();
    private final TableView<Person> results = new TableView<>();
    private final TableView<Procedure> history = new TableView<>();
    private final Label historyHeading = new Label();
    private final Label summary = new Label();
    private final VBox root = new VBox(16);

    public CitizensView(AppServices services, UiSession session, Navigator navigator) {
        this.services = services;
        this.session = session;
        this.navigator = navigator;
        build();
    }

    @Override
    public RouteKey route() {
        return RouteKey.CITIZENS;
    }

    @Override
    public String titleKey() {
        return "nav.citizens";
    }

    @Override
    public Parent node() {
        return root;
    }

    private void build() {
        search.setPromptText(Messages.get("citizens.search.prompt"));
        search.textProperty().addListener((obs, old, value) -> runSearch(value));
        HBox.setHgrow(search, Priority.ALWAYS);

        summary.getStyleClass().add(Styles.MUTED);
        historyHeading.getStyleClass().add(Styles.SUBTITLE);
        historyHeading.setText(Messages.get("citizens.history.none"));

        buildResultsTable();
        buildHistoryTable();

        root.getChildren()
                .addAll(
                        Typography.title("citizens.title"),
                        Cards.card(new HBox(8, search), summary, results),
                        Cards.card(historyHeading, history));
    }

    private void buildResultsTable() {
        DataTable.styled(results);
        results.setPrefHeight(240);
        results.getColumns()
                .addAll(
                        List.of(
                                column(results, "citizens.column.name", 280, Person::fullName),
                                column(
                                        results,
                                        "citizens.column.birthdate",
                                        140,
                                        p -> p.birthDate().map(Object::toString).orElse("—"))));
        results.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, old, person) -> showHistory(person));
    }

    private void buildHistoryTable() {
        DataTable.styled(history);
        history.setPrefHeight(260);
        history.getColumns()
                .addAll(
                        List.of(
                                column(history, "procedures.column.code", 140, Procedure::code),
                                column(
                                        history,
                                        "procedures.column.service",
                                        260,
                                        p ->
                                                services.serviceCatalog()
                                                        .findDefinitionById(p.serviceDefinitionId())
                                                        .map(d -> d.name())
                                                        .orElse("—")),
                                column(
                                        history,
                                        "procedures.column.status",
                                        160,
                                        p -> Enums.label("procedure.status", p.status()))));
        history.setRowFactory(
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

    private <T> TableColumn<T, String> column(
            TableView<T> table,
            String headerKey,
            double width,
            java.util.function.Function<T, String> value) {
        TableColumn<T, String> col = new TableColumn<>(Messages.get(headerKey));
        col.setPrefWidth(width);
        col.setCellValueFactory(c -> new SimpleStringProperty(value.apply(c.getValue())));
        return col;
    }

    /** Run the people search; short queries are ignored so the table does not thrash on one letter. */
    public void runSearch(String query) {
        if (query == null || query.strip().length() < 2) {
            results.setItems(FXCollections.emptyObservableList());
            summary.setText("");
            return;
        }
        List<Person> found = services.people().search(query.strip(), PAGE_SIZE, 0);
        results.setItems(FXCollections.observableArrayList(found));
        summary.setText(Messages.get("common.count.results", found.size()));
    }

    private void showHistory(Person person) {
        if (person == null) {
            history.setItems(FXCollections.emptyObservableList());
            historyHeading.setText(Messages.get("citizens.history.none"));
            return;
        }
        historyHeading.setText(Messages.get("citizens.history.of", person.fullName()));
        history.setItems(
                FXCollections.observableArrayList(
                        services.procedures()
                                .findByApplicant(PartyRef.person(person.id()), HISTORY_SIZE)));
    }

    /** Exposed for tests: how many people the last search returned. */
    public int resultCount() {
        return results.getItems().size();
    }

    /** Exposed for tests: select a row and load its history. */
    public void select(int index) {
        results.getSelectionModel().select(index);
    }

    /** Exposed for tests: how many cases the selected citizen has. */
    public int historyCount() {
        return history.getItems().size();
    }
}
