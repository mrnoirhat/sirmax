// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.shell;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.sirmax.ui.designsystem.Styles;
import org.sirmax.ui.designsystem.ToastHost;
import org.sirmax.ui.i18n.Messages;
import org.sirmax.ui.nav.NavItem;
import org.sirmax.ui.nav.Navigator;
import org.sirmax.ui.nav.RouteKey;
import org.sirmax.ui.view.DashboardView;
import org.sirmax.ui.view.GlobalSearchView;
import org.sirmax.ui.view.HomeView;
import org.sirmax.ui.view.PlaceholderView;
import org.sirmax.ui.view.SirmaxView;

/**
 * The application shell: top bar (brand + global search + user), task-first navigation, a content
 * host driven by the {@link Navigator}, and a toast overlay.
 *
 * <p>The shell is a {@link StackPane}: the framed {@link BorderPane} plus a mouse-transparent
 * {@link ToastHost} on top.
 */
public final class ShellView extends StackPane {

    private final Navigator navigator;
    private final Map<RouteKey, SirmaxView> views = new EnumMap<>(RouteKey.class);
    private final Map<RouteKey, Button> navButtons = new EnumMap<>(RouteKey.class);
    private final StackPane contentHost = new StackPane();
    private final Label breadcrumb = new Label();
    private final TextField search = new TextField();
    private final ToastHost toasts = new ToastHost();
    private final GlobalSearchView searchView = new GlobalSearchView();

    public ShellView(Navigator navigator) {
        this.navigator = navigator;
        getStyleClass().add(Styles.SHELL);

        registerViews();

        BorderPane frame = new BorderPane();
        frame.setTop(buildTopBar());
        frame.setLeft(buildTaskNav());
        frame.setCenter(buildContentArea());

        getChildren().addAll(frame, toasts);

        navigator.addListener(this::showRoute);
        showRoute(navigator.current());
    }

    /** Toast/notification surface, for views and the composition root. */
    public ToastHost notifications() {
        return toasts;
    }

    /** Move keyboard focus to the global search field (Ctrl+K). */
    public void focusSearch() {
        search.requestFocus();
        search.selectAll();
    }

    public void goHome() {
        navigator.navigate(RouteKey.HOME);
    }

    // ---- construction ----------------------------------------------------

    private void registerViews() {
        put(new HomeView(navigator));
        put(new DashboardView());
        put(searchView);
        put(new PlaceholderView(RouteKey.PROCEDURES, "nav.procedures"));
        put(new PlaceholderView(RouteKey.BILLING, "nav.billing"));
        put(new PlaceholderView(RouteKey.CASH, "nav.cash"));
        put(new PlaceholderView(RouteKey.DOCUMENTS, "nav.documents"));
        put(new PlaceholderView(RouteKey.CITIZENS, "nav.citizens"));
        put(new PlaceholderView(RouteKey.DEPARTMENTS, "nav.departments"));
        put(new PlaceholderView(RouteKey.SETTINGS, "nav.settings"));
        put(new PlaceholderView(RouteKey.REPORTS, "nav.reports"));
    }

    private void put(SirmaxView view) {
        views.put(view.route(), view);
    }

    private HBox buildTopBar() {
        Label brand = new Label(Messages.get("app.brand"));
        brand.getStyleClass().add(Styles.BRAND);

        search.setPromptText(Messages.get("shell.search.prompt"));
        search.getStyleClass().add(Styles.GLOBAL_SEARCH);
        search.setOnAction(e -> submitSearch());
        HBox.setHgrow(search, Priority.ALWAYS);

        Label user = new Label(Messages.get("shell.user.placeholder"));
        user.getStyleClass().add(Styles.MUTED);

        HBox bar = new HBox(16, brand, search, user);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add(Styles.TOPBAR);
        return bar;
    }

    private ScrollPane buildTaskNav() {
        VBox nav = new VBox(2);
        nav.getStyleClass().add(Styles.TASKNAV);

        NavItem.Section currentSection = null;
        for (NavItem item : NavItem.defaults()) {
            if (item.section() != currentSection) {
                currentSection = item.section();
                Label header = new Label(Messages.get(currentSection.labelKey()));
                header.getStyleClass().add(Styles.NAV_SECTION);
                nav.getChildren().add(header);
            }
            Button b = new Button(Messages.get(item.labelKey()));
            b.getStyleClass().add(Styles.NAV_ITEM);
            b.setMaxWidth(Double.MAX_VALUE);
            b.setAlignment(Pos.CENTER_LEFT);
            b.setOnAction(e -> navigator.navigate(item.key()));
            navButtons.put(item.key(), b);
            nav.getChildren().add(b);
        }

        ScrollPane scroll = new ScrollPane(nav);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add(Styles.TASKNAV);
        return scroll;
    }

    private VBox buildContentArea() {
        breadcrumb.getStyleClass().add(Styles.BREADCRUMB);
        VBox.setVgrow(contentHost, Priority.ALWAYS);

        VBox area = new VBox(12, breadcrumb, contentHost);
        area.getStyleClass().add(Styles.CONTENT);
        area.setPadding(new Insets(24, 28, 28, 28));
        return area;
    }

    // ---- navigation ----------------------------------------------------

    private void submitSearch() {
        searchView.query(search.getText());
        navigator.navigate(RouteKey.SEARCH);
    }

    private void showRoute(RouteKey route) {
        SirmaxView view = views.getOrDefault(route, views.get(RouteKey.HOME));
        Parent node = view.node();

        contentHost.getChildren().setAll(node);
        breadcrumb.setText(
                Messages.get("app.brand") + "  ›  " + Messages.get(view.titleKey()));

        navButtons.forEach(
                (key, button) -> {
                    boolean selected = key == route;
                    button.getStyleClass().remove(Styles.SELECTED);
                    if (selected) {
                        button.getStyleClass().add(Styles.SELECTED);
                    }
                });
    }

    /** Exposed for tests: the registered routes and their view titles. */
    public Map<RouteKey, String> routeTitles() {
        Map<RouteKey, String> out = new LinkedHashMap<>();
        views.forEach((k, v) -> out.put(k, v.titleKey()));
        return out;
    }
}
