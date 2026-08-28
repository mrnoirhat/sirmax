// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.view;

import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.sirmax.ui.designsystem.Cards;
import org.sirmax.ui.designsystem.Styles;
import org.sirmax.ui.designsystem.Typography;
import org.sirmax.ui.i18n.Messages;
import org.sirmax.ui.nav.RouteKey;

/**
 * The global-search results screen (Ctrl+K). Phase 2 shows the <em>categorized</em> results
 * structure — one section per category — each with an empty state. The index and live results
 * arrive in Phase 5 (master prompt §32).
 */
public final class GlobalSearchView implements SirmaxView {

    /** The result categories the search groups by (i18n keys). */
    public static final List<String> CATEGORIES =
            List.of(
                    "nav.citizens",
                    "nav.procedures",
                    "nav.billing",
                    "nav.documents",
                    "nav.departments");

    private final VBox root = new VBox(18);
    private final Label queryEcho = new Label();

    public GlobalSearchView() {
        queryEcho.getStyleClass().add(Styles.MUTED);
        rebuild("");
    }

    /**
     * Called by the shell when the operator submits a query. Phase 2 has no index, so every category
     * shows its empty state; Phase 5 wires the real {@code SearchIndexPort}.
     */
    public void query(String text) {
        rebuild(text == null ? "" : text.strip());
    }

    private void rebuild(String text) {
        root.getChildren().clear();
        root.getChildren().add(Typography.display("shortcuts.search"));

        queryEcho.setText(text.isBlank() ? "" : "«" + text + "»");
        if (!text.isBlank()) {
            root.getChildren().add(queryEcho);
        }

        for (String categoryKey : CATEGORIES) {
            Label header = new Label(Messages.get(categoryKey));
            header.getStyleClass().add(Styles.SUBTITLE);

            Label emptyLine = new Label(Messages.get("state.empty.title"));
            emptyLine.getStyleClass().add(Styles.STATE_DETAIL);

            VBox section = new VBox(8, header, Cards.card(emptyLine));
            section.setPadding(new Insets(0, 0, 4, 0));
            root.getChildren().add(section);
        }
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
