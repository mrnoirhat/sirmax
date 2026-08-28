// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The configurable intake form of a service version (master prompt §16). An ordered list of {@link
 * FormField}s with unique keys; an empty schema is valid (a service with no extra fields).
 */
public record FormSchema(List<FormField> fields) {

    public FormSchema {
        fields = fields == null ? List.of() : List.copyOf(fields);
        Set<String> keys = new LinkedHashSet<>();
        for (FormField f : fields) {
            if (!keys.add(f.key())) {
                throw new IllegalArgumentException("Duplicate form field key: " + f.key());
            }
        }
    }

    public static FormSchema empty() {
        return new FormSchema(List.of());
    }

    public boolean isEmpty() {
        return fields.isEmpty();
    }
}
