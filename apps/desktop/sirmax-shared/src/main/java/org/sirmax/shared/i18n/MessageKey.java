// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.shared.i18n;

import java.util.Objects;

/**
 * A stable i18n message key (e.g. {@code "procedure.requirements.incomplete"}).
 *
 * <p>Domain and application code carry {@code MessageKey}s, never literal user-facing strings
 * (master prompt §36). The UI resolves them against a locale bundle (Spanish first).
 *
 * @param value dotted lowercase key
 */
public record MessageKey(String value) {

    public MessageKey {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("MessageKey must not be blank");
        }
    }

    public static MessageKey of(String value) {
        return new MessageKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
