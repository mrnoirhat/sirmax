// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.shell;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * The application shell: top bar, task navigation and the task-first home screen.
 *
 * <p>This is a Phase 1 placeholder that establishes the layout the design system (Phase 2) will
 * refine. Every string here is provisional and will move to i18n bundles.
 */
public final class ShellView extends BorderPane {

    public ShellView() {
        getStyleClass().add("sirmax-shell");
        setTop(buildTopBar());
        setLeft(buildTaskNav());
        setCenter(buildHome());
    }

    private HBox buildTopBar() {
        Label brand = new Label("SIRMAX");
        brand.getStyleClass().add("sirmax-brand");

        TextField search = new TextField();
        search.setPromptText("Búsqueda global  (Ctrl+K)");
        search.getStyleClass().add("sirmax-global-search");
        HBox.setHgrow(search, Priority.ALWAYS);

        Label user = new Label("Usuaria · Caja");

        HBox bar = new HBox(16, brand, search, user);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 16, 10, 16));
        bar.getStyleClass().add("sirmax-topbar");
        return bar;
    }

    private VBox buildTaskNav() {
        VBox nav =
                new VBox(
                        4,
                        navItem("Inicio"),
                        navItem("Trámites"),
                        navItem("Facturación"),
                        navItem("Caja"),
                        navItem("Documentos"),
                        navItem("Ciudadanos"),
                        separator(),
                        navItem("Departamentos"),
                        navItem("Configuración"),
                        navItem("Reportes"));
        nav.setPadding(new Insets(16, 12, 16, 12));
        nav.setPrefWidth(220);
        nav.getStyleClass().add("sirmax-tasknav");
        return nav;
    }

    private VBox buildHome() {
        Label question = new Label("¿Qué necesitas hacer?");
        question.getStyleClass().add("sirmax-home-question");

        FlowPane tasks =
                new FlowPane(
                        16,
                        16,
                        taskCard("Registrar un trámite"),
                        taskCard("Emitir una certificación"),
                        taskCard("Registrar un pago"),
                        taskCard("Registrar un documento"),
                        taskCard("Registrar una solicitud/queja"),
                        taskCard("Gestionar un contrato"));
        tasks.setPrefWrapLength(720);

        VBox home = new VBox(24, question, tasks);
        home.setPadding(new Insets(40));
        home.setAlignment(Pos.TOP_LEFT);
        home.getStyleClass().add("sirmax-home");
        return home;
    }

    private static Button navItem(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.getStyleClass().add("sirmax-navitem");
        return b;
    }

    private static Button taskCard(String text) {
        Button b = new Button(text);
        b.setPrefSize(230, 96);
        b.getStyleClass().add("sirmax-taskcard");
        return b;
    }

    private static Region separator() {
        Region r = new Region();
        r.setPrefHeight(12);
        return r;
    }
}
