// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.util.List;
import java.util.Optional;
import org.sirmax.domain.service.ServiceCategory;
import org.sirmax.domain.service.ServiceDefinition;
import org.sirmax.domain.service.ServiceDefinitionVersion;

/** Persistence for the service catalog: categories, definitions and their versions. */
public interface ServiceCatalogRepository {

    // ── categories ──
    void saveCategory(ServiceCategory category);

    Optional<ServiceCategory> findCategoryById(String id);

    Optional<ServiceCategory> findCategoryByCode(String code);

    List<ServiceCategory> listActiveCategories();

    // ── definitions ──
    void saveDefinition(ServiceDefinition definition);

    Optional<ServiceDefinition> findDefinitionById(String id);

    Optional<ServiceDefinition> findDefinitionByCode(String code);

    List<ServiceDefinition> listDefinitions(boolean includeArchived);

    // ── versions ──
    void saveVersion(ServiceDefinitionVersion version);

    Optional<ServiceDefinitionVersion> findVersionById(String id);

    List<ServiceDefinitionVersion> listVersions(String serviceDefinitionId);

    /** The single ACTIVE version of a definition, if any. */
    Optional<ServiceDefinitionVersion> findActiveVersion(String serviceDefinitionId);

    /** {@code max(version_number) + 1} for the definition, or 1 if it has none. */
    int nextVersionNumber(String serviceDefinitionId);
}
