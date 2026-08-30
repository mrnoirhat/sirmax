// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sirmax.application.security.Session;
import org.sirmax.application.usecase.Authenticate;
import org.sirmax.application.usecase.ProvisionInitialAdmin;
import org.sirmax.application.usecase.RegisterPerson;
import org.sirmax.application.usecase.SeedServiceCatalog;
import org.sirmax.application.usecase.SetServiceAvailability;
import org.sirmax.domain.common.ArchiveStatus;
import org.sirmax.domain.service.ServiceDefinition;
import org.sirmax.infrastructure.persistence.SqliteDatabase;
import org.sirmax.ui.app.UiSession;
import org.sirmax.ui.designsystem.ToastHost;
import org.sirmax.ui.nav.RouteKey;
import org.sirmax.ui.nav.ShellNavigator;
import org.sirmax.ui.view.CitizensView;
import org.sirmax.ui.view.NewProcedureView;
import org.sirmax.ui.view.ServicesView;

/**
 * The defects reported from actually using the packaged application.
 *
 * <p>Each test is one thing an operator said was broken, written before the fix so it fails for the
 * reported reason. They live together because they came from one session at the counter, and
 * keeping them together makes it obvious what that session cost.
 */
class ReportedDefectsIT {

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
                                "Ayuntamiento de Santiago", "Santiago", "DO",
                                "admin", "Administradora", "una-contrasena-larga".toCharArray()));
        session.signIn(
                root.authenticate()
                        .execute(
                                new Authenticate.Command(
                                        "admin", "una-contrasena-larga".toCharArray(), "test"))
                        .orElseThrow());
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    private void seed() {
        root.seedServiceCatalog()
                .execute(new SeedServiceCatalog.Command(session.require(), "test"))
                .orElseThrow();
    }

    /**
     * "Cargá el catálogo base y carga, pero no se guarda."
     *
     * <p>Reads the catalogue back through a repository built on the same database, which is what
     * reopening the screen does.
     */
    @Test
    void theSeededCatalogueIsStillThereWhenTheScreenIsReopened() {
        seed();
        int afterSeeding = root.serviceCatalogRepository().listDefinitions(false).size();
        assertThat(afterSeeding).as("el catálogo base debe cargar servicios").isPositive();

        ServicesView reopened = FxToolkit.onFxThread(() -> new ServicesView(root, session, toasts));
        FxToolkit.onFxThread(
                () -> {
                    reopened.node();
                    return null;
                });
        assertThat(reopened.serviceCount()).isEqualTo(afterSeeding);
    }

    /**
     * "En trámites, cuando registro nuevo trámite y le doy a la barra de servicios, no cargan los
     * servicios predeterminados."
     *
     * <p>The seed produces drafts, and a draft genuinely cannot take a case: it has no reviewed fee
     * and no published terms. Auto-publishing the base catalogue would put a hundred services with
     * a zero fee into service, which is a worse failure than an empty list.
     *
     * <p>So the fix is not to loosen the rule but to make a published service reachable in one
     * step. Once any service is published, the screen must offer it.
     */
    @Test
    void theNewProcedureScreenOffersEveryPublishedService() {
        seed();

        NewProcedureView before =
                FxToolkit.onFxThread(() -> new NewProcedureView(root, session, navigator, toasts));
        FxToolkit.onFxThread(
                () -> {
                    before.node();
                    return null;
                });
        assertThat(before.availableServiceCount())
                .as("un borrador no puede tomar un caso")
                .isZero();

        var draft =
                root.serviceCatalogRepository()
                        .listVersions(
                                root.serviceCatalogRepository().listDefinitions(false).getFirst().id())
                        .getFirst();
        root.publishServiceVersion()
                .execute(
                        new org.sirmax.application.usecase.PublishServiceVersion.Command(
                                session.require(), draft.id(), "test"))
                .orElseThrow();

        NewProcedureView after =
                FxToolkit.onFxThread(() -> new NewProcedureView(root, session, navigator, toasts));
        FxToolkit.onFxThread(
                () -> {
                    after.node();
                    return null;
                });
        assertThat(after.availableServiceCount())
                .as("publicado, el servicio debe poder elegirse")
                .isPositive();
    }

    /**
     * "Ciudadanos, solo cargan los que busco, pero no cargan ninguno en anterior. Deberían cargar
     * por defecto en orden alfabético."
     */
    @Test
    void theCitizensScreenListsEveryoneBeforeAnySearch() {
        for (String[] who : List.of(
                new String[] {"Zoraida", "Almonte"},
                new String[] {"Ana", "Bencosme"},
                new String[] {"Carlos", "Abreu"})) {
            root.registerPerson()
                    .execute(
                            new RegisterPerson.Command(
                                    session.require(), who[0], who[1],
                                    Optional.empty(), Optional.empty(), Optional.empty(),
                                    Optional.empty(), Optional.empty(), "test"))
                    .orElseThrow();
        }

        CitizensView view = FxToolkit.onFxThread(() -> new CitizensView(root, session, navigator));
        FxToolkit.onFxThread(
                () -> {
                    view.node();
                    return null;
                });

        assertThat(view.resultCount())
                .as("la lista debe traer a los ciudadanos sin buscar nada")
                .isEqualTo(3);
        assertThat(view.displayedNames())
                .as("y en orden alfabético")
                .isSorted();
    }

    /**
     * "Algunos se seleccionan para activar y no se desactiva al darle la segunda vez."
     *
     * <p>The screen decided the toggle from {@code isAvailable()}, which is false for a service with
     * no published version no matter what the flag says — so pressing it appeared to do nothing and
     * pressing again asked for the same thing.
     */
    @Test
    void activatingAndDeactivatingAServiceActuallyToggles() {
        seed();
        ServiceDefinition definition =
                root.serviceCatalogRepository().listDefinitions(false).getFirst();
        assertThat(definition.archiveStatus()).isEqualTo(ArchiveStatus.ACTIVE);

        root.setServiceAvailability()
                .execute(
                        new SetServiceAvailability.Command(
                                session.require(), definition.id(), false, "test"))
                .orElseThrow();
        assertThat(archiveStatusOf(definition.id()))
                .as("desactivar debe archivar")
                .isEqualTo(ArchiveStatus.ARCHIVED);

        root.setServiceAvailability()
                .execute(
                        new SetServiceAvailability.Command(
                                session.require(), definition.id(), true, "test"))
                .orElseThrow();
        assertThat(archiveStatusOf(definition.id()))
                .as("y volver a activar debe restaurarlo")
                .isEqualTo(ArchiveStatus.ACTIVE);
    }

    private ArchiveStatus archiveStatusOf(String definitionId) {
        return root.serviceCatalogRepository()
                .findDefinitionById(definitionId)
                .orElseThrow()
                .archiveStatus();
    }
}
