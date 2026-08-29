// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javafx.application.Platform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sirmax.application.security.Session;
import org.sirmax.application.usecase.Authenticate;
import org.sirmax.application.usecase.ProvisionInitialAdmin;
import org.sirmax.application.usecase.RegisterPerson;
import org.sirmax.application.usecase.StartProcedure;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.identity.Person;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.service.RequirementDef;
import org.sirmax.domain.service.RequirementStage;
import org.sirmax.domain.service.ServiceCategory;
import org.sirmax.domain.service.ServiceDefinition;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.domain.service.ServiceType;
import org.sirmax.domain.workflow.Transition;
import org.sirmax.domain.workflow.TransitionKind;
import org.sirmax.domain.workflow.WorkflowDefinition;
import org.sirmax.domain.workflow.WorkflowStep;
import org.sirmax.infrastructure.persistence.SqliteDatabase;
import org.sirmax.ui.app.UiSession;
import org.sirmax.ui.nav.RouteKey;
import org.sirmax.ui.nav.ShellNavigator;
import org.sirmax.ui.shell.ShellView;
import org.sirmax.ui.theme.Theme;
import org.sirmax.ui.view.CitizensView;
import org.sirmax.ui.view.ProcedureDetailView;
import org.sirmax.ui.view.ProceduresView;

/**
 * The Phase 5 front office against the real graph: real SQLite, real migrations, real use cases,
 * real JavaFX views. Nothing is faked, so a schema, wiring or permission mistake fails here.
 */
class FrontOfficeUiIT {

    private static boolean toolkitStarted;

    private SqliteDatabase database;
    private CompositionRoot root;
    private UiSession session;
    private ShellNavigator navigator;
    private String serviceId;
    private String personId;

    @BeforeAll
    static void fx() {
        if (!toolkitStarted) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            await(latch);
            toolkitStarted = true;
        }
    }

    @BeforeEach
    void setUp() {
        database = SqliteDatabase.openInMemory();
        root = CompositionRoot.bootstrap(database);
        session = new UiSession();
        navigator = new ShellNavigator(RouteKey.HOME);

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

        seedService(signedIn);
        personId = registerCitizen(signedIn);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    /** A free certificate with one requirement and a two-step workflow, published and ready. */
    private void seedService(Session admin) {
        var catalog = root.serviceCatalogRepository();
        var now = root.clock().now();

        catalog.saveCategory(ServiceCategory.create("cat-1", "CERT", "Certificaciones", 1, now));
        ServiceDefinition definition =
                ServiceDefinition.create(
                        "svc-1",
                        "CERT-RES",
                        "cat-1",
                        "Certificado de residencia",
                        ServiceType.GRATUITO,
                        "DO",
                        now);
        ServiceDefinitionVersion version = ServiceDefinitionVersion.draft("ver-1", "svc-1", 1, now);
        version.setRequirements(
                List.of(
                        RequirementDef.mandatoryDocument(
                                "cedula", "Cédula de identidad", RequirementStage.INTAKE)));
        version.setWorkflow(
                new WorkflowDefinition(
                        "recepcion",
                        List.of(
                                WorkflowStep.task("recepcion", "Recepción", "emision"),
                                new WorkflowStep(
                                        "emision",
                                        "Emisión",
                                        org.sirmax.domain.workflow.StepType.DOCUMENT_OUTPUT,
                                        Optional.empty(),
                                        1,
                                        List.of(Transition.terminal(TransitionKind.APPROVE))))));
        version.publish(now);
        definition.setCurrentVersion("ver-1", now);

        catalog.saveDefinition(definition);
        catalog.saveVersion(version);
        serviceId = definition.id();
    }

    private String registerCitizen(Session admin) {
        Person person =
                root.registerPerson()
                        .execute(
                                new RegisterPerson.Command(
                                        admin,
                                        "José Luis",
                                        "Peña Gómez",
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        "test"))
                        .orElseThrow();
        return person.id();
    }

    private Procedure openCase() {
        return root.startProcedure()
                .execute(
                        new StartProcedure.Command(
                                session.require(),
                                serviceId,
                                PartyRef.person(personId),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                "test"))
                .orElseThrow();
    }

    @Test
    void theShellMountsEveryFeatureViewAgainstTheRealGraph() {
        var titles =
                onFxThread(
                        () ->
                                new ShellView(navigator, Theme.LIGHT, root, session)
                                        .routeTitles());

        assertThat(titles)
                .containsKeys(
                        RouteKey.PROCEDURES,
                        RouteKey.PROCEDURE_NEW,
                        RouteKey.PROCEDURE_DETAIL,
                        RouteKey.CITIZENS);
    }

    @Test
    void theWorklistShowsAnOpenCaseAndDropsItOnceItIsClosed() {
        Procedure procedure = openCase();

        ProceduresView view =
                onFxThread(() -> new ProceduresView(root, session, navigator));
        onFxThread(
                () -> {
                    view.node();
                    return null;
                });
        assertThat(view.rowCount()).isEqualTo(1);

        procedure.close(root.clock().now());
        root.procedureRepository().save(procedure);

        onFxThread(
                () -> {
                    view.refresh();
                    return null;
                });
        assertThat(view.rowCount()).isZero();
    }

    @Test
    void theDetailViewRendersTheCaseItIsNavigatedTo() {
        Procedure procedure = openCase();

        ProcedureDetailView view =
                onFxThread(
                        () -> {
                            var toasts = new org.sirmax.ui.designsystem.ToastHost();
                            var detail = new ProcedureDetailView(root, session, navigator, toasts);
                            navigator.navigate(RouteKey.PROCEDURE_DETAIL, procedure.id());
                            detail.node();
                            return detail;
                        });

        assertThat(view.currentProcedure().map(Procedure::code)).contains(procedure.code());
    }

    @Test
    void citizenSearchIgnoresAccentsAndListsTheirCases() {
        Procedure procedure = openCase();

        CitizensView view = onFxThread(() -> new CitizensView(root, session, navigator));
        onFxThread(
                () -> {
                    view.node();
                    view.runSearch("pena"); // typed without the tilde, as at a real counter
                    return null;
                });
        assertThat(view.resultCount()).isEqualTo(1);

        onFxThread(
                () -> {
                    view.select(0);
                    return null;
                });
        assertThat(view.historyCount()).isEqualTo(1);
        assertThat(procedure.code()).startsWith("TRM-");
    }

    // ── JavaFX thread plumbing ──

    private static <T> T onFxThread(Supplier<T> work) {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<RuntimeException> failure = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(
                () -> {
                    try {
                        result.set(work.get());
                    } catch (RuntimeException e) {
                        failure.set(e);
                    } finally {
                        latch.countDown();
                    }
                });
        await(latch);
        if (failure.get() != null) {
            throw failure.get();
        }
        return result.get();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(20, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for the JavaFX thread");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
