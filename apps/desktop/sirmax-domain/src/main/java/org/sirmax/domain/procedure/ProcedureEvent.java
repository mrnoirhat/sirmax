// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.procedure;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * One append-only entry of a case timeline (master prompt §16 "History").
 *
 * @param actorUserId the operator who caused it; empty for system-generated entries
 * @param detail short human-readable text (a note body, a decision reason, an invoice number)
 */
public record ProcedureEvent(
        String id,
        String procedureId,
        Instant occurredAt,
        Optional<String> actorUserId,
        ProcedureEventKind kind,
        Optional<String> fromStepKey,
        Optional<String> toStepKey,
        Optional<String> detail) {

    public ProcedureEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(procedureId, "procedureId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(kind, "kind");
        actorUserId = normalize(actorUserId);
        fromStepKey = normalize(fromStepKey);
        toStepKey = normalize(toStepKey);
        detail = normalize(detail);
    }

    public static ProcedureEvent of(
            String id, String procedureId, ProcedureEventKind kind, String actorUserId, String detail, Instant at) {
        return new ProcedureEvent(
                id,
                procedureId,
                at,
                Optional.ofNullable(actorUserId),
                kind,
                Optional.empty(),
                Optional.empty(),
                Optional.ofNullable(detail));
    }

    public static ProcedureEvent stepChange(
            String id,
            String procedureId,
            String actorUserId,
            String fromStep,
            String toStep,
            Instant at) {
        return new ProcedureEvent(
                id,
                procedureId,
                at,
                Optional.ofNullable(actorUserId),
                ProcedureEventKind.STEP_ADVANCED,
                Optional.ofNullable(fromStep),
                Optional.ofNullable(toStep),
                Optional.empty());
    }

    private static Optional<String> normalize(Optional<String> v) {
        if (v == null || v.isEmpty()) {
            return Optional.empty();
        }
        String s = v.get().strip();
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }
}
