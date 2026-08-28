// SPDX-License-Identifier: AGPL-3.0-or-later

/**
 * RBAC domain: {@link org.sirmax.domain.security.AppUser accounts}, {@link
 * org.sirmax.domain.security.Role roles}, the {@link org.sirmax.domain.security.Permission}
 * catalog, and {@link org.sirmax.domain.security.AccessPolicy} for authorization decisions.
 *
 * <p>The domain never handles plaintext passwords — it holds a {@link
 * org.sirmax.domain.security.PasswordHash} and the application layer's hasher port does the rest.
 */
package org.sirmax.domain.security;
