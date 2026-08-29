// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.catalog;

import java.util.List;
import java.util.Objects;

/**
 * A bundle of editable service templates for one country (master prompt §54).
 *
 * @param country ISO 3166-1 alpha-2 code the bundle targets, e.g. {@code "DO"}
 * @param version bundle revision, for traceability in seed notes / audit
 * @param categories catalog groupings
 * @param services the service templates
 */
public record ServiceCatalogTemplates(
        String country,
        int version,
        List<ServiceCategoryTemplate> categories,
        List<ServiceTemplate> services) {

    public ServiceCatalogTemplates {
        Objects.requireNonNull(country, "country");
        country = country.strip().toUpperCase(java.util.Locale.ROOT);
        if (country.length() != 2) {
            throw new IllegalArgumentException("country must be an ISO 3166-1 alpha-2 code");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1");
        }
        categories = categories == null ? List.of() : List.copyOf(categories);
        services = services == null ? List.of() : List.copyOf(services);
    }

    public boolean isEmpty() {
        return services.isEmpty();
    }
}
