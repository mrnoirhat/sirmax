// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.view;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.sirmax.application.usecase.AddProcedureNote;
import org.sirmax.application.usecase.AdvanceProcedure;
import org.sirmax.application.usecase.SaveProcedureForm;
import org.sirmax.application.usecase.UpdateProcedureRequirement;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.procedure.ProcedureChecklist;
import org.sirmax.domain.procedure.ProcedureEvent;
import org.sirmax.domain.procedure.ProcedureRequirementItem;
import org.sirmax.domain.security.Permission;
import org.sirmax.domain.service.FormField;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.domain.workflow.Transition;
import org.sirmax.domain.workflow.TransitionKind;
import org.sirmax.domain.workflow.WorkflowEngine;
import org.sirmax.shared.Result;
import org.sirmax.ui.app.AppServices;
import org.sirmax.ui.app.UiSession;
import org.sirmax.ui.designsystem.Banner;
import org.sirmax.ui.designsystem.Buttons;
import org.sirmax.ui.designsystem.Cards;
import org.sirmax.ui.designsystem.Styles;
import org.sirmax.ui.designsystem.ToastHost;
import org.sirmax.ui.designsystem.Typography;
import org.sirmax.ui.i18n.Messages;
import org.sirmax.ui.nav.Navigator;
import org.sirmax.ui.nav.RouteKey;

/**
 * One case, everything about it (master prompt §16, §56, §58).
 *
 * <p>Layout follows what the operator does, top to bottom: what is missing, the case's own fields,
 * what can be done next, and the history. The blocker banner is first because it answers the only
 * question that matters at the counter — "can I finish this now, and if not, what does the citizen
 * still have to bring?"
 *
 * <p>Available actions come from the workflow engine, not from a fixed button row: a case only
 * offers the transitions its service version actually declares, filtered by what this operator is
 * allowed to do. There is no disabled button explaining why it is disabled.
 */
public final class ProcedureDetailView implements SirmaxView {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final AppServices services;
    private final UiSession session;
    private final Navigator navigator;
    private final ToastHost toasts;

    private final VBox root = new VBox(20);
    private final Label heading = new Label();
    private final Label subheading = new Label();
    private final Banner blockers = new Banner();
    private final VBox checklistBox = new VBox(6);
    private final VBox formBox = new VBox(10);
    private final FlowPane actions = new FlowPane(8, 8);
    private final VBox timeline = new VBox(6);
    private final Map<String, TextField> formFields = new LinkedHashMap<>();

    private Procedure procedure;

    public ProcedureDetailView(
            AppServices services, UiSession session, Navigator navigator, ToastHost toasts) {
        this.services = services;
        this.session = session;
        this.navigator = navigator;
        this.toasts = toasts;
        build();
    }

    @Override
    public RouteKey route() {
        return RouteKey.PROCEDURE_DETAIL;
    }

    @Override
    public String titleKey() {
        return "procedure.detail.title";
    }

    @Override
    public Parent node() {
        navigator.argument().ifPresent(this::load);
        return root;
    }

    /** Load and render a case by id. Public so tests and deep links can drive it. */
    public void load(String procedureId) {
        Optional<Procedure> found = services.procedures().findById(procedureId);
        if (found.isEmpty()) {
            toasts.error("procedure.not_found");
            navigator.navigate(RouteKey.PROCEDURES);
            return;
        }
        this.procedure = found.get();
        render();
    }

    private void build() {
        heading.getStyleClass().add(Styles.TITLE);
        subheading.getStyleClass().add(Styles.MUTED);

        root.getChildren()
                .addAll(
                        new VBox(4, heading, subheading),
                        blockers,
                        Cards.card(Typography.subtitle("procedure.checklist"), checklistBox),
                        Cards.card(Typography.subtitle("procedure.form"), formBox),
                        Cards.card(Typography.subtitle("procedure.actions"), actions),
                        Cards.card(Typography.subtitle("procedure.timeline"), timeline));
    }

    private void render() {
        renderHeader();
        renderChecklist();
        renderForm();
        renderActions();
        renderTimeline();
    }

    private void renderHeader() {
        String serviceName =
                services.serviceCatalog()
                        .findDefinitionById(procedure.serviceDefinitionId())
                        .map(d -> d.name())
                        .orElse("");
        heading.setText(procedure.code() + "  ·  " + serviceName);

        String status =
                Messages.get(
                        "procedure.status." + procedure.status().name().toLowerCase(Locale.ROOT));
        String step = procedure.currentStepKey().map(s -> "  ·  " + s).orElse("");
        String due =
                procedure.dueDate()
                        .map(d -> "  ·  " + Messages.get("procedure.due", d.toString()))
                        .orElse("");
        subheading.setText(status + step + due);
    }

    private void renderChecklist() {
        checklistBox.getChildren().clear();
        List<ProcedureRequirementItem> items = services.procedures().findRequirements(procedure.id());
        ProcedureChecklist checklist = ProcedureChecklist.of(items, formVariables());

        if (items.isEmpty()) {
            checklistBox.getChildren().add(Typography.muted("procedure.checklist.none"));
        }
        for (ProcedureRequirementItem item : items) {
            if (!checklist.applies(item)) {
                continue; // a condition that does not hold: not this citizen's problem
            }
            checklistBox.getChildren().add(checklistRow(item));
        }

        if (checklist.isComplete()) {
            blockers.hide();
        } else {
            blockers.show(
                    Banner.Severity.WARNING,
                    "procedure.blocked",
                    "procedure.blocked.detail",
                    checklist.pendingCount());
        }
    }

    private HBox checklistRow(ProcedureRequirementItem item) {
        CheckBox box = new CheckBox(item.label() + (item.required() ? " *" : ""));
        box.setSelected(item.isSatisfied());
        box.setDisable(!session.can(Permission.PROCEDURE_WORK) || procedure.status().isTerminal());
        box.setOnAction(
                e ->
                        updateRequirement(
                                item.requirementKey(),
                                box.isSelected()
                                        ? UpdateProcedureRequirement.Action.SATISFY
                                        : UpdateProcedureRequirement.Action.UNSATISFY,
                                null));

        HBox row = new HBox(10, box, spacer());
        row.setAlignment(Pos.CENTER_LEFT);

        if (item.isWaived()) {
            row.getChildren()
                    .add(Typography.rawMuted(Messages.get("procedure.waived", item.note().orElse(""))));
        } else if (item.isPending()
                && session.can(Permission.PROCEDURE_DECIDE)
                && !procedure.status().isTerminal()) {
            row.getChildren().add(Buttons.ghost("procedure.waive", () -> promptWaiver(item)));
        }
        return row;
    }

    private void promptWaiver(ProcedureRequirementItem item) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(Messages.get("procedure.waive"));
        dialog.setHeaderText(item.label());
        dialog.setContentText(Messages.get("procedure.waive.reason"));
        dialog.showAndWait()
                .filter(reason -> !reason.isBlank())
                .ifPresent(
                        reason ->
                                updateRequirement(
                                        item.requirementKey(),
                                        UpdateProcedureRequirement.Action.WAIVE,
                                        reason));
    }

    private void updateRequirement(
            String key, UpdateProcedureRequirement.Action action, String note) {
        Result<ProcedureChecklist> result =
                services
                        .updateProcedureRequirement()
                        .execute(
                                new UpdateProcedureRequirement.Command(
                                        session.require(),
                                        procedure.id(),
                                        key,
                                        action,
                                        Optional.ofNullable(note),
                                        "desktop.procedure_detail"));
        if (result instanceof Result.Err<ProcedureChecklist> err) {
            toasts.error(err.messageKey());
        }
        load(procedure.id());
    }

    private void renderForm() {
        formBox.getChildren().clear();
        formFields.clear();

        Optional<ServiceDefinitionVersion> version =
                services.serviceCatalog().findVersionById(procedure.serviceVersionId());
        if (version.isEmpty() || version.get().formSchema().isEmpty()) {
            formBox.getChildren().add(Typography.muted("procedure.form.none"));
            return;
        }

        Map<String, String> values = services.procedures().findFormValues(procedure.id());
        for (FormField field : version.get().formSchema().fields()) {
            TextField input = new TextField(values.getOrDefault(field.key(), ""));
            input.setDisable(procedure.status().isTerminal());
            formFields.put(field.key(), input);
            // The label is administrator-authored data, not program text.
            formBox.getChildren()
                    .add(
                            org.sirmax.ui.designsystem.FormField.withLiteralLabel(
                                    field.label() + (field.required() ? " *" : ""), input));
        }
        if (!procedure.status().isTerminal() && session.can(Permission.PROCEDURE_WORK)) {
            formBox.getChildren().add(Buttons.secondary("action.save", this::saveForm));
        }
    }

    private void saveForm() {
        Map<String, String> values = new LinkedHashMap<>();
        formFields.forEach((key, field) -> values.put(key, field.getText()));

        Result<List<String>> result =
                services
                        .saveProcedureForm()
                        .execute(
                                new SaveProcedureForm.Command(
                                        session.require(),
                                        procedure.id(),
                                        values,
                                        "desktop.procedure_detail"));
        if (result instanceof Result.Err<List<String>> err) {
            toasts.error(err.messageKey());
            return;
        }
        List<String> problems = result.orElseThrow();
        if (problems.isEmpty()) {
            toasts.success("procedure.form.saved");
        } else {
            // Problem keys carry the offending field after a colon; show the first plainly.
            toasts.error(problems.get(0).split(":", 2)[0]);
        }
        load(procedure.id());
    }

    private void renderActions() {
        actions.getChildren().clear();
        if (procedure.status().isTerminal()) {
            actions.getChildren().add(Typography.muted("procedure.closed"));
            return;
        }

        Optional<ServiceDefinitionVersion> version =
                services.serviceCatalog().findVersionById(procedure.serviceVersionId());
        String stepKey = procedure.currentStepKey().orElse(null);
        if (version.isEmpty() || stepKey == null) {
            actions.getChildren().add(Typography.muted("procedure.no_workflow"));
            return;
        }

        List<Transition> available =
                WorkflowEngine.availableTransitions(
                        version.get().workflow(), stepKey, formVariables());
        List<Button> buttons = new ArrayList<>();
        for (Transition transition : available) {
            if (!mayPerform(transition.kind())) {
                continue;
            }
            String labelKey = "procedure.transition." + transition.kind().name().toLowerCase(Locale.ROOT);
            buttons.add(
                    transition.kind() == TransitionKind.ADVANCE
                                    || transition.kind() == TransitionKind.APPROVE
                            ? Buttons.primary(labelKey, () -> advance(transition.kind()))
                            : Buttons.secondary(labelKey, () -> advance(transition.kind())));
        }
        if (buttons.isEmpty()) {
            actions.getChildren().add(Typography.muted("procedure.no_actions"));
        } else {
            actions.getChildren().addAll(buttons);
        }
        actions.getChildren().add(Buttons.ghost("procedure.add_note", this::promptNote));
    }

    private boolean mayPerform(TransitionKind kind) {
        boolean decision =
                kind == TransitionKind.APPROVE
                        || kind == TransitionKind.REJECT
                        || kind == TransitionKind.CANCEL;
        return decision
                ? session.can(Permission.PROCEDURE_DECIDE)
                : session.can(Permission.PROCEDURE_WORK);
    }

    private void advance(TransitionKind kind) {
        Optional<String> reason = Optional.empty();
        if (kind == TransitionKind.REJECT || kind == TransitionKind.CANCEL) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle(Messages.get("procedure.transition." + kind.name().toLowerCase(Locale.ROOT)));
            dialog.setContentText(Messages.get("procedure.reason"));
            reason = dialog.showAndWait().filter(r -> !r.isBlank());
            if (reason.isEmpty()) {
                return; // the operator backed out of the dialog
            }
        }

        Result<Procedure> result =
                services
                        .advanceProcedure()
                        .execute(
                                new AdvanceProcedure.Command(
                                        session.require(),
                                        procedure.id(),
                                        kind,
                                        reason,
                                        "desktop.procedure_detail"));
        if (result instanceof Result.Err<Procedure> err) {
            toasts.error(err.messageKey());
        } else {
            toasts.success("procedure.advanced");
        }
        load(procedure.id());
    }

    private void promptNote() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(Messages.get("procedure.add_note"));
        dialog.setContentText(Messages.get("procedure.note"));
        dialog.getDialogPane().setContent(new TextArea());
        dialog.showAndWait()
                .filter(text -> !text.isBlank())
                .ifPresent(
                        text -> {
                            services
                                    .addProcedureNote()
                                    .execute(
                                            new AddProcedureNote.Command(
                                                    session.require(),
                                                    procedure.id(),
                                                    text,
                                                    "desktop.procedure_detail"));
                            load(procedure.id());
                        });
    }

    private void renderTimeline() {
        timeline.getChildren().clear();
        List<ProcedureEvent> events = services.procedures().findEvents(procedure.id());
        if (events.isEmpty()) {
            timeline.getChildren().add(Typography.muted("procedure.timeline.empty"));
            return;
        }
        // Newest first: the last thing that happened is what the operator is looking for.
        for (int i = events.size() - 1; i >= 0; i--) {
            ProcedureEvent event = events.get(i);
            String kind =
                    Messages.get("procedure.event." + event.kind().name().toLowerCase(Locale.ROOT));
            String detail = event.detail().map(d -> " — " + d).orElse("");
            timeline.getChildren()
                    .add(
                            Typography.rawMuted(
                                    TIMESTAMP.format(event.occurredAt()) + "  ·  " + kind + detail));
        }
    }

    /** Form answers as rule variables, so conditional requirements re-evaluate as the case fills in. */
    private Map<String, Object> formVariables() {
        Map<String, Object> vars = new LinkedHashMap<>();
        services.procedures().findFormValues(procedure.id()).forEach(vars::put);
        return vars;
    }

    /** Exposed for tests: the case currently on screen. */
    public Optional<Procedure> currentProcedure() {
        return Optional.ofNullable(procedure);
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }
}
