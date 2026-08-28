// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.view;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.sirmax.ui.designsystem.Banner;
import org.sirmax.ui.designsystem.Buttons;
import org.sirmax.ui.designsystem.Cards;
import org.sirmax.ui.designsystem.FormField;
import org.sirmax.ui.designsystem.StatefulContent;
import org.sirmax.ui.designsystem.ToastHost;
import org.sirmax.ui.designsystem.Typography;
import org.sirmax.ui.nav.RouteKey;

/**
 * A visual gallery of every Design System component — a developer tool for manual review and living
 * documentation. Not shown in the task navigation (master prompt §1.2, §1.3); reached via
 * {@code Ctrl+Shift+G} or the Help menu.
 */
public final class StyleGuideView implements SirmaxView {

    private final ScrollPane root;

    public StyleGuideView(ToastHost toasts) {
        VBox content =
                new VBox(
                        22,
                        Typography.display("styleguide.title"),
                        Typography.muted("styleguide.intro"),
                        typographySection(),
                        buttonsSection(),
                        bannersSection(),
                        statesSection(),
                        formSection(),
                        toastsSection(toasts));
        content.setPadding(new Insets(4, 4, 24, 4));

        root = new ScrollPane(content);
        root.setFitToWidth(true);
        root.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    }

    @Override
    public RouteKey route() {
        return RouteKey.STYLEGUIDE;
    }

    @Override
    public String titleKey() {
        return "styleguide.title";
    }

    @Override
    public Parent node() {
        return root;
    }

    private static VBox typographySection() {
        return section(
                "styleguide.section.typography",
                Typography.display("styleguide.title"),
                Typography.title("styleguide.section.typography"),
                Typography.subtitle("styleguide.section.buttons"),
                Typography.body("styleguide.intro"),
                Typography.muted("styleguide.intro"));
    }

    private static VBox buttonsSection() {
        FlowPane row =
                new FlowPane(
                        10,
                        10,
                        Buttons.primary("action.save", () -> {}),
                        Buttons.secondary("action.cancel", () -> {}),
                        Buttons.danger("action.close", () -> {}),
                        Buttons.ghost("action.back", () -> {}));
        return section("styleguide.section.buttons", row);
    }

    private static VBox bannersSection() {
        return section(
                "styleguide.section.banners",
                new Banner(Banner.Severity.INFO, "placeholder.title", "placeholder.detail"),
                new Banner(Banner.Severity.SUCCESS, "state.success.title", null),
                new Banner(Banner.Severity.WARNING, "state.error.title", "state.error.detail"),
                new Banner(Banner.Severity.DANGER, "state.error.title", "state.error.detail"));
    }

    private static VBox statesSection() {
        StatefulContent loading = demoState(StatefulContent.State.LOADING);
        StatefulContent empty = new StatefulContent(new Region());
        empty.showEmpty("state.empty.title", "state.empty.detail");
        StatefulContent error = new StatefulContent(new Region());
        error.showError("java.lang.RuntimeException: demo stack (oculto por defecto)");

        FlowPane row = new FlowPane(16, 16, sized(loading), sized(empty), sized(error));
        return section("styleguide.section.states", row);
    }

    private static VBox formSection() {
        FormField field =
                new FormField(
                        "styleguide.sample.field", new TextField(), "styleguide.sample.hint");
        FormField invalid =
                new FormField("styleguide.sample.field", new TextField(), "styleguide.sample.hint");
        invalid.setError("styleguide.sample.error");
        return section("styleguide.section.form", field, invalid);
    }

    private static VBox toastsSection(ToastHost toasts) {
        FlowPane row =
                new FlowPane(
                        10,
                        10,
                        Buttons.secondary(
                                "styleguide.action.toast_info",
                                () -> toasts.info("styleguide.toast.demo")),
                        Buttons.secondary(
                                "styleguide.action.toast_success",
                                () -> toasts.success("styleguide.toast.demo")),
                        Buttons.secondary(
                                "styleguide.action.toast_error",
                                () -> toasts.error("styleguide.toast.demo")));
        return section("styleguide.section.toasts", row);
    }

    private static StatefulContent demoState(StatefulContent.State state) {
        StatefulContent c = new StatefulContent(new Region());
        c.show(state);
        return c;
    }

    private static Region sized(Region r) {
        r.setPrefSize(240, 200);
        r.setMinHeight(200);
        return r;
    }

    private static VBox section(String titleKey, javafx.scene.Node... items) {
        VBox box = new VBox(12);
        box.getChildren().add(Typography.title(titleKey));
        box.getChildren().add(Cards.card(items));
        return box;
    }
}
