// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application;

import org.sirmax.shared.Result;

/**
 * A single operator-facing operation.
 *
 * <p>Implementations are small, transactional and side-effect-aware: they validate, authorize,
 * mutate through repositories, record an {@link org.sirmax.application.port.AuditSink audit event}
 * when required, and return a {@link Result}.
 *
 * @param <I> input command type
 * @param <O> success output type
 */
public interface UseCase<I, O> {

    Result<O> execute(I input);
}
