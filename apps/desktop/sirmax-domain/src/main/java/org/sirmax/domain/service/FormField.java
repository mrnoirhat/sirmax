// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One configurable field of a service's intake form (master prompt §16).
 *
 * @param key stable key within the form
 * @param label operator-facing label (administrator-authored data)
 * @param type input kind
 * @param required whether it must be filled to submit the request
 * @param helpText optional hint shown under the field
 * @param options choices for {@link FieldType#SELECT} (empty otherwise)
 */
public record FormField(
        String key,
        String label,
        FieldType type,
        boolean required,
        Optional<String> helpText,
        List<Option> options) {

    /** A choice for a {@link FieldType#SELECT} field. */
    public record Option(String value, String label) {
        public Option {
            value = requireText(value, "value");
            label = requireText(label, "label");
        }
    }

    public FormField {
        key = requireKey(key);
        label = requireText(label, "label");
        Objects.requireNonNull(type, "type");
        helpText = normalize(helpText);
        options = options == null ? List.of() : List.copyOf(options);
        if (type == FieldType.SELECT && options.isEmpty()) {
            throw new IllegalArgumentException("SELECT field '" + key + "' needs options");
        }
    }

    public static FormField text(String key, String label, boolean required) {
        return new FormField(key, label, FieldType.TEXT, required, Optional.empty(), List.of());
    }

    private static String requireKey(String key) {
        String k = requireText(key, "key").toLowerCase(java.util.Locale.ROOT);
        if (!k.matches("[a-z0-9_]{1,40}")) {
            throw new IllegalArgumentException("key must be 1–40 chars of a–z, 0–9 or '_'");
        }
        return k;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String v = value.strip();
        if (v.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return v;
    }

    private static Optional<String> normalize(Optional<String> v) {
        if (v == null || v.isEmpty()) {
            return Optional.empty();
        }
        String s = v.get().strip();
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }
}
