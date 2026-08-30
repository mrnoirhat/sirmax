// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.view;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.sirmax.application.port.DocumentRepository;
import org.sirmax.application.usecase.PrintDocument;
import org.sirmax.domain.document.IssuedDocument;
import org.sirmax.domain.document.PaperFormat;
import org.sirmax.domain.document.PrinterProfile;
import org.sirmax.domain.security.Permission;
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
 * Issued documents: find one, verify it, reprint it, and configure where printing goes
 * (master prompt §46, §59D, §59F).
 *
 * <p>The search is by document number or verification code and nothing else. Both are printed on
 * the paper the citizen is holding, which is the only thing anyone standing at a counter has. A
 * free-text search over customer names would find several documents where the citizen needs one,
 * and picking the wrong one is how the wrong certificate gets reissued.
 *
 * <p>Every reprint asks for a reason and is marked COPIA on the output (§59D). That is not
 * bureaucracy: an unmarked reprint of a receipt is indistinguishable from a second payment, and the
 * print history below is what settles the argument when a citizen returns with two of them.
 */
public final class DocumentsView implements SirmaxView {

    /** How many recent documents the screen lists before anyone searches. */
    private static final int PAGE_SIZE = 100;

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final AppServices services;
    private final UiSession session;
    private final ToastHost toasts;

    private final TextField query = new TextField();
    private final TableView<IssuedDocument> results = new TableView<>();
    private final TableView<DocumentRepository.PrintEntry> history = new TableView<>();
    private final Label detail = new Label();
    private final Banner outcome = new Banner();
    private final ComboBox<PrinterProfile> profile = new ComboBox<>();

    // ── printer profiles ──
    private final TableView<PrinterProfile> profiles = new TableView<>();
    private final TextField profileName = new TextField();
    private final ComboBox<String> queueName = new ComboBox<>();
    private final ComboBox<PaperFormat> paper = new ComboBox<>();
    private final Spinner<Integer> copies = new Spinner<>(1, 5, 1);
    private final CheckBox silent = new CheckBox();
    private final CheckBox defaultProfile = new CheckBox();
    private final VBox profilesBox = new VBox(10);

    private final VBox root = new VBox(16);

    public DocumentsView(AppServices services, UiSession session, ToastHost toasts) {
        this.services = services;
        this.session = session;
        this.toasts = toasts;
        build();
    }

    @Override
    public RouteKey route() {
        return RouteKey.DOCUMENTS;
    }

    @Override
    public String titleKey() {
        return "nav.documents";
    }

    @Override
    public Parent node() {
        refreshProfiles();
        showRecent();
        return root;
    }

    /**
     * The documents issued most recently.
     *
     * <p>The screen used to open empty and wait for a number. That is the right tool when a citizen
     * is holding the paper, and no help at all for "what did we issue today" — which left the
     * impression that there were no documents.
     */
    public void showRecent() {
        List<IssuedDocument> recent = services.documents().listRecent(PAGE_SIZE, 0);
        results.setItems(FXCollections.observableArrayList(recent));
        if (recent.isEmpty()) {
            outcome.show(Banner.Severity.INFO, "documents.none_yet", "documents.none_yet.hint");
        } else {
            outcome.hide();
            results.getSelectionModel().selectFirst();
        }
    }

    // ---- construction ----------------------------------------------------

    private void build() {
        detail.getStyleClass().add(Styles.BODY);
        detail.setWrapText(true);

        buildResults();
        buildHistory();
        buildProfileEditor();

        query.setPromptText(Messages.get("documents.search.prompt"));
        query.setOnAction(e -> search());

        HBox searchRow = new HBox(8, query, Buttons.primary("documents.search", this::search));
        searchRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(query, javafx.scene.layout.Priority.ALWAYS);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        if (session.can(Permission.INVOICE_REPRINT)) {
            actions.getChildren()
                    .addAll(
                            new FormField("documents.printer", profile),
                            Buttons.secondary("documents.reprint", this::reprint));
        }
        if (session.can(Permission.INVOICE_VOID)) {
            actions.getChildren().add(Buttons.danger("documents.void", this::voidDocument));
        }

        root.getChildren()
                .addAll(
                        Typography.title("documents.title"),
                        Typography.muted("documents.explain"),
                        outcome,
                        Cards.card(
                                Typography.subtitle("documents.find"),
                                searchRow,
                                results,
                                detail,
                                actions),
                        Cards.card(Typography.subtitle("documents.history"), history),
                        Cards.card(Typography.subtitle("documents.profiles"), profilesBox));
    }

    private void buildResults() {
        DataTable.styled(results);
        results.setPrefHeight(220);
        results.getColumns()
                .addAll(
                        List.of(
                                col("documents.column.number", 180, IssuedDocument::documentNumber),
                                col(
                                        "documents.column.kind",
                                        150,
                                        d -> Enums.label("document.kind", d.kind())),
                                col(
                                        "documents.column.customer",
                                        220,
                                        d -> d.snapshot().customer().name()),
                                col(
                                        "documents.column.total",
                                        130,
                                        d -> d.snapshot().totals().total().toString()),
                                col(
                                        "documents.column.issued",
                                        150,
                                        d -> STAMP.format(d.issuedAt())),
                                col(
                                        "documents.column.prints",
                                        90,
                                        d -> String.valueOf(d.printCount()))));
        results.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, old, document) -> showDocument(document));
    }

    private void buildHistory() {
        DataTable.styled(history);
        history.setPrefHeight(150);
        history.getColumns()
                .addAll(
                        List.of(
                                historyCol(
                                        "documents.column.printed_at",
                                        160,
                                        e -> STAMP.format(e.printedAt())),
                                historyCol(
                                        "documents.column.printed_by",
                                        180,
                                        e -> userName(e.printedBy())),
                                historyCol(
                                        "documents.column.copy",
                                        110,
                                        e ->
                                                Messages.get(
                                                        e.isReprint()
                                                                ? "documents.is_copy"
                                                                : "documents.is_original")),
                                historyCol(
                                        "documents.column.reason",
                                        280,
                                        e -> e.reason().orElse("—"))));
    }

    private void buildProfileEditor() {
        DataTable.styled(profiles);
        profiles.setPrefHeight(150);
        profiles.getColumns()
                .addAll(
                        List.of(
                                profileCol("documents.column.profile", 200, PrinterProfile::name),
                                profileCol(
                                        "documents.column.queue",
                                        240,
                                        p -> p.printerName().orElse(Messages.get("documents.queue_default"))),
                                profileCol(
                                        "documents.column.paper",
                                        150,
                                        p -> Enums.label("paper.format", p.paperFormat())),
                                profileCol(
                                        "documents.column.copies",
                                        90,
                                        p -> String.valueOf(p.copies())),
                                profileCol(
                                        "documents.column.silent",
                                        110,
                                        p ->
                                                Messages.get(
                                                        p.silent() ? "common.yes" : "common.no"))));

        paper.setItems(FXCollections.observableArrayList(PaperFormat.values()));
        paper.setValue(PaperFormat.LETTER);
        paper.setCellFactory(list -> Enums.cell("paper.format"));
        paper.setButtonCell(Enums.cell("paper.format"));
        silent.setText(Messages.get("documents.silent"));
        defaultProfile.setText(Messages.get("documents.is_default"));
        profile.setCellFactory(list -> profileCell());
        profile.setButtonCell(profileCell());
        queueName.setCellFactory(list -> queueCell());
        queueName.setButtonCell(queueCell());

        profilesBox.getChildren()
                .addAll(
                        Typography.muted("documents.profiles.explain"),
                        profiles,
                        new FormField("documents.profile_name", profileName),
                        new FormField("documents.queue", queueName, "documents.queue.hint"),
                        new FormField("documents.paper", paper),
                        new FormField("documents.copies", copies),
                        silent,
                        defaultProfile,
                        Buttons.primary("documents.save_profile", this::saveProfile));
        boolean canConfigure = session.can(Permission.CONFIG_MANAGE);
        profilesBox.setVisible(canConfigure);
        profilesBox.setManaged(canConfigure);
    }

    // ---- data ------------------------------------------------------------

    /** Reload the printer profiles and the queues Windows currently offers. */
    public void refreshProfiles() {
        List<PrinterProfile> all = services.documents().listProfiles();
        profiles.setItems(FXCollections.observableArrayList(all));
        profile.setItems(FXCollections.observableArrayList(all));
        all.stream().filter(PrinterProfile::isDefault).findFirst().ifPresent(profile::setValue);

        // A workstation with no printers is normal on a fresh install; the combo stays empty and
        // the profile falls back to the system default queue.
        List<String> queues = services.printer().availablePrinters();
        queueName.setItems(FXCollections.observableArrayList(queues));
        services.printer().defaultPrinter().filter(queues::contains).ifPresent(queueName::setValue);
    }

    private void search() {
        String raw = query.getText();
        if (raw == null || raw.isBlank()) {
            showRecent();
            return;
        }
        String term = raw.strip();

        // Number first, then verification code. Both are printed on the paper, and a citizen who
        // reads out "the code" could mean either.
        Optional<IssuedDocument> found = services.documents().findByNumber(term);
        if (found.isEmpty()) {
            found = verificationLookup(term);
        }

        if (found.isEmpty()) {
            results.setItems(FXCollections.emptyObservableList());
            outcome.show(Banner.Severity.WARNING, "documents.not_found", "documents.not_found.hint");
            return;
        }
        outcome.hide();
        results.setItems(FXCollections.observableArrayList(found.get()));
        results.getSelectionModel().selectFirst();
    }

    /**
     * A verification code that does not parse is a typo, not a failure worth a stack trace: the
     * caller treats it exactly like "no such document".
     */
    private Optional<IssuedDocument> verificationLookup(String term) {
        try {
            return services.documents()
                    .findByVerificationCode(
                            new org.sirmax.domain.document.VerificationCode(term.toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    private void showDocument(IssuedDocument document) {
        if (document == null) {
            detail.setText("");
            history.setItems(FXCollections.emptyObservableList());
            return;
        }
        detail.setText(
                Messages.get(
                        "documents.detail",
                        document.documentNumber(),
                        document.verificationCode().value(),
                        document.snapshot().customer().name(),
                        document.snapshot().totals().total().toString(),
                        STAMP.format(document.issuedAt())));
        history.setItems(
                FXCollections.observableArrayList(
                        services.documents().printHistory(document.id())));

        if (document.isVoided()) {
            outcome.show(Banner.Severity.WARNING, "documents.voided", "documents.voided.hint");
        } else if (document.isReprintNext()) {
            outcome.show(Banner.Severity.INFO, "documents.next_is_copy", "documents.next_is_copy.hint");
        } else {
            outcome.hide();
        }
    }

    // ---- actions ---------------------------------------------------------

    private void reprint() {
        IssuedDocument document = selected();
        if (document == null) {
            return;
        }
        if (document.isVoided()) {
            toasts.error("documents.cannot_reprint_voided");
            return;
        }

        Optional<String> reason = prompt("documents.reprint", "documents.reprint_reason");
        if (reason.isEmpty()) {
            return; // cancelled; a reprint without a reason is not recorded, so it is not done
        }

        Result<PrintDocument.Outcome> result =
                services.printDocument()
                        .execute(
                                new PrintDocument.Command(
                                        session.require(),
                                        document.id(),
                                        Optional.ofNullable(profile.getValue())
                                                .map(PrinterProfile::id),
                                        reason,
                                        workstation(),
                                        "desktop.documents"));
        if (result instanceof Result.Err<PrintDocument.Outcome> err) {
            toasts.error(err.messageKey());
            return;
        }
        PrintDocument.Outcome printed = result.orElseThrow();
        if (printed.sentToPrinter()) {
            toasts.success(
                    printed.wasReprint() ? "documents.reprinted_copy" : "documents.reprinted",
                    document.documentNumber());
        } else {
            toasts.info("documents.print_cancelled", document.documentNumber());
        }
        // Re-read: the print count and the history both changed.
        services.documents().findById(document.id()).ifPresent(this::reselect);
    }

    private void voidDocument() {
        IssuedDocument document = selected();
        if (document == null) {
            return;
        }
        if (document.isVoided()) {
            toasts.info("documents.already_voided");
            return;
        }
        // Voiding is recorded on the document itself; the invoice it belongs to is voided from the
        // billing screen, which is where the money side of the decision lives.
        if (!org.sirmax.ui.designsystem.Dialogs.confirm(
                root.getScene() == null ? null : root.getScene().getWindow(),
                "documents.void",
                "documents.void_confirm",
                "documents.void")) {
            return;
        }
        document.voidDocument();
        services.documents().save(document);
        toasts.success("documents.voided_ok", document.documentNumber());
        reselect(document);
    }

    private void saveProfile() {
        if (profileName.getText() == null || profileName.getText().isBlank()) {
            toasts.error("documents.profile_name_required");
            return;
        }
        PrinterProfile existing = profiles.getSelectionModel().getSelectedItem();
        java.time.Instant now = java.time.Instant.now();
        PrinterProfile saved =
                new PrinterProfile(
                        existing == null
                                ? java.util.UUID.randomUUID().toString()
                                : existing.id(),
                        profileName.getText().strip(),
                        Optional.ofNullable(queueName.getValue()).filter(s -> !s.isBlank()),
                        paper.getValue(),
                        // Empty means "any workstation", which is right for the single-machine
                        // office this most often runs in.
                        Optional.empty(),
                        defaultProfile.isSelected(),
                        copies.getValue(),
                        silent.isSelected(),
                        existing == null ? now : existing.createdAt(),
                        now);
        services.documents().save(saved);
        toasts.success("documents.profile_saved", saved.name());
        profileName.clear();
        refreshProfiles();
    }

    // ---- helpers ---------------------------------------------------------

    private void reselect(IssuedDocument document) {
        results.setItems(FXCollections.observableArrayList(document));
        results.getSelectionModel().selectFirst();
        showDocument(document);
    }

    private IssuedDocument selected() {
        IssuedDocument document = results.getSelectionModel().getSelectedItem();
        if (document == null) {
            toasts.warning("documents.pick");
        }
        return document;
    }

    /** Printer profiles can be per-machine, so the view has to say which machine it is (§59D). */
    private static String workstation() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (java.net.UnknownHostException e) {
            return "";
        }
    }

    private String userName(Optional<String> userId) {
        return userId.flatMap(id -> services.users().findById(id))
                .map(org.sirmax.domain.security.AppUser::displayName)
                .orElse("—");
    }

    private Optional<String> prompt(String titleKey, String contentKey) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(Messages.get(titleKey));
        dialog.setContentText(Messages.get(contentKey));
        return dialog.showAndWait().map(String::strip).filter(s -> !s.isBlank());
    }

    private javafx.scene.control.ListCell<PrinterProfile> profileCell() {
        return new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(PrinterProfile item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        };
    }

    private javafx.scene.control.ListCell<String> queueCell() {
        return new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
            }
        };
    }

    private TableColumn<IssuedDocument, String> col(
            String headerKey,
            double width,
            java.util.function.Function<IssuedDocument, String> value) {
        TableColumn<IssuedDocument, String> c = new TableColumn<>(Messages.get(headerKey));
        c.setPrefWidth(width);
        c.setCellValueFactory(x -> new SimpleStringProperty(value.apply(x.getValue())));
        return c;
    }

    private TableColumn<DocumentRepository.PrintEntry, String> historyCol(
            String headerKey,
            double width,
            java.util.function.Function<DocumentRepository.PrintEntry, String> value) {
        TableColumn<DocumentRepository.PrintEntry, String> c =
                new TableColumn<>(Messages.get(headerKey));
        c.setPrefWidth(width);
        c.setCellValueFactory(x -> new SimpleStringProperty(value.apply(x.getValue())));
        return c;
    }

    private TableColumn<PrinterProfile, String> profileCol(
            String headerKey,
            double width,
            java.util.function.Function<PrinterProfile, String> value) {
        TableColumn<PrinterProfile, String> c = new TableColumn<>(Messages.get(headerKey));
        c.setPrefWidth(width);
        c.setCellValueFactory(x -> new SimpleStringProperty(value.apply(x.getValue())));
        return c;
    }

    /** Exposed for tests: how many documents the result list holds. */
    public int resultCount() {
        return results.getItems().size();
    }
}
