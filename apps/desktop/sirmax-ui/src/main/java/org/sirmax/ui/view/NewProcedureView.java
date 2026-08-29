// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.view;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.sirmax.application.usecase.FindDuplicatePeople;
import org.sirmax.application.usecase.RegisterPerson;
import org.sirmax.application.usecase.StartProcedure;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.identity.IdentificationType;
import org.sirmax.domain.identity.Person;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.service.ServiceDefinition;
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
import org.sirmax.ui.nav.Navigator;
import org.sirmax.ui.nav.RouteKey;

/**
 * "Registrar un trámite" — the one-screen wizard (master prompt §33, §34).
 *
 * <p>Two decisions, in the order the counter makes them: which service, and for whom. The citizen
 * half searches as the operator types and offers matches before it offers "create new", because
 * duplicate citizens are the failure this screen exists to prevent (§23). Creating one runs the
 * duplicate check first and shows what it found rather than silently accepting a second record.
 *
 * <p>No data model is exposed: the operator never picks a "service version" or a "party type".
 */
public final class NewProcedureView implements SirmaxView {

    private static final int MAX_MATCHES = 8;

    private final AppServices services;
    private final UiSession session;
    private final Navigator navigator;
    private final ToastHost toasts;

    private final ComboBox<ServiceDefinition> serviceBox = new ComboBox<>();
    private final TextField citizenSearch = new TextField();
    private final ListView<Person> matches = new ListView<>();
    private final TextField givenNames = new TextField();
    private final TextField familyNames = new TextField();
    private final TextField idNumber = new TextField();
    private final Banner duplicateWarning = new Banner();
    private final Label selectedCitizen = new Label();
    private final VBox root = new VBox(20);

    private Person citizen;

    public NewProcedureView(
            AppServices services, UiSession session, Navigator navigator, ToastHost toasts) {
        this.services = services;
        this.session = session;
        this.navigator = navigator;
        this.toasts = toasts;
        build();
    }

    @Override
    public RouteKey route() {
        return RouteKey.PROCEDURE_NEW;
    }

    @Override
    public String titleKey() {
        return "procedures.new";
    }

    @Override
    public Parent node() {
        reloadServices();
        return root;
    }

    private void build() {
        root.getChildren()
                .addAll(
                        Typography.title("procedures.new"),
                        buildServiceCard(),
                        buildCitizenCard(),
                        buildActions());
    }

    private VBox buildServiceCard() {
        serviceBox.setMaxWidth(Double.MAX_VALUE);
        serviceBox.setCellFactory(list -> serviceCell());
        serviceBox.setButtonCell(serviceCell());
        return Cards.card(
                Typography.subtitle("procedures.new.service"),
                new FormField("procedures.new.service.label", serviceBox));
    }

    private ListCell<ServiceDefinition> serviceCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(ServiceDefinition item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name() + "  ·  " + item.code());
            }
        };
    }

    private VBox buildCitizenCard() {
        citizenSearch.setPromptText(Messages.get("procedures.new.citizen.prompt"));
        citizenSearch.textProperty().addListener((obs, old, value) -> searchCitizens(value));

        matches.setPrefHeight(140);
        matches.setCellFactory(list -> personCell());
        matches.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, old, value) -> selectCitizen(value));

        selectedCitizen.getStyleClass().add(Styles.BODY);
        duplicateWarning.setVisible(false);
        duplicateWarning.setManaged(false);

        VBox newCitizen =
                new VBox(
                        8,
                        Typography.muted("procedures.new.citizen.create"),
                        new FormField("person.given_names", givenNames),
                        new FormField("person.family_names", familyNames),
                        new FormField("person.id_number", idNumber, "person.id_number.hint"),
                        duplicateWarning,
                        Buttons.secondary("procedures.new.citizen.register", this::registerCitizen));

        return Cards.card(
                Typography.subtitle("procedures.new.citizen"),
                new FormField("procedures.new.citizen.search", citizenSearch),
                matches,
                selectedCitizen,
                newCitizen);
    }

    private ListCell<Person> personCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Person item, boolean empty) {
                super.updateItem(item, empty);
                setText(
                        empty || item == null
                                ? null
                                : item.fullName()
                                        + item.birthDate()
                                                .map(d -> "  ·  " + d)
                                                .orElse(""));
            }
        };
    }

    private HBox buildActions() {
        HBox actions =
                new HBox(
                        8,
                        spacer(),
                        Buttons.ghost("action.cancel", () -> navigator.navigate(RouteKey.HOME)),
                        Buttons.primary("procedures.new.submit", this::start));
        actions.setAlignment(Pos.CENTER_RIGHT);
        return actions;
    }

    private void reloadServices() {
        List<ServiceDefinition> available =
                services.serviceCatalog().listDefinitions(false).stream()
                        // only services with a published version can take a case
                        .filter(d -> d.currentVersionId().isPresent())
                        .toList();
        serviceBox.setItems(FXCollections.observableArrayList(available));
        if (available.isEmpty()) {
            toasts.warning("procedures.new.no_services");
        }
    }

    private void searchCitizens(String query) {
        if (query == null || query.strip().length() < 2) {
            matches.setItems(FXCollections.emptyObservableList());
            return;
        }
        matches.setItems(
                FXCollections.observableArrayList(
                        services.people().search(query.strip(), MAX_MATCHES, 0)));
    }

    private void selectCitizen(Person person) {
        this.citizen = person;
        selectedCitizen.setText(
                person == null
                        ? ""
                        : Messages.get("procedures.new.citizen.selected", person.fullName()));
    }

    private void registerCitizen() {
        duplicateWarning.setVisible(false);
        duplicateWarning.setManaged(false);

        Optional<String> id = Optional.ofNullable(idNumber.getText()).filter(s -> !s.isBlank());
        var duplicates =
                services
                        .findDuplicatePeople()
                        .execute(
                                new FindDuplicatePeople.Command(
                                        session.require(),
                                        givenNames.getText(),
                                        familyNames.getText(),
                                        Optional.<LocalDate>empty(),
                                        id.map(x -> IdentificationType.CEDULA),
                                        id))
                        .orElseGet(List::of);

        // A conclusive hit (same cédula) is not a suggestion: that person already exists, so select
        // them instead of writing a second record.
        Optional<FindDuplicatePeople.Candidate> conclusive =
                duplicates.stream().filter(FindDuplicatePeople.Candidate::isConclusive).findFirst();
        if (conclusive.isPresent()) {
            selectCitizen(conclusive.get().person());
            warn("person.duplicate_id");
            return;
        }
        if (!duplicates.isEmpty()) {
            matches.setItems(
                    FXCollections.observableArrayList(
                            duplicates.stream().map(FindDuplicatePeople.Candidate::person).toList()));
            warn("procedures.new.citizen.possible_duplicates");
            return;
        }

        Result<Person> registered =
                services
                        .registerPerson()
                        .execute(
                                new RegisterPerson.Command(
                                        session.require(),
                                        givenNames.getText(),
                                        familyNames.getText(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        id.map(x -> IdentificationType.CEDULA),
                                        id,
                                        "desktop.new_procedure"));
        if (registered instanceof Result.Err<Person> err) {
            toasts.error(err.messageKey());
            return;
        }
        selectCitizen(registered.orElseThrow());
        toasts.success("person.registered");
    }

    /**
     * Second press on "Registrar" after a warning goes through — the operator has now seen the
     * candidates and decided this really is someone new.
     */
    private void warn(String messageKey) {
        duplicateWarning.show(Banner.Severity.WARNING, messageKey, null);
        duplicateWarning.setVisible(true);
        duplicateWarning.setManaged(true);
    }

    private void start() {
        ServiceDefinition service = serviceBox.getValue();
        if (service == null) {
            toasts.warning("procedures.new.pick_service");
            return;
        }
        if (citizen == null) {
            toasts.warning("procedures.new.pick_citizen");
            return;
        }

        Result<Procedure> opened =
                services
                        .startProcedure()
                        .execute(
                                new StartProcedure.Command(
                                        session.require(),
                                        service.id(),
                                        PartyRef.person(citizen.id()),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        "desktop.new_procedure"));
        if (opened instanceof Result.Err<Procedure> err) {
            toasts.error(err.messageKey());
            return;
        }
        Procedure procedure = opened.orElseThrow();
        toasts.success("procedures.new.opened", procedure.code());
        reset();
        navigator.navigate(RouteKey.PROCEDURE_DETAIL, procedure.id());
    }

    private void reset() {
        citizen = null;
        selectedCitizen.setText("");
        citizenSearch.clear();
        givenNames.clear();
        familyNames.clear();
        idNumber.clear();
        matches.setItems(FXCollections.emptyObservableList());
        duplicateWarning.setVisible(false);
        duplicateWarning.setManaged(false);
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }
}
