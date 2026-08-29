// SPDX-License-Identifier: AGPL-3.0-or-later
/**
 * The municipal register, inspections and recorded decisions — master prompt §4, §28, §29.
 *
 * <p>{@link org.sirmax.domain.registry.RegisteredDocument} is the Conservaduría entry, deliberately
 * distinct from a file attached to a case: it has a book/folio identity, named parties and
 * append-only marginal annotations. {@link org.sirmax.domain.registry.Inspection} is the one site
 * visit every module reuses, and {@link org.sirmax.domain.registry.Decision} records each approval
 * act with its own author and reason.
 */
package org.sirmax.domain.registry;
