// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.app;

import javafx.application.Application;
import org.sirmax.ui.SirmaxApplication;

/**
 * Process entry point.
 *
 * <p>A plain (non-{@link Application}) {@code main} is the standard way to launch a modular JavaFX
 * app without fighting the module path. It opens the local database, applies pending migrations and
 * hands the wired graph to the UI, which shows the login (or first-run setup) screen.
 *
 * <p>The composition root is closed on JVM shutdown rather than after {@code launch} returns, so the
 * SQLite connection is released even if the window is closed while work is in flight.
 */
public final class Launcher {

    private Launcher() {}

    public static void main(String[] args) {
        CompositionRoot root = CompositionRoot.bootstrapDefault();
        Runtime.getRuntime().addShutdownHook(new Thread(root::close, "sirmax-shutdown"));

        SirmaxApplication.services(root);
        Application.launch(SirmaxApplication.class, args);
    }
}
