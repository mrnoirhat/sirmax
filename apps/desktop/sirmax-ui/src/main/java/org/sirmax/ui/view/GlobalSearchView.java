// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.view;

import java.util.List;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import org.sirmax.ui.designsystem.StatefulContent;
import org.sirmax.ui.designsystem.Typography;
import org.sirmax.ui.nav.RouteKey;

/**
 * The global-search results screen (Ctrl+K). Phase 2 provides the categorized-results shell with an
 * empty state; the index and live results arrive in Phase 5 (master prompt §32).
 */
public final class GlobalSearchView implements SirmaxView {

    /** The result categories the search will group by. */
    public static final List<String> CATEGORIES =
            List.of(
                    "nav.citizens",
                    "nav.procedures",
                    "nav.billing",
                    "nav.documents",
                    "nav.departments");

    private final VBox root;
    private final StatefulContent results;

    public GlobalSearchView() {
        results = new StatefulContent(new VBox());
        results.showEmpty("state.empty.title", "state.empty.detail");
        root = new VBox(18, Typography.display("shell.search.prompt"), results);
    }

    /**
     * Called by the shell when the operator submits a query. Phase 2 has no index yet, so every
     * query shows the empty state; Phase 5 wires the real {@code SearchIndexPort}.
     */
    public void query(String text) {
        results.showEmpty("state.empty.title", "state.empty.detail");
    }

    @Override
    public RouteKey route() {
        return RouteKey.SEARCH;
    }

    @Override
    public String titleKey() {
        return "shortcuts.search";
    }

    @Override
    public Parent node() {
        return root;
    }
}
