// SPDX-License-Identifier: AGPL-3.0-or-later

/**
 * Use cases: one class per operator intent. Each validates, authorizes, mutates through repository
 * ports inside a {@link org.sirmax.application.port.UnitOfWork}, records an audit event when
 * required, and returns a {@link org.sirmax.shared.Result} for expected outcomes (throwing only for
 * unexpected faults).
 */
package org.sirmax.application.usecase;
