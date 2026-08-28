// SPDX-License-Identifier: AGPL-3.0-or-later

/**
 * Cross-cutting primitives shared by every SIRMAX layer.
 *
 * <p>This package has <strong>no dependencies</strong> on other SIRMAX modules and no I/O. It holds
 * value types that the domain, application, infrastructure and UI layers all speak: {@link
 * org.sirmax.shared.Money}, {@link org.sirmax.shared.Result}, identifier helpers and base error
 * types.
 *
 * <p>Rule: never represent money with {@code double}/{@code float}. Use {@link
 * org.sirmax.shared.Money}.
 */
package org.sirmax.shared;
