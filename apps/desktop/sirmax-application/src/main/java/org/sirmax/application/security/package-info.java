// SPDX-License-Identifier: AGPL-3.0-or-later

/**
 * Cross-cutting security/audit helpers for the application layer: {@link
 * org.sirmax.application.security.Session} (a signed-in user + effective {@link
 * org.sirmax.domain.security.AccessPolicy}), {@link org.sirmax.application.security.AuditContext}
 * (who / where from), and {@link org.sirmax.application.security.Audit} (builds and writes audit
 * events).
 */
package org.sirmax.application.security;
