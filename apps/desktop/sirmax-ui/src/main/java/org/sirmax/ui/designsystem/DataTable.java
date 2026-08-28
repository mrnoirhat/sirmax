// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.designsystem;

import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import org.sirmax.ui.i18n.Messages;

/**
 * Applies the SIRMAX table styling and a friendly empty placeholder to a {@link TableView}.
 *
 * <p>Phase 2 provides styling + empty state only. Server-side pagination and column configuration
 * arrive with the first data-backed list (Phase 5).
 */
public final class DataTable {

    private DataTable() {}

    public static <T> TableView<T> styled(TableView<T> table) {
        table.getStyleClass().add(Styles.TABLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        Label placeholder = new Label(Messages.get("state.empty.title"));
        placeholder.getStyleClass().add(Styles.STATE_DETAIL);
        table.setPlaceholder(placeholder);
        return table;
    }
}
