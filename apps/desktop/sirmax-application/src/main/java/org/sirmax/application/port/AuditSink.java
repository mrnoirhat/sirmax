// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import org.sirmax.domain.audit.AuditEvent;

/**
 * Append-only destination for {@link AuditEvent}s.
 *
 * <p>The infrastructure adapter writes to an immutable table; there is deliberately no update or
 * delete operation (master prompt §40).
 */
public interface AuditSink {

    void record(AuditEvent event);
}
