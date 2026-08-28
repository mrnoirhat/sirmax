// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.app;

import javafx.application.Application;
import org.sirmax.ui.SirmaxApplication;

/**
 * Process entry point.
 *
 * <p>A plain (non-{@link Application}) {@code main} is the standard way to launch a modular JavaFX
 * app without fighting the module path. From Phase 3 this method builds the {@link CompositionRoot}
 * and passes it to the UI; for now it just shows the shell.
 */
public final class Launcher {

    private Launcher() {}

    public static void main(String[] args) {
        // TODO(Phase 3): CompositionRoot root = CompositionRoot.bootstrap(AppPaths.resolve());
        Application.launch(SirmaxApplication.class, args);
    }
}
