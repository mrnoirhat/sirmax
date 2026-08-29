// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.registry;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A recorded act of approval on a case (master prompt §28).
 *
 * <p>Kept separate from {@code Procedure.outcome} because a real case collects <em>several</em>
 * decisions from several roles — a technical review, then legal, then the director — and each one
 * needs its own author, date and reason. The procedure's outcome is the last word; this is the
 * record of how it was reached.
 *
 * <p>Immutable: a decision is not revised, it is superseded by another one.
 *
 * @param decidedByRole the role the person acted in, snapshotted — role assignments change
 * @param conditions what a {@link Outcome#CONDITIONALLY_APPROVED} case must still satisfy
 */
public record Decision(
        String id,
        String procedureId,
        Optional<String> stepKey,
        Outcome outcome,
        Optional<String> decidedBy,
        Optional<String> decidedByRole,
        Instant decidedAt,
        Optional<String> reason,
        Optional<String> comments,
        Optional<String> conditions,
        Optional<String> documentId) {

    /** The decision vocabulary from §28; closed, because the workflow branches on it. */
    public enum Outcome {
        APPROVED,
        REJECTED,
        RETURNED_FOR_CORRECTION,
        CONDITIONALLY_APPROVED,
        EXPIRED,
        CANCELLED;

        /** {@code true} when the case may move forward on this decision. */
        public boolean allowsProgress() {
            return this == APPROVED || this == CONDITIONALLY_APPROVED;
        }

        /** {@code true} when the citizen has to do something before the case moves again. */
        public boolean returnsToApplicant() {
            return this == RETURNED_FOR_CORRECTION;
        }
    }

    public Decision {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(procedureId, "procedureId");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(decidedAt, "decidedAt");
        stepKey = orEmpty(stepKey);
        decidedBy = orEmpty(decidedBy);
        decidedByRole = orEmpty(decidedByRole);
        reason = orEmpty(reason);
        comments = orEmpty(comments);
        conditions = orEmpty(conditions);
        documentId = orEmpty(documentId);

        // A refusal the citizen cannot act on is not a decision, it is a dead end.
        if ((outcome == Outcome.REJECTED || outcome == Outcome.RETURNED_FOR_CORRECTION)
                && reason.isEmpty()) {
            throw new IllegalArgumentException(outcome + " must carry a reason");
        }
        if (outcome == Outcome.CONDITIONALLY_APPROVED && conditions.isEmpty()) {
            throw new IllegalArgumentException(
                    "A conditional approval must state its conditions");
        }
    }

    public static Decision approved(
            String id, String procedureId, String stepKey, String userId, String role, Instant at) {
        return new Decision(
                id,
                procedureId,
                Optional.ofNullable(stepKey),
                Outcome.APPROVED,
                Optional.ofNullable(userId),
                Optional.ofNullable(role),
                at,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    public static Decision rejected(
            String id,
            String procedureId,
            String stepKey,
            String userId,
            String role,
            String reason,
            Instant at) {
        return new Decision(
                id,
                procedureId,
                Optional.ofNullable(stepKey),
                Outcome.REJECTED,
                Optional.ofNullable(userId),
                Optional.ofNullable(role),
                at,
                Optional.ofNullable(reason),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static Optional<String> orEmpty(Optional<String> v) {
        if (v == null || v.isEmpty()) {
            return Optional.empty();
        }
        String s = v.get().strip();
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }
}
