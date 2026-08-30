// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.designsystem;

import java.util.Locale;
import javafx.scene.control.ListCell;
import org.sirmax.ui.i18n.Messages;

/**
 * Enum constants shown to an operator.
 *
 * <p>Every screen that puts an enum in a table or a combo box was doing the same
 * {@code prefix + "." + value.name().toLowerCase(ROOT)} dance, and each copy was one more place a
 * new constant could quietly render as a raw Java identifier. One helper means one convention:
 * {@code invoice.status.paid}, {@code payment.method.cash}, {@code service.type.con_tasa}.
 *
 * <p>{@link Locale#ROOT} on the lowercasing is deliberate rather than incidental. A Turkish default
 * locale maps {@code I} to a dotless {@code ı}, which would turn {@code INVOICE} into a key no
 * properties file has — a bug that only appears on the machines least likely to be testing it.
 */
public final class Enums {

    private Enums() {}

    /** The translated label for {@code value} under {@code prefix}. */
    public static String label(String prefix, Enum<?> value) {
        if (value == null) {
            return "—";
        }
        return Messages.get(key(prefix, value));
    }

    /**
     * The message key for {@code value}, for the APIs that take a key rather than a label —
     * {@link Buttons}, for instance, translates its own argument.
     */
    public static String key(String prefix, Enum<?> value) {
        return prefix + "." + value.name().toLowerCase(Locale.ROOT);
    }

    /** A cell that renders enum items with {@link #label}; use for both list and button cells. */
    public static <E extends Enum<E>> ListCell<E> cell(String prefix) {
        return new ListCell<>() {
            @Override
            protected void updateItem(E item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : label(prefix, item));
            }
        };
    }
}
