// SPDX-License-Identifier: AGPL-3.0-or-later

/**
 * The configurable service engine (master prompt §15–§22, §55; docs/adr/0006–0008).
 *
 * <p>{@link org.sirmax.domain.service.ServiceCategory} groups {@link
 * org.sirmax.domain.service.ServiceDefinition}s. Each definition has one or more {@link
 * org.sirmax.domain.service.ServiceDefinitionVersion}s: editable while {@link
 * org.sirmax.domain.service.ServiceStatus#DRAFT}, immutable once published, so procedures stay
 * interpretable against the exact version they were opened with. A version carries typed {@link
 * org.sirmax.domain.service.RequirementDef requirements}, {@link org.sirmax.domain.service.Sla} and
 * {@link org.sirmax.domain.service.Validity}, plus validated JSON for the parts that gain typed
 * models in later slices (form schema, workflow, fee rules, output documents, authorization).
 */
package org.sirmax.domain.service;
