// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.shared;

import java.util.Objects;

/**
 * An opaque, syntactically-sane JSON string.
 *
 * <p>Used by the domain for the still-loosely-typed, configurable parts of a service definition
 * (form schema, output documents, authorization rules) so the domain stays framework-free — full
 * parsing/validation happens in the infrastructure layer, which owns the JSON library. As those
 * parts gain typed domain models they stop using {@code JsonDoc}.
 *
 * @param value a non-blank string that looks like a JSON object or array
 */
public record JsonDoc(String value) {

    public static final JsonDoc EMPTY_OBJECT = new JsonDoc("{}");
    public static final JsonDoc EMPTY_ARRAY = new JsonDoc("[]");

    public JsonDoc {
        Objects.requireNonNull(value, "value");
        String v = value.strip();
        if (v.isEmpty()) {
            throw new IllegalArgumentException("JsonDoc must not be blank");
        }
        char first = v.charAt(0);
        char last = v.charAt(v.length() - 1);
        boolean object = first == '{' && last == '}';
        boolean array = first == '[' && last == ']';
        if (!object && !array) {
            throw new IllegalArgumentException("JsonDoc must be a JSON object or array");
        }
        value = v;
    }

    public static JsonDoc of(String value) {
        return new JsonDoc(value);
    }

    public boolean isArray() {
        return value.charAt(0) == '[';
    }

    public boolean isObject() {
        return value.charAt(0) == '{';
    }

    @Override
    public String toString() {
        return value;
    }
}
