// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.catalog;

import java.util.Objects;

/**
 * A catalog grouping in a seed bundle (master prompt §54).
 *
 * @param code stable code, matched case-insensitively against existing categories
 * @param name operator-facing name (seed data, editable afterwards)
 * @param sortOrder display order in the catalog
 */
public record ServiceCategoryTemplate(String code, String name, int sortOrder) {

    public ServiceCategoryTemplate {
        code = requireText(code, "code").toUpperCase(java.util.Locale.ROOT);
        name = requireText(name, "name");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String v = value.strip();
        if (v.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return v;
    }
}
