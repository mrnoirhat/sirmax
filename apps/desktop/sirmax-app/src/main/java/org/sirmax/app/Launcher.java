// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.app;

import javafx.application.Application;
import org.sirmax.ui.SirmaxApplication;

/**
 * Process entry point.
 *
 * <p>A plain (non-{@link Application}) {@code main} is the standard way to launch a modular JavaFX
 * app without fighting the module path. The domain graph is built by {@link
 * CompositionRoot#bootstrapDefault()}; Phase 5 hands it to the UI (login / first-run setup). For now
 * the shell renders on its own.
 */
public final class Launcher {

    private Launcher() {}

    public static void main(String[] args) {
        // Phase 5: try (CompositionRoot root = CompositionRoot.bootstrapDefault()) { ... }
        Application.launch(SirmaxApplication.class, args);
    }
}
