// SPDX-License-Identifier: AGPL-3.0-or-later

/**
 * Editable service templates (master prompt §54–§55).
 *
 * <p>A {@link org.sirmax.application.catalog.ServiceTemplate} is the starting point for a municipal
 * service: it carries the same typed configuration a {@code ServiceDefinitionVersion} holds
 * (requirements, workflow, fee rules, SLA, validity). {@link
 * org.sirmax.application.usecase.SeedServiceCatalog} materializes a {@link
 * org.sirmax.application.catalog.ServiceCatalogTemplates} bundle into real catalog rows whose v1 is
 * a {@code DRAFT} the municipality reviews, adjusts (amounts, requirements, flow) and publishes.
 *
 * <p>The bundle is produced by a {@link org.sirmax.application.port.ServiceCatalogTemplateSource}
 * port; the desktop build ships a Dominican Republic bundle as an infrastructure resource.
 */
package org.sirmax.application.catalog;
