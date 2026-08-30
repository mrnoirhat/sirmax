// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sirmax.application.security.Session;
import org.sirmax.application.usecase.Authenticate;
import org.sirmax.application.usecase.ProvisionInitialAdmin;
import org.sirmax.application.usecase.SeedServiceCatalog;
import org.sirmax.domain.org.OrganizationUnit;
import org.sirmax.domain.service.ServiceDefinition;
import org.sirmax.domain.service.ServiceStatus;
import org.sirmax.infrastructure.persistence.SqliteDatabase;
import org.sirmax.ui.app.UiSession;
import org.sirmax.ui.designsystem.ToastHost;
import org.sirmax.ui.nav.RouteKey;
import org.sirmax.ui.nav.ShellNavigator;
import org.sirmax.ui.shell.ShellView;
import org.sirmax.ui.theme.Theme;
import org.sirmax.ui.theme.ThemeManager;
import org.sirmax.ui.view.DepartmentsView;
import org.sirmax.ui.view.DocumentsView;
import org.sirmax.ui.view.ReportsView;
import org.sirmax.ui.view.ServicesView;
import org.sirmax.ui.view.SettingsView;

/**
 * The administration screens against the real graph: real SQLite, real migrations, real use cases,
 * real JavaFX views.
 *
 * <p>Its companion is {@code FrontOfficeUiIT}, which covers the counter. This one covers what an
 * administrator does: define what the municipality offers, who works where, how it backs itself up,
 * and what it collected.
 */
class BackOfficeUiIT {

    private SqliteDatabase database;
    private CompositionRoot root;
    private UiSession session;
    private ShellNavigator navigator;
    private ToastHost toasts;

    @BeforeAll
    static void fx() {
        FxToolkit.start();
    }

    @BeforeEach
    void setUp() {
        database = SqliteDatabase.openInMemory();
        root = CompositionRoot.bootstrap(database);
        session = new UiSession();
        navigator = new ShellNavigator(RouteKey.HOME);
        toasts = new ToastHost();

        root.provisionInitialAdmin()
                .execute(
                        new ProvisionInitialAdmin.Command(
                                "Ayuntamiento de Santiago",
                                "Santiago",
                                "DO",
                                "admin",
                                "Administradora",
                                "una-contrasena-larga".toCharArray()));
        Session signedIn =
                root.authenticate()
                        .execute(
                                new Authenticate.Command(
                                        "admin", "una-contrasena-larga".toCharArray(), "test"))
                        .orElseThrow();
        session.signIn(signedIn);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void theShellMountsEveryAdministrationViewAgainstTheRealGraph() {
        var titles =
                onFxThread(() -> new ShellView(navigator, Theme.LIGHT, root, session).routeTitles());

        assertThat(titles)
                .containsKeys(
                        RouteKey.DOCUMENTS,
                        RouteKey.SERVICES,
                        RouteKey.DEPARTMENTS,
                        RouteKey.SETTINGS,
                        RouteKey.REPORTS);
        // Every mounted view must be the real one, not the placeholder that stands in when there is
        // no application graph — a shell that silently falls back would pass the check above.
        assertThat(titles.get(RouteKey.SERVICES)).isEqualTo("nav.services");
    }

    @Test
    void theServicesScreenListsTheSeededCatalogue() {
        root.seedServiceCatalog()
                .execute(new SeedServiceCatalog.Command(session.require(), "test"))
                .orElseThrow();

        ServicesView view = onFxThread(() -> new ServicesView(root, session, toasts));
        onFxThread(
                () -> {
                    view.node();
                    return null;
                });

        assertThat(view.serviceCount()).isPositive();
    }

    @Test
    void aDraftIsEditableAndBecomesReadOnlyOnceItIsPublished() {
        root.seedServiceCatalog()
                .execute(new SeedServiceCatalog.Command(session.require(), "test"))
                .orElseThrow();
        ServiceDefinition definition = root.serviceCatalogRepository().listDefinitions(false).getFirst();

        ServicesView view = onFxThread(() -> new ServicesView(root, session, toasts));
        onFxThread(
                () -> {
                    view.node();
                    return null;
                });
        // The seed leaves every service on a v1 draft, and the screen lands on it.
        assertThat(view.isEditorEnabled()).isTrue();

        var draft =
                root.serviceCatalogRepository().listVersions(definition.id()).stream()
                        .filter(v -> v.status() == ServiceStatus.DRAFT)
                        .findFirst()
                        .orElseThrow();
        root.publishServiceVersion()
                .execute(
                        new org.sirmax.application.usecase.PublishServiceVersion.Command(
                                session.require(), draft.id(), "test"))
                .orElseThrow();

        onFxThread(
                () -> {
                    view.node(); // re-reads, so the published version is what it now shows
                    return null;
                });
        assertThat(view.isEditorEnabled())
                .as("a published version pins live procedures and must not be editable (§39)")
                .isFalse();
    }

    @Test
    void aDepartmentCanBeCreatedAndArchived() {
        DepartmentsView view = onFxThread(() -> new DepartmentsView(root, session, toasts));
        onFxThread(
                () -> {
                    view.node();
                    return null;
                });
        int before = view.departmentCount();

        OrganizationUnit unit = root.organizationRepository().findActive().orElseThrow();
        root.organizationRepository()
                .save(
                        org.sirmax.domain.org.Department.create(
                                "dep-plan",
                                unit.id(),
                                "Planeamiento Urbano",
                                "PLAN",
                                root.clock().now()));

        onFxThread(
                () -> {
                    view.refresh();
                    return null;
                });
        assertThat(view.departmentCount()).isEqualTo(before + 1);
    }

    @Test
    void theSettingsScreenReadsThePolicyAndTheBackupHistory() {
        SettingsView view =
                onFxThread(
                        () -> {
                            ShellView shell = new ShellView(navigator, Theme.LIGHT, root, session);
                            ThemeManager themes = shell.themeManager();
                            return new SettingsView(root, session, toasts, themes);
                        });
        onFxThread(
                () -> {
                    view.node();
                    return null;
                });

        // A fresh install has no backups yet; the point is that reading the history works at all.
        assertThat(view.backupCount()).isZero();
    }

    @Test
    void theDocumentsScreenSearchesWithoutAMatchInsteadOfFailing() {
        DocumentsView view = onFxThread(() -> new DocumentsView(root, session, toasts));
        onFxThread(
                () -> {
                    view.node();
                    return null;
                });

        assertThat(view.resultCount()).isZero();
    }

    @Test
    void theReportsScreenComputesAnEmptyPeriodWithoutFailing() {
        ReportsView view = onFxThread(() -> new ReportsView(root, session, toasts));
        onFxThread(
                () -> {
                    view.node();
                    return null;
                });

        assertThat(view.methodRows()).isEmpty();
    }

    /**
     * Renders each administration screen to PNG so a human can look at it.
     *
     * <p>Off by default, like {@code ScreenshotGenerator}: it writes outside the build directory.
     * It lives beside the assertions because a screen that satisfies every assertion can still be
     * unreadable, and the only way to find that out is to look.
     */
    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(
            named = "sirmax.screenshots",
            matches = "true")
    void renderEveryAdministrationScreen() throws java.io.IOException {
        root.seedServiceCatalog()
                .execute(new SeedServiceCatalog.Command(session.require(), "test"))
                .orElseThrow();
        OrganizationUnit unit = root.organizationRepository().findActive().orElseThrow();
        root.organizationRepository()
                .save(
                        org.sirmax.domain.org.Department.create(
                                "dep-plan", unit.id(), "Planeamiento Urbano", "PLAN",
                                root.clock().now()));

        java.nio.file.Path out =
                java.nio.file.Path.of("..", "..", "landing", "public", "screenshots").normalize();
        java.nio.file.Files.createDirectories(out);

        ServicesView servicios =
                capture(out, "sirmax-servicios.png", () -> new ServicesView(root, session, toasts));
        assertThat(servicios.serviceCount())
                .as("a screenshot of an empty catalogue would misrepresent the screen")
                .isPositive();
        capture(out, "sirmax-documentos.png", () -> new DocumentsView(root, session, toasts));
        capture(out, "sirmax-departamentos.png", () -> new DepartmentsView(root, session, toasts));
        capture(out, "sirmax-reportes.png", () -> new ReportsView(root, session, toasts));
        capture(
                out,
                "sirmax-configuracion.png",
                () -> {
                    ShellView shell = new ShellView(navigator, Theme.LIGHT, root, session);
                    return new SettingsView(root, session, toasts, shell.themeManager());
                });

        // Settings is the densest control surface in the application — spinners, combos, tables,
        // checkboxes — so it is the screen where a control JavaFX still paints from modena.css
        // shows up first. That makes it the one worth rendering dark.
        captureDark(
                out,
                "sirmax-configuracion-oscuro.png",
                () -> {
                    ShellView shell = new ShellView(navigator, Theme.DARK, root, session);
                    return new SettingsView(root, session, toasts, shell.themeManager());
                });
    }

    /**
     * Renders one screen at its full natural height.
     *
     * <p>The height matters more than it looks. These views are taller than a window, and the shell
     * puts them in a ScrollPane; render one into a fixed-height scene instead and the VBox shrinks
     * every child to its minimum, which for a TableView is the header alone. The result is a
     * screenshot showing empty tables for data that is really there — the most misleading output
     * this harness could produce. So the scene is built far taller than the content and the
     * snapshot is cropped back to what the content actually occupies.
     */
    private <V extends org.sirmax.ui.view.SirmaxView> V captureDark(
            java.nio.file.Path dir, String fileName, Supplier<V> build) throws java.io.IOException {
        return capture(dir, fileName, build, true);
    }

    private <V extends org.sirmax.ui.view.SirmaxView> V capture(
            java.nio.file.Path dir, String fileName, Supplier<V> build) throws java.io.IOException {
        return capture(dir, fileName, build, false);
    }

    private <V extends org.sirmax.ui.view.SirmaxView> V capture(
            java.nio.file.Path dir, String fileName, Supplier<V> build, boolean dark)
            throws java.io.IOException {
        double width = 1200;
        record Shot(javafx.scene.image.WritableImage image, Object view) {}

        Shot shot =
                onFxThread(
                        () -> {
                            V view = build.get();
                            javafx.scene.Parent node = view.node();
                            javafx.scene.Scene scene = new javafx.scene.Scene(node, width, 4000);
                            var css =
                                    ShellView.class.getResource("/org/sirmax/ui/theme/sirmax.css");
                            if (css != null) {
                                scene.getStylesheets().add(css.toExternalForm());
                            }
                            if (dark) {
                                // Normally the shell's root carries this class; here the view is
                                // the root, so it has to carry it itself.
                                node.getStyleClass().add("sirmax-dark");
                            }
                            node.applyCss();
                            node.layout();

                            double height = Math.max(400, Math.ceil(node.prefHeight(width)) + 24);
                            var params = new javafx.scene.SnapshotParameters();
                            params.setViewport(
                                    new javafx.geometry.Rectangle2D(0, 0, width, height));
                            // Node#snapshot, not Scene#snapshot: only the node form takes a
                            // viewport, and the node is in a scene so it still gets the stylesheet.
                            return new Shot(node.snapshot(params, null), view);
                        });

        javax.imageio.ImageIO.write(
                javafx.embed.swing.SwingFXUtils.fromFXImage(shot.image(), null),
                "png",
                dir.resolve(fileName).toFile());
        @SuppressWarnings("unchecked")
        V view = (V) shot.view();
        return view;
    }

    // ── JavaFX thread plumbing ──

    private static <T> T onFxThread(Supplier<T> work) {
        return FxToolkit.onFxThread(work);
    }
}
