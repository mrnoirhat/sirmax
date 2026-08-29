// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.app;

import org.sirmax.application.port.ProcedureRepository;
import org.sirmax.application.port.ServiceCatalogRepository;
import org.sirmax.application.port.UserRepository;
import org.sirmax.application.usecase.AddProcedureNote;
import org.sirmax.application.usecase.AdvanceProcedure;
import org.sirmax.application.usecase.AssignProcedure;
import org.sirmax.application.usecase.Authenticate;
import org.sirmax.application.usecase.FindDuplicatePeople;
import org.sirmax.application.usecase.ProvisionInitialAdmin;
import org.sirmax.application.usecase.RegisterPerson;
import org.sirmax.application.usecase.SaveProcedureForm;
import org.sirmax.application.usecase.SeedServiceCatalog;
import org.sirmax.application.usecase.StartProcedure;
import org.sirmax.application.usecase.UpdateProcedureRequirement;

/**
 * What the UI is allowed to reach for.
 *
 * <p>The shell and its views are constructed against this interface, not against the composition
 * root: {@code sirmax-ui} must not know that SQLite, Jackson or a {@code CompositionRoot} exist. The
 * composition root in {@code sirmax-app} implements it, which keeps the dependency arrow pointing
 * the way ArchUnit enforces (ADR 0005).
 *
 * <p>Read-only repositories appear here too. Listing the service catalog or a worklist is a query,
 * not a state change, and wrapping every list in a use case would add a layer that only forwards.
 */
public interface AppServices {

    // ── identity / bootstrap ──
    boolean needsInitialSetup();

    ProvisionInitialAdmin provisionInitialAdmin();

    Authenticate authenticate();

    // ── citizens ──
    RegisterPerson registerPerson();

    FindDuplicatePeople findDuplicatePeople();

    org.sirmax.application.port.PersonRepository people();

    // ── service catalog ──
    ServiceCatalogRepository serviceCatalog();

    SeedServiceCatalog seedServiceCatalog();

    // ── cases ──
    StartProcedure startProcedure();

    UpdateProcedureRequirement updateProcedureRequirement();

    SaveProcedureForm saveProcedureForm();

    AdvanceProcedure advanceProcedure();

    AssignProcedure assignProcedure();

    AddProcedureNote addProcedureNote();

    ProcedureRepository procedures();

    UserRepository users();
}
