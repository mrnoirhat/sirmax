// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.view;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.sirmax.domain.org.Department;
import org.sirmax.domain.org.OrganizationUnit;
import org.sirmax.domain.security.AppUser;
import org.sirmax.domain.security.Permission;
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
import org.sirmax.ui.nav.RouteKey;

/**
 * Departments and the people who work in them (master prompt §21).
 *
 * <p>Departments are what cases get routed to, so the list has to stay honest: a department is
 * archived rather than deleted, because procedures already routed to it keep referring to it and a
 * dangling reference in a case file is worse than a name nobody uses any more.
 *
 * <p>The user list here is read-only. Creating accounts and assigning roles is a separate concern
 * with its own permission ({@code user.manage}, {@code role.manage}) and its own consequences; this
 * screen answers "who is in Planeamiento", which is the question an administrator has while looking
 * at departments.
 */
public final class DepartmentsView implements SirmaxView {

    private final AppServices services;
    private final UiSession session;
    private final ToastHost toasts;

    private final Label institution = new Label();
    private final TableView<Department> departments = new TableView<>();
    private final TableView<AppUser> users = new TableView<>();
    private final TextField newName = new TextField();
    private final TextField newCode = new TextField();
    private final VBox createBox = new VBox(10);
    private final Banner status = new Banner();

    private final VBox root = new VBox(16);

    public DepartmentsView(AppServices services, UiSession session, ToastHost toasts) {
        this.services = services;
        this.session = session;
        this.toasts = toasts;
        build();
    }

    @Override
    public RouteKey route() {
        return RouteKey.DEPARTMENTS;
    }

    @Override
    public String titleKey() {
        return "nav.departments";
    }

    @Override
    public Parent node() {
        refresh();
        return root;
    }

    // ---- construction ----------------------------------------------------

    private void build() {
        institution.getStyleClass().add(Styles.SUBTITLE);

        DataTable.styled(departments);
        departments.setPrefHeight(240);
        departments
                .getColumns()
                .addAll(
                        List.of(
                                col("departments.column.code", 140, Department::code),
                                col("departments.column.name", 320, Department::name),
                                col(
                                        "departments.column.status",
                                        140,
                                        d ->
                                                Messages.get(
                                                        d.isActive()
                                                                ? "departments.active"
                                                                : "departments.archived"))));

        DataTable.styled(users);
        users.setPrefHeight(220);
        users.getColumns()
                .addAll(
                        List.of(
                                userCol("departments.column.user", 240, AppUser::displayName),
                                userCol("departments.column.username", 200, AppUser::username),
                                userCol(
                                        "departments.column.roles",
                                        260,
                                        this::roleNames)));

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        if (session.can(Permission.DEPARTMENT_MANAGE)) {
            actions.getChildren().add(Buttons.secondary("departments.archive", this::archive));
        }

        createBox
                .getChildren()
                .addAll(
                        Typography.muted("departments.create.explain"),
                        new FormField("departments.name", newName),
                        new FormField("departments.code", newCode, "departments.code.hint"),
                        Buttons.primary("departments.create", this::create));
        boolean canManage = session.can(Permission.DEPARTMENT_MANAGE);
        createBox.setVisible(canManage);
        createBox.setManaged(canManage);

        root.getChildren()
                .addAll(
                        Typography.title("departments.title"),
                        institution,
                        status,
                        Cards.card(Typography.subtitle("departments.list"), departments, actions),
                        Cards.card(Typography.subtitle("departments.create_title"), createBox),
                        Cards.card(Typography.subtitle("departments.users"), users));
    }

    // ---- data ------------------------------------------------------------

    /** Reload the institution, its departments and the user list. */
    public void refresh() {
        Optional<OrganizationUnit> unit = services.organization().findActive();
        institution.setText(
                unit.map(OrganizationUnit::name)
                        .orElseGet(() -> Messages.get("settings.no_institution")));

        if (unit.isEmpty()) {
            status.show(Banner.Severity.WARNING, "departments.no_institution", "departments.no_institution.hint");
            departments.setItems(FXCollections.emptyObservableList());
        } else {
            List<Department> list =
                    services.organization().listActiveDepartments(unit.get().id());
            departments.setItems(FXCollections.observableArrayList(list));
            if (list.isEmpty()) {
                status.show(Banner.Severity.INFO, "departments.empty", "departments.empty.hint");
            } else {
                status.hide();
            }
        }

        users.setItems(
                FXCollections.observableArrayList(
                        session.can(Permission.USER_MANAGE) ? services.users().list() : List.of()));
    }

    // ---- actions ---------------------------------------------------------

    private void create() {
        Optional<OrganizationUnit> unit = services.organization().findActive();
        if (unit.isEmpty()) {
            toasts.error("departments.no_institution");
            return;
        }
        if (newName.getText() == null || newName.getText().isBlank()) {
            toasts.error("departments.name_required");
            return;
        }
        if (newCode.getText() == null || newCode.getText().isBlank()) {
            toasts.error("departments.code_required");
            return;
        }

        String code = newCode.getText().strip().toUpperCase(java.util.Locale.ROOT);
        if (services.organization().findDepartmentByCode(unit.get().id(), code).isPresent()) {
            toasts.error("departments.code_taken");
            return;
        }

        try {
            services.organization()
                    .save(
                            Department.create(
                                    java.util.UUID.randomUUID().toString(),
                                    unit.get().id(),
                                    newName.getText().strip(),
                                    code,
                                    Instant.now()));
        } catch (IllegalArgumentException e) {
            // The domain rejects malformed codes; surfacing its complaint as a generic failure
            // would leave the operator guessing which of the two fields it meant.
            toasts.error("departments.code_invalid");
            return;
        }

        toasts.success("departments.created", newName.getText().strip());
        newName.clear();
        newCode.clear();
        refresh();
    }

    private void archive() {
        Department department = departments.getSelectionModel().getSelectedItem();
        if (department == null) {
            toasts.warning("departments.pick");
            return;
        }
        if (!department.isActive()) {
            toasts.info("departments.already_archived");
            return;
        }
        department.archive();
        services.organization().save(department);
        toasts.success("departments.archived_ok", department.name());
        refresh();
    }

    /** Role names, not ids: a column of UUIDs tells an administrator nothing. */
    private String roleNames(AppUser user) {
        List<String> names =
                services.roles().rolesOf(user.id()).stream()
                        .map(org.sirmax.domain.security.Role::name)
                        .sorted()
                        .toList();
        return names.isEmpty() ? "—" : String.join(", ", names);
    }

    // ---- helpers ---------------------------------------------------------

    private TableColumn<Department, String> col(
            String headerKey, double width, java.util.function.Function<Department, String> value) {
        TableColumn<Department, String> c = new TableColumn<>(Messages.get(headerKey));
        c.setPrefWidth(width);
        c.setCellValueFactory(x -> new SimpleStringProperty(value.apply(x.getValue())));
        return c;
    }

    private TableColumn<AppUser, String> userCol(
            String headerKey, double width, java.util.function.Function<AppUser, String> value) {
        TableColumn<AppUser, String> c = new TableColumn<>(Messages.get(headerKey));
        c.setPrefWidth(width);
        c.setCellValueFactory(x -> new SimpleStringProperty(value.apply(x.getValue())));
        return c;
    }

    /** Exposed for tests: how many departments the list holds. */
    public int departmentCount() {
        return departments.getItems().size();
    }
}
