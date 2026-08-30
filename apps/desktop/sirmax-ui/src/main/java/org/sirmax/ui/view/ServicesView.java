// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.view;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.sirmax.application.usecase.ConfigureServiceDraft;
import org.sirmax.application.usecase.CreateServiceDraft;
import org.sirmax.application.usecase.CreateServiceDraftVersion;
import org.sirmax.application.usecase.PublishServiceVersion;
import org.sirmax.application.usecase.SeedServiceCatalog;
import org.sirmax.application.usecase.SetServiceAvailability;
import org.sirmax.domain.finance.ChargeType;
import org.sirmax.domain.finance.FeeRule;
import org.sirmax.domain.security.Permission;
import org.sirmax.domain.service.RequirementDef;
import org.sirmax.domain.service.RequirementKind;
import org.sirmax.domain.service.RequirementStage;
import org.sirmax.domain.service.ServiceCategory;
import org.sirmax.domain.service.ServiceDefinition;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.domain.service.ServiceType;
import org.sirmax.domain.service.Sla;
import org.sirmax.domain.service.Validity;
import org.sirmax.shared.Money;
import org.sirmax.shared.Result;
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
 * Authoring the service catalogue (master prompt §22, §54–§55).
 *
 * <p>The screen is built around the one rule that makes the rest of the system trustworthy: a
 * published service version is immutable, because every procedure pins the version it was started
 * under (§39). So the editor is only ever enabled for a draft. Changing a live service means
 * "nueva versión" — a fresh draft, edited and published — and the cases already in flight keep the
 * terms the citizen was told when they applied.
 *
 * <p>That is also why the fee editor covers a fixed amount and nothing else. It is what almost
 * every Dominican tasa is, and the per-unit, tiered and area-based rules are configuration a
 * municipality imports rather than types at a counter. A half-built rule builder would invite
 * someone to publish a fee that does not compute what they meant, which is worse than not offering
 * one.
 */
public final class ServicesView implements SirmaxView {

    /** SIRMAX ships configured for the Dominican peso; other currencies come with §37. */
    private static final String CURRENCY = "DOP";

    private final AppServices services;
    private final UiSession session;
    private final ToastHost toasts;

    private final TableView<ServiceDefinition> definitions = new TableView<>();
    private final TableView<ServiceDefinitionVersion> versions = new TableView<>();

    // ── new service ──
    private final TextField newCode = new TextField();
    private final TextField newName = new TextField();
    private final ComboBox<ServiceCategory> newCategory = new ComboBox<>();
    private final ComboBox<ServiceType> newType = new ComboBox<>();
    private final VBox createBox = new VBox(10);

    // ── draft editor ──
    private final CheckBox requiresPayment = new CheckBox();
    private final TextField slaDays = new TextField();
    private final ComboBox<Sla.Basis> slaBasis = new ComboBox<>();
    private final TextField validityDays = new TextField();
    private final CheckBox renewable = new CheckBox();
    private final TextField feeConcept = new TextField();
    private final ComboBox<ChargeType> feeChargeType = new ComboBox<>();
    private final TextField feeAmount = new TextField();
    private final TextField requirementsText = new TextField();
    private final TextField notes = new TextField();
    private final Label versionHeading = new Label();
    private final VBox editorBox = new VBox(10);
    private final Banner status = new Banner();

    private final VBox root = new VBox(16);

    public ServicesView(AppServices services, UiSession session, ToastHost toasts) {
        this.services = services;
        this.session = session;
        this.toasts = toasts;
        build();
    }

    @Override
    public RouteKey route() {
        return RouteKey.SERVICES;
    }

    @Override
    public String titleKey() {
        return "nav.services";
    }

    @Override
    public Parent node() {
        refresh();
        return root;
    }

    // ---- construction ----------------------------------------------------

    private void build() {
        buildDefinitionTable();
        buildVersionTable();
        buildCreateForm();
        buildEditor();

        versionHeading.getStyleClass().add(Styles.SUBTITLE);
        versionHeading.setText(Messages.get("services.pick"));

        HBox catalogActions = new HBox(8);
        catalogActions.setAlignment(Pos.CENTER_LEFT);
        if (session.can(Permission.SERVICE_CONFIGURE)) {
            catalogActions
                    .getChildren()
                    .addAll(
                            Buttons.secondary("services.new_version", this::newDraftVersion),
                            Buttons.secondary("services.toggle_active", this::toggleAvailability),
                            Buttons.secondary("services.seed", this::seedCatalog));
        }

        root.getChildren()
                .addAll(
                        Typography.title("services.title"),
                        Typography.muted("services.explain"),
                        status,
                        Cards.card(
                                Typography.subtitle("services.catalog"),
                                definitions,
                                catalogActions),
                        Cards.card(Typography.subtitle("services.create"), createBox),
                        Cards.card(versionHeading, versions, editorBox));
    }

    private void buildDefinitionTable() {
        DataTable.styled(definitions);
        definitions.setPrefHeight(240);
        definitions
                .getColumns()
                .addAll(
                        List.of(
                                col("services.column.code", 120, ServiceDefinition::code),
                                col("services.column.name", 280, ServiceDefinition::name),
                                col("services.column.category", 180, this::categoryName),
                                col(
                                        "services.column.type",
                                        140,
                                        d -> Enums.label("service.type", d.serviceType())),
                                col(
                                        "services.column.available",
                                        110,
                                        d ->
                                                Messages.get(
                                                        d.isAvailable()
                                                                ? "services.available"
                                                                : "services.unavailable"))));
        definitions
                .getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, old, definition) -> showDefinition(definition));
    }

    private void buildVersionTable() {
        DataTable.styled(versions);
        versions.setPrefHeight(160);
        versions
                .getColumns()
                .addAll(
                        List.of(
                                versionCol(
                                        "services.column.version",
                                        100,
                                        v -> "v" + v.versionNumber()),
                                versionCol(
                                        "services.column.status",
                                        140,
                                        v -> Enums.label("service.status", v.status())),
                                versionCol(
                                        "services.column.requires_payment",
                                        130,
                                        v ->
                                                Messages.get(
                                                        v.requiresPayment()
                                                                ? "common.yes"
                                                                : "common.no")),
                                versionCol(
                                        "services.column.sla",
                                        120,
                                        v ->
                                                v.sla().isDefined()
                                                        ? Messages.get(
                                                                "services.sla_days",
                                                                v.sla().targetDays())
                                                        : "—"),
                                versionCol(
                                        "services.column.requirements",
                                        140,
                                        v -> String.valueOf(v.requirements().size()))));
        versions
                .getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, old, version) -> showVersion(version));
    }

    private void buildCreateForm() {
        newCategory.setCellFactory(list -> categoryCell());
        newCategory.setButtonCell(categoryCell());
        newType.setItems(FXCollections.observableArrayList(ServiceType.values()));
        newType.setValue(ServiceType.CON_TASA);
        newType.setCellFactory(list -> Enums.cell("service.type"));
        newType.setButtonCell(Enums.cell("service.type"));

        createBox
                .getChildren()
                .addAll(
                        Typography.muted("services.create.explain"),
                        new FormField("services.code", newCode, "services.code.hint"),
                        new FormField("services.name", newName),
                        new FormField("services.category", newCategory),
                        new FormField("services.type", newType),
                        Buttons.primary("services.create.submit", this::createService));
        createBox.setVisible(session.can(Permission.SERVICE_CONFIGURE));
        createBox.setManaged(createBox.isVisible());
    }

    private void buildEditor() {
        requiresPayment.setText(Messages.get("services.requires_payment"));
        renewable.setText(Messages.get("services.renewable"));
        slaBasis.setItems(FXCollections.observableArrayList(Sla.Basis.values()));
        slaBasis.setValue(Sla.Basis.BUSINESS_DAYS);
        slaBasis.setCellFactory(list -> Enums.cell("services.sla_basis"));
        slaBasis.setButtonCell(Enums.cell("services.sla_basis"));
        feeChargeType.setItems(FXCollections.observableArrayList(ChargeType.values()));
        feeChargeType.setValue(ChargeType.TASA);
        feeChargeType.setCellFactory(list -> Enums.cell("charge.type"));
        feeChargeType.setButtonCell(Enums.cell("charge.type"));
        slaDays.setPromptText("0");
        validityDays.setPromptText("0");
        feeAmount.setPromptText("0.00");

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getChildren()
                .addAll(
                        Buttons.primary("services.save_draft", this::saveDraft),
                        Buttons.secondary("services.publish", this::publish));

        editorBox
                .getChildren()
                .addAll(
                        Typography.muted("services.editor.explain"),
                        requiresPayment,
                        new FormField(
                                "services.requirements",
                                requirementsText,
                                "services.requirements.hint"),
                        new FormField("services.sla", slaDays, "services.sla.hint"),
                        new FormField("services.sla_basis", slaBasis),
                        new FormField("services.validity", validityDays, "services.validity.hint"),
                        renewable,
                        new FormField("services.fee_concept", feeConcept),
                        new FormField("services.fee_charge_type", feeChargeType),
                        new FormField("services.fee_amount", feeAmount, "services.fee_amount.hint"),
                        new FormField("services.notes", notes),
                        actions);
        editorBox.setDisable(true);
    }

    // ---- data ------------------------------------------------------------

    /** Reload the catalogue, keeping the selected service selected where it still exists. */
    public void refresh() {
        String selectedId =
                Optional.ofNullable(definitions.getSelectionModel().getSelectedItem())
                        .map(ServiceDefinition::id)
                        .orElse(null);

        newCategory.setItems(
                FXCollections.observableArrayList(services.serviceCatalog().listActiveCategories()));
        List<ServiceDefinition> all = services.serviceCatalog().listDefinitions(false);
        definitions.setItems(FXCollections.observableArrayList(all));

        if (all.isEmpty()) {
            status.show(Banner.Severity.INFO, "services.empty", "services.empty.hint");
        } else {
            status.hide();
        }
        // Keep the operator where they were; otherwise land on the first service. An unselected
        // list leaves the whole editor below it disabled with nothing saying why.
        all.stream()
                .filter(d -> d.id().equals(selectedId))
                .findFirst()
                .ifPresentOrElse(
                        d -> definitions.getSelectionModel().select(d),
                        () -> definitions.getSelectionModel().selectFirst());
    }

    private void showDefinition(ServiceDefinition definition) {
        if (definition == null) {
            versionHeading.setText(Messages.get("services.pick"));
            versions.setItems(FXCollections.emptyObservableList());
            editorBox.setDisable(true);
            return;
        }
        versionHeading.setText(Messages.get("services.versions_of", definition.name()));
        List<ServiceDefinitionVersion> list =
                services.serviceCatalog().listVersions(definition.id());
        versions.setItems(FXCollections.observableArrayList(list));
        // Land on the draft when there is one: it is the only thing that can be edited, and the
        // reason anyone opened this screen.
        list.stream()
                .filter(v -> v.status().isEditable())
                .findFirst()
                .ifPresentOrElse(
                        v -> versions.getSelectionModel().select(v),
                        () -> versions.getSelectionModel().selectLast());
    }

    private void showVersion(ServiceDefinitionVersion version) {
        boolean editable =
                version != null
                        && version.status().isEditable()
                        && session.can(Permission.SERVICE_CONFIGURE);
        editorBox.setDisable(!editable);
        if (version == null) {
            return;
        }

        requiresPayment.setSelected(version.requiresPayment());
        slaDays.setText(version.sla().isDefined() ? String.valueOf(version.sla().targetDays()) : "");
        slaBasis.setValue(version.sla().basis());
        validityDays.setText(
                version.validity().validForDays().isPresent()
                        ? String.valueOf(version.validity().validForDays().getAsInt())
                        : "");
        renewable.setSelected(version.validity().renewable());
        requirementsText.setText(
                String.join(
                        ", ", version.requirements().stream().map(RequirementDef::label).toList()));
        notes.setText(version.notes().orElse(""));

        version.feeRules().stream()
                .findFirst()
                .ifPresentOrElse(
                        rule -> {
                            feeConcept.setText(rule.concept());
                            feeChargeType.setValue(rule.chargeType());
                            feeAmount.setText(
                                    new Money(rule.amountMinor(), currency())
                                            .toDecimal()
                                            .toPlainString());
                        },
                        () -> {
                            feeConcept.clear();
                            feeAmount.clear();
                        });

        if (!version.status().isEditable()) {
            status.show(
                    Banner.Severity.INFO,
                    "services.published_locked",
                    "services.published_locked.hint");
        } else {
            status.hide();
        }
    }

    // ---- actions ---------------------------------------------------------

    private void createService() {
        if (isBlank(newCode)) {
            toasts.error("services.code_required");
            return;
        }
        if (isBlank(newName)) {
            toasts.error("services.name_required");
            return;
        }
        ServiceCategory category = newCategory.getValue();
        if (category == null) {
            toasts.error("services.category_required");
            return;
        }

        Result<CreateServiceDraft.Created> result =
                services.createServiceDraft()
                        .execute(
                                new CreateServiceDraft.Command(
                                        session.require(),
                                        newCode.getText().strip(),
                                        category.id(),
                                        newName.getText().strip(),
                                        newType.getValue(),
                                        "DO",
                                        "desktop.services"));
        if (result instanceof Result.Err<CreateServiceDraft.Created> err) {
            toasts.error(err.messageKey());
            return;
        }

        toasts.success("services.created", newName.getText().strip());
        String definitionId = result.orElseThrow().definitionId();
        newCode.clear();
        newName.clear();
        refresh();
        // Drop the operator straight into the draft they just made. Creating a service and then
        // hunting for it in the list is two steps where one was meant.
        definitions.getItems().stream()
                .filter(d -> d.id().equals(definitionId))
                .findFirst()
                .ifPresent(d -> definitions.getSelectionModel().select(d));
    }

    private void saveDraft() {
        ServiceDefinitionVersion version = selectedDraft();
        if (version == null) {
            return;
        }

        Optional<List<FeeRule>> fees = feeRules(version);
        if (fees == null) { // parse failure; the toast is already up
            return;
        }

        Result<ServiceDefinitionVersion> result =
                services.configureServiceDraft()
                        .execute(
                                new ConfigureServiceDraft.Command(
                                        session.require(),
                                        version.id(),
                                        Optional.of(requirements()),
                                        Optional.of(requiresPayment.isSelected()),
                                        Optional.of(sla()),
                                        Optional.of(validity()),
                                        Optional.empty(),
                                        Optional.empty(),
                                        fees,
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        text(notes),
                                        "desktop.services"));
        report(result, "services.saved");
    }

    private void publish() {
        ServiceDefinitionVersion version = selectedDraft();
        if (version == null) {
            return;
        }
        Result<ServiceDefinitionVersion> result =
                services.publishServiceVersion()
                        .execute(
                                new PublishServiceVersion.Command(
                                        session.require(), version.id(), "desktop.services"));
        report(result, "services.published");
    }

    private void newDraftVersion() {
        ServiceDefinition definition = selectedDefinition();
        if (definition == null) {
            return;
        }
        Result<ServiceDefinitionVersion> result =
                services.createServiceDraftVersion()
                        .execute(
                                new CreateServiceDraftVersion.Command(
                                        session.require(), definition.id(), "desktop.services"));
        report(result, "services.version_created");
    }

    private void toggleAvailability() {
        ServiceDefinition definition = selectedDefinition();
        if (definition == null) {
            return;
        }
        boolean wasAvailable = definition.isAvailable();
        Result<ServiceDefinition> result =
                services.setServiceAvailability()
                        .execute(
                                new SetServiceAvailability.Command(
                                        session.require(),
                                        definition.id(),
                                        !wasAvailable,
                                        "desktop.services"));
        report(result, wasAvailable ? "services.deactivated" : "services.activated");
    }

    private void seedCatalog() {
        Result<?> result =
                services.seedServiceCatalog()
                        .execute(
                                new SeedServiceCatalog.Command(
                                        session.require(), "desktop.services"));
        report(result, "services.seeded");
    }

    private void report(Result<?> result, String successKey) {
        if (result instanceof Result.Err<?> err) {
            toasts.error(err.messageKey());
            return;
        }
        toasts.success(successKey);
        ServiceDefinition definition = definitions.getSelectionModel().getSelectedItem();
        refresh();
        if (definition != null) {
            showDefinition(definition);
        }
    }

    // ---- form → domain ---------------------------------------------------

    /**
     * Requirements are typed as a comma-separated list of labels.
     *
     * <p>Every one becomes a mandatory intake document, which is what a counter requirement almost
     * always is: "cédula, título de propiedad, croquis". Conditional and stage-specific
     * requirements are imported configuration, not something to compose in a text field.
     */
    private List<RequirementDef> requirements() {
        List<RequirementDef> out = new ArrayList<>();
        String raw = requirementsText.getText();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        int index = 1;
        for (String label : raw.split(",")) {
            String trimmed = label.strip();
            if (trimmed.isEmpty()) {
                continue;
            }
            out.add(
                    new RequirementDef(
                            "req-" + index++,
                            trimmed,
                            RequirementKind.DOCUMENT,
                            RequirementStage.INTAKE,
                            true,
                            Optional.empty(),
                            Optional.empty()));
        }
        return out;
    }

    private Sla sla() {
        int days = positiveInt(slaDays.getText());
        return days == 0 ? Sla.none() : new Sla(days, slaBasis.getValue(), OptionalInt.empty());
    }

    private Validity validity() {
        int days = positiveInt(validityDays.getText());
        return days == 0 ? Validity.permanent() : Validity.ofDays(days, renewable.isSelected());
    }

    /** {@code null} signals a parse failure the caller must abort on; empty means "no fee". */
    private Optional<List<FeeRule>> feeRules(ServiceDefinitionVersion version) {
        String raw = feeAmount.getText();
        if (raw == null || raw.isBlank()) {
            return Optional.of(List.of());
        }
        Money amount;
        try {
            amount = Money.of(new java.math.BigDecimal(raw.strip()), currency());
        } catch (NumberFormatException | ArithmeticException e) {
            toasts.error("services.invalid_fee");
            return null;
        }
        String concept =
                text(feeConcept).orElseGet(this::selectedDefinitionName);
        return Optional.of(
                List.of(
                        FeeRule.fixed(
                                version.id() + "-fee",
                                feeChargeType.getValue(),
                                concept,
                                CURRENCY,
                                amount.minorUnits(),
                                LocalDate.now())));
    }

    // ---- helpers ---------------------------------------------------------

    private static java.util.Currency currency() {
        return java.util.Currency.getInstance(CURRENCY);
    }

    private String selectedDefinitionName() {
        ServiceDefinition definition = definitions.getSelectionModel().getSelectedItem();
        return definition == null ? Messages.get("services.fee_default_concept") : definition.name();
    }

    private ServiceDefinition selectedDefinition() {
        ServiceDefinition definition = definitions.getSelectionModel().getSelectedItem();
        if (definition == null) {
            toasts.warning("services.pick");
        }
        return definition;
    }

    private ServiceDefinitionVersion selectedDraft() {
        ServiceDefinitionVersion version = versions.getSelectionModel().getSelectedItem();
        if (version == null) {
            toasts.warning("services.pick_version");
            return null;
        }
        if (!version.status().isEditable()) {
            toasts.error("services.not_editable");
            return null;
        }
        return version;
    }

    private String categoryName(ServiceDefinition definition) {
        return services.serviceCatalog()
                .findCategoryById(definition.categoryId())
                .map(ServiceCategory::name)
                .orElse("—");
    }

    private ListCell<ServiceCategory> categoryCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(ServiceCategory item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        };
    }

    private static boolean isBlank(TextField field) {
        return field.getText() == null || field.getText().isBlank();
    }

    private static int positiveInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(raw.strip()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Optional<String> text(TextField field) {
        return Optional.ofNullable(field.getText()).filter(s -> !s.isBlank()).map(String::strip);
    }

    private TableColumn<ServiceDefinition, String> col(
            String headerKey,
            double width,
            java.util.function.Function<ServiceDefinition, String> value) {
        TableColumn<ServiceDefinition, String> c = new TableColumn<>(Messages.get(headerKey));
        c.setPrefWidth(width);
        c.setCellValueFactory(x -> new SimpleStringProperty(value.apply(x.getValue())));
        return c;
    }

    private TableColumn<ServiceDefinitionVersion, String> versionCol(
            String headerKey,
            double width,
            java.util.function.Function<ServiceDefinitionVersion, String> value) {
        TableColumn<ServiceDefinitionVersion, String> c = new TableColumn<>(Messages.get(headerKey));
        c.setPrefWidth(width);
        c.setCellValueFactory(x -> new SimpleStringProperty(value.apply(x.getValue())));
        return c;
    }

    /** Exposed for tests: how many services the catalogue list holds. */
    public int serviceCount() {
        return definitions.getItems().size();
    }

    /** Exposed for tests: whether the draft editor is currently accepting edits. */
    public boolean isEditorEnabled() {
        return !editorBox.isDisabled();
    }
}
