// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.security;

import java.util.Optional;
import org.sirmax.application.port.AuditSink;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.domain.audit.AuditEvent;

/**
 * Builds and writes {@link AuditEvent}s from an {@link AuditContext} so use cases do not repeat the
 * who/when/id plumbing. Audit is append-only (master prompt §40).
 */
public final class Audit {

    private final AuditSink sink;
    private final Clock clock;
    private final IdGenerator ids;

    public Audit(AuditSink sink, Clock clock, IdGenerator ids) {
        this.sink = sink;
        this.clock = clock;
        this.ids = ids;
    }

    public void record(AuditContext ctx, String action, String entityType, String entityId) {
        record(ctx, action, entityType, entityId, null, null, null);
    }

    public void record(
            AuditContext ctx,
            String action,
            String entityType,
            String entityId,
            String beforeJson,
            String afterJson,
            String reason) {
        sink.record(
                new AuditEvent(
                        ids.newId(),
                        clock.now(),
                        ctx.actorUserId(),
                        action,
                        entityType,
                        entityId,
                        Optional.ofNullable(beforeJson),
                        Optional.ofNullable(afterJson),
                        Optional.ofNullable(reason),
                        ctx.sessionId(),
                        ctx.source()));
    }
}
