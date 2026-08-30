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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.sirmax.application.usecase.CreateBackup;
import org.sirmax.application.usecase.ManageBackupPolicy;
import org.sirmax.application.usecase.VerifyAuditIntegrity;
import org.sirmax.domain.audit.AuditChain;
import org.sirmax.domain.backup.BackupKind;
import org.sirmax.domain.backup.BackupRecord;
import org.sirmax.domain.backup.BackupSchedule;
import org.sirmax.domain.org.InstitutionProfile;
import org.sirmax.domain.org.OrganizationUnit;
import org.sirmax.domain.security.Permission;
import org.sirmax.domain.security.SecurityPolicy;
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
import org.sirmax.ui.theme.Theme;
import org.sirmax.ui.theme.ThemeManager;

/**
 * Configuration: who the municipality is, how it backs itself up, and how strict its sign-in is
 * (master prompt §41–§42, §59C).
 *
 * <p>Grouped by who changes it and how often. The institution profile is filled in once at install
 * and appears on every document; backups run on a schedule nobody should have to remember; the
 * security policy is a rarely-touched set of numbers that has to be visible so an administrator can
 * see it is not the default.
 *
 * <p>Restore is deliberately <em>not</em> here. It is a destructive operation with a mandatory
 * sequence (§42: validate, emergency backup, confirm, restore, verify), and burying it in a
 * settings screen next to a colour picker is how it gets pressed by accident. It stays a
 * command-line operation until it has a screen built around that sequence.
 */
public final class SettingsView implements SirmaxView {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final AppServices services;
    private final UiSession session;
    private final ToastHost toasts;
    private final ThemeManager themeManager;

    // ── institution ──
    private final Label institutionName = new Label();
    private final TextField legalIdentifier = new TextField();
    private final TextField address = new TextField();
    private final TextField phone = new TextField();
    private final TextField email = new TextField();
    private final TextField website = new TextField();
    private final TextField invoiceFooter = new TextField();
    private final TextField documentHeader = new TextField();
    private final VBox institutionBox = new VBox(10);

    // ── appearance ──
    private final ComboBox<Theme> theme = new ComboBox<>();

    // ── backup ──
    private final CheckBox backupEnabled = new CheckBox();
    private final ComboBox<BackupSchedule.Frequency> frequency = new ComboBox<>();
    private final Spinner<Integer> hourOfDay = new Spinner<>(0, 23, 20);
    private final Spinner<Integer> keepCopies = new Spinner<>(1, 365, 30);
    private final CheckBox encrypt = new CheckBox();
    private final TableView<BackupRecord> backups = new TableView<>();
    private final VBox backupBox = new VBox(10);

    // ── security ──
    private final Spinner<Integer> minPasswordLength = new Spinner<>(8, 64, 12);
    private final Spinner<Integer> maxFailedAttempts = new Spinner<>(3, 20, 5);
    private final Spinner<Integer> lockoutMinutes = new Spinner<>(1, 1440, 15);
    private final Spinner<Integer> idleLockMinutes = new Spinner<>(1, 480, 20);
    private final Spinner<Integer> sessionMaxHours = new Spinner<>(1, 72, 12);
    private final Spinner<Integer> maxAttachmentMb = new Spinner<>(1, 500, 25);
    private final Banner integrity = new Banner();
    private final VBox securityBox = new VBox(10);

    private final VBox root = new VBox(16);

    public SettingsView(
            AppServices services, UiSession session, ToastHost toasts, ThemeManager themeManager) {
        this.services = services;
        this.session = session;
        this.toasts = toasts;
        this.themeManager = themeManager;
        build();
    }

    @Override
    public RouteKey route() {
        return RouteKey.SETTINGS;
    }

    @Override
    public String titleKey() {
        return "nav.settings";
    }

    @Override
    public Parent node() {
        refresh();
        return root;
    }

    // ---- construction ----------------------------------------------------

    private void build() {
        institutionName.getStyleClass().add(Styles.SUBTITLE);
        buildInstitution();
        buildBackup();
        buildSecurity();

        theme.setItems(FXCollections.observableArrayList(Theme.values()));
        theme.setCellFactory(list -> Enums.cell("theme"));
        theme.setButtonCell(Enums.cell("theme"));
        theme.setValue(themeManager.current());
        theme.valueProperty()
                .addListener(
                        (obs, old, value) -> {
                            if (value != null && value != themeManager.current()) {
                                themeManager.set(value);
                            }
                        });

        root.getChildren()
                .addAll(
                        Typography.title("settings.title"),
                        Cards.card(institutionName, institutionBox),
                        Cards.card(
                                Typography.subtitle("settings.appearance"),
                                Typography.muted("settings.appearance.explain"),
                                new FormField("settings.theme", theme)),
                        Cards.card(Typography.subtitle("settings.backup"), backupBox),
                        Cards.card(Typography.subtitle("settings.security"), securityBox));
    }

    private void buildInstitution() {
        institutionBox
                .getChildren()
                .addAll(
                        Typography.muted("settings.institution.explain"),
                        new FormField("settings.legal_id", legalIdentifier, "settings.legal_id.hint"),
                        new FormField("settings.address", address),
                        new FormField("settings.phone", phone),
                        new FormField("settings.email", email),
                        new FormField("settings.website", website),
                        new FormField(
                                "settings.invoice_footer", invoiceFooter, "settings.invoice_footer.hint"),
                        new FormField("settings.document_header", documentHeader),
                        Buttons.primary("settings.save_institution", this::saveInstitution));
        boolean canManage = session.can(Permission.CONFIG_MANAGE);
        institutionBox.setDisable(!canManage);
    }

    private void buildBackup() {
        backupEnabled.setText(Messages.get("settings.backup_enabled"));
        encrypt.setText(Messages.get("settings.backup_encrypt"));
        frequency.setItems(FXCollections.observableArrayList(BackupSchedule.Frequency.values()));
        frequency.setCellFactory(list -> Enums.cell("backup.frequency"));
        frequency.setButtonCell(Enums.cell("backup.frequency"));

        DataTable.styled(backups);
        backups.setPrefHeight(180);
        backups.getColumns()
                .addAll(
                        List.of(
                                backupCol("settings.column.code", 170, BackupRecord::code),
                                backupCol(
                                        "settings.column.kind",
                                        130,
                                        b -> Enums.label("backup.kind", b.kind())),
                                backupCol(
                                        "settings.column.when",
                                        160,
                                        b -> STAMP.format(b.createdAt())),
                                backupCol("settings.column.size", 120, b -> megabytes(b.sizeBytes())),
                                backupCol(
                                        "settings.column.offsite",
                                        110,
                                        b ->
                                                Messages.get(
                                                        b.isOffsite()
                                                                ? "common.yes"
                                                                : "common.no"))));

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        if (session.can(Permission.CONFIG_MANAGE)) {
            actions.getChildren().add(Buttons.secondary("settings.save_backup", this::saveBackupPolicy));
        }
        if (session.can(Permission.BACKUP_RUN)) {
            actions.getChildren().add(Buttons.primary("settings.backup_now", this::backupNow));
        }

        backupBox
                .getChildren()
                .addAll(
                        Typography.muted("settings.backup.explain"),
                        backupEnabled,
                        new FormField("settings.backup_frequency", frequency),
                        new FormField("settings.backup_hour", hourOfDay, "settings.backup_hour.hint"),
                        new FormField("settings.backup_keep", keepCopies, "settings.backup_keep.hint"),
                        encrypt,
                        actions,
                        Typography.subtitle("settings.backup_history"),
                        backups);
    }

    private void buildSecurity() {
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        if (session.can(Permission.CONFIG_MANAGE)) {
            actions.getChildren().add(Buttons.primary("settings.save_security", this::saveSecurity));
        }
        if (session.can(Permission.AUDIT_READ)) {
            actions.getChildren()
                    .add(Buttons.secondary("settings.verify_audit", this::verifyAudit));
        }

        securityBox
                .getChildren()
                .addAll(
                        Typography.muted("settings.security.explain"),
                        new FormField("settings.min_password", minPasswordLength),
                        new FormField("settings.max_attempts", maxFailedAttempts),
                        new FormField("settings.lockout", lockoutMinutes, "settings.minutes"),
                        new FormField("settings.idle_lock", idleLockMinutes, "settings.minutes"),
                        new FormField("settings.session_max", sessionMaxHours, "settings.hours"),
                        new FormField("settings.max_attachment", maxAttachmentMb, "settings.megabytes"),
                        actions,
                        integrity);
    }

    // ---- data ------------------------------------------------------------

    /** Read every setting back from storage, so the screen never shows a stale form. */
    public void refresh() {
        Optional<OrganizationUnit> unit = services.organization().findActive();
        institutionName.setText(
                unit.map(OrganizationUnit::name).orElseGet(() -> Messages.get("settings.no_institution")));
        unit.flatMap(u -> services.organization().findProfile(u.id()))
                .ifPresent(
                        profile -> {
                            legalIdentifier.setText(profile.legalIdentifier().orElse(""));
                            address.setText(profile.address().orElse(""));
                            phone.setText(profile.phone().orElse(""));
                            email.setText(profile.email().orElse(""));
                            website.setText(profile.website().orElse(""));
                            invoiceFooter.setText(profile.invoiceFooter().orElse(""));
                            documentHeader.setText(profile.documentHeader().orElse(""));
                        });

        BackupSchedule schedule = services.backups().loadSchedule();
        backupEnabled.setSelected(schedule.enabled());
        frequency.setValue(schedule.frequency());
        hourOfDay.getValueFactory().setValue(schedule.hourOfDay());
        keepCopies.getValueFactory().setValue(schedule.keepCopies());
        encrypt.setSelected(schedule.encrypt());
        backups.setItems(FXCollections.observableArrayList(services.backups().list(20, 0)));

        SecurityPolicy policy = services.securityPolicy().load();
        minPasswordLength.getValueFactory().setValue(policy.minPasswordLength());
        maxFailedAttempts.getValueFactory().setValue(policy.maxFailedAttempts());
        lockoutMinutes.getValueFactory().setValue(policy.lockoutMinutes());
        idleLockMinutes.getValueFactory().setValue(policy.idleLockMinutes());
        sessionMaxHours.getValueFactory().setValue(policy.sessionMaxHours());
        maxAttachmentMb.getValueFactory().setValue(policy.maxAttachmentMb());

        theme.setValue(themeManager.current());
    }

    // ---- actions ---------------------------------------------------------

    private void saveInstitution() {
        Optional<OrganizationUnit> unit = services.organization().findActive();
        if (unit.isEmpty()) {
            toasts.error("settings.no_institution");
            return;
        }
        String unitId = unit.get().id();
        InstitutionProfile current =
                services.organization()
                        .findProfile(unitId)
                        .orElseGet(() -> InstitutionProfile.empty(unitId));

        // Overrides take "" as "clear this field" and null as "leave it": every field on this form
        // is bound, so emptying one is a deliberate erasure and is passed through as such.
        InstitutionProfile.Overrides o = new InstitutionProfile.Overrides();
        o.legalIdentifier = value(legalIdentifier);
        o.address = value(address);
        o.phone = value(phone);
        o.email = value(email);
        o.website = value(website);
        o.invoiceFooter = value(invoiceFooter);
        o.documentHeader = value(documentHeader);

        services.organization().saveProfile(current.with(o));
        toasts.success("settings.institution_saved");
    }

    private void saveBackupPolicy() {
        Result<BackupSchedule> result =
                services.manageBackupPolicy()
                        .configure(
                                new ManageBackupPolicy.ConfigureCommand(
                                        session.require(),
                                        backupEnabled.isSelected(),
                                        frequency.getValue(),
                                        hourOfDay.getValue(),
                                        keepCopies.getValue(),
                                        encrypt.isSelected(),
                                        "desktop.settings"));
        report(result, "settings.backup_saved");
    }

    private void backupNow() {
        BackupSchedule schedule = services.backups().loadSchedule();
        if (schedule.encrypt()) {
            // An encrypted backup needs a passphrase, and a passphrase typed into a settings form
            // is a passphrase stored in a screenshot. It belongs in a dedicated prompt, which the
            // backup screen (§41) owns.
            toasts.warning("settings.backup_encrypted_cli");
            return;
        }
        Result<BackupRecord> result =
                services.createBackup()
                        .execute(
                                new CreateBackup.Command(
                                        session.require(),
                                        BackupKind.MANUAL,
                                        Optional.empty(),
                                        Optional.of("desktop.settings"),
                                        "desktop.settings"));
        if (result instanceof Result.Err<BackupRecord> err) {
            toasts.error(err.messageKey());
            return;
        }
        toasts.success("settings.backup_done", result.orElseThrow().code());
        backups.setItems(FXCollections.observableArrayList(services.backups().list(20, 0)));
    }

    private void saveSecurity() {
        SecurityPolicy policy =
                new SecurityPolicy(
                        minPasswordLength.getValue(),
                        maxFailedAttempts.getValue(),
                        lockoutMinutes.getValue(),
                        idleLockMinutes.getValue(),
                        sessionMaxHours.getValue(),
                        maxAttachmentMb.getValue(),
                        java.time.Instant.now());
        services.securityPolicy().save(policy);
        toasts.success("settings.security_saved");
    }

    private void verifyAudit() {
        Result<AuditChain.Verification> result =
                services.verifyAuditIntegrity()
                        .execute(new VerifyAuditIntegrity.Command(session.require()));
        if (result instanceof Result.Err<AuditChain.Verification> err) {
            toasts.error(err.messageKey());
            return;
        }
        AuditChain.Verification verification = result.orElseThrow();
        if (verification.isIntact()) {
            integrity.show(
                    Banner.Severity.SUCCESS,
                    "settings.audit_intact",
                    null,
                    verification.verifiedEntries());
        } else {
            // A broken chain names the event it broke at. That is the whole point of the chain: it
            // says where to look, not merely that something is wrong.
            integrity.show(
                    Banner.Severity.DANGER,
                    "settings.audit_broken",
                    "settings.audit_broken.hint",
                    verification.brokenAtEventId().orElse("—"));
        }
    }

    private void report(Result<?> result, String successKey) {
        if (result instanceof Result.Err<?> err) {
            toasts.error(err.messageKey());
            return;
        }
        toasts.success(successKey);
        refresh();
    }

    // ---- helpers ---------------------------------------------------------

    /** "" clears the stored value; that is what an emptied bound field means. */
    private static String value(TextField field) {
        return field.getText() == null ? "" : field.getText().strip();
    }

    private static String megabytes(long bytes) {
        return String.format(java.util.Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private TableColumn<BackupRecord, String> backupCol(
            String headerKey, double width, java.util.function.Function<BackupRecord, String> value) {
        TableColumn<BackupRecord, String> c = new TableColumn<>(Messages.get(headerKey));
        c.setPrefWidth(width);
        c.setCellValueFactory(x -> new SimpleStringProperty(value.apply(x.getValue())));
        return c;
    }

    /** Exposed for tests: how many backup records the history holds. */
    public int backupCount() {
        return backups.getItems().size();
    }
}
