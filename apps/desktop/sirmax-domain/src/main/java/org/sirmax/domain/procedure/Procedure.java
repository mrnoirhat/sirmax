// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.procedure;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.domain.common.PartyRef;

/**
 * A municipal case: one citizen request against one published service version (master prompt §16).
 *
 * <p>The same aggregate carries certificates, permits, registrations, complaints and operational
 * cases — the difference is entirely in the service version it was opened with, never in a parallel
 * class hierarchy (§15).
 *
 * <p>A procedure pins {@code serviceVersionId} at open time and never re-reads the service's current
 * version: rules are versioned (§39), so a case decided under last year's fee table stays
 * interpretable.
 *
 * <p>Transitions are guarded here rather than in the use case so the invariants hold for every
 * caller: a terminal case does not move, an outcome is recorded exactly once, and the closing
 * timestamp is set with the outcome.
 */
public final class Procedure {

    private final String id;
    private final String code;
    private final String serviceDefinitionId;
    private final String serviceVersionId;
    private final PartyRef applicant;

    private String subjectType; // nullable — property, plot, registered document…
    private String subjectId; // nullable
    private ProcedureStatus status;
    private Priority priority;
    private String currentStepKey; // nullable once terminal
    private String departmentId; // nullable
    private String assignedUserId; // nullable
    private final Instant openedAt;
    private LocalDate dueDate; // nullable — the service version may declare no SLA
    private Instant closedAt; // nullable
    private ProcedureOutcome outcome; // nullable until decided
    private String outcomeReason; // nullable
    private String notes; // nullable
    private final Instant createdAt;
    private Instant updatedAt;

    public Procedure(
            String id,
            String code,
            String serviceDefinitionId,
            String serviceVersionId,
            PartyRef applicant,
            String subjectType,
            String subjectId,
            ProcedureStatus status,
            Priority priority,
            String currentStepKey,
            String departmentId,
            String assignedUserId,
            Instant openedAt,
            LocalDate dueDate,
            Instant closedAt,
            ProcedureOutcome outcome,
            String outcomeReason,
            String notes,
            Instant createdAt,
            Instant updatedAt) {
        this.id = requireText(id, "id");
        this.code = requireText(code, "code");
        this.serviceDefinitionId = requireText(serviceDefinitionId, "serviceDefinitionId");
        this.serviceVersionId = requireText(serviceVersionId, "serviceVersionId");
        this.applicant = Objects.requireNonNull(applicant, "applicant");
        this.subjectType = blankToNull(subjectType);
        this.subjectId = blankToNull(subjectId);
        this.status = Objects.requireNonNull(status, "status");
        this.priority = Objects.requireNonNull(priority, "priority");
        this.currentStepKey = blankToNull(currentStepKey);
        this.departmentId = blankToNull(departmentId);
        this.assignedUserId = blankToNull(assignedUserId);
        this.openedAt = Objects.requireNonNull(openedAt, "openedAt");
        this.dueDate = dueDate;
        this.closedAt = closedAt;
        this.outcome = outcome;
        this.outcomeReason = blankToNull(outcomeReason);
        this.notes = blankToNull(notes);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** Open a new case at the workflow's first step. */
    public static Procedure open(
            String id,
            String code,
            String serviceDefinitionId,
            String serviceVersionId,
            PartyRef applicant,
            String firstStepKey,
            LocalDate dueDate,
            Instant now) {
        return new Procedure(
                id,
                code,
                serviceDefinitionId,
                serviceVersionId,
                applicant,
                null,
                null,
                ProcedureStatus.OPEN,
                Priority.NORMAL,
                firstStepKey,
                null,
                null,
                now,
                dueDate,
                null,
                null,
                null,
                null,
                now,
                now);
    }

    public String id() {
        return id;
    }

    public String code() {
        return code;
    }

    public String serviceDefinitionId() {
        return serviceDefinitionId;
    }

    public String serviceVersionId() {
        return serviceVersionId;
    }

    public PartyRef applicant() {
        return applicant;
    }

    public Optional<String> subjectType() {
        return Optional.ofNullable(subjectType);
    }

    public Optional<String> subjectId() {
        return Optional.ofNullable(subjectId);
    }

    public ProcedureStatus status() {
        return status;
    }

    public Priority priority() {
        return priority;
    }

    public Optional<String> currentStepKey() {
        return Optional.ofNullable(currentStepKey);
    }

    public Optional<String> departmentId() {
        return Optional.ofNullable(departmentId);
    }

    public Optional<String> assignedUserId() {
        return Optional.ofNullable(assignedUserId);
    }

    public Instant openedAt() {
        return openedAt;
    }

    public Optional<LocalDate> dueDate() {
        return Optional.ofNullable(dueDate);
    }

    public Optional<Instant> closedAt() {
        return Optional.ofNullable(closedAt);
    }

    public Optional<ProcedureOutcome> outcome() {
        return Optional.ofNullable(outcome);
    }

    public Optional<String> outcomeReason() {
        return Optional.ofNullable(outcomeReason);
    }

    public Optional<String> notes() {
        return Optional.ofNullable(notes);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    /** {@code true} when {@code today} is past the SLA due date and the case is still open. */
    public boolean isOverdue(LocalDate today) {
        return dueDate != null && status.isOpenWork() && today.isAfter(dueDate);
    }

    /** What the case is about, when it is not just about the applicant (a property, a plot…). */
    public void setSubject(String type, String subjectIdentifier, Instant now) {
        this.subjectType = blankToNull(type);
        this.subjectId = blankToNull(subjectIdentifier);
        touch(now);
    }

    public void setPriority(Priority newPriority, Instant now) {
        this.priority = Objects.requireNonNull(newPriority, "priority");
        touch(now);
    }

    public void setNotes(String newNotes, Instant now) {
        this.notes = blankToNull(newNotes);
        touch(now);
    }

    public void setDueDate(LocalDate newDueDate, Instant now) {
        this.dueDate = newDueDate;
        touch(now);
    }

    /** Route the case to a department and/or a user (§57 — queues are per department and per user). */
    public void assign(String newDepartmentId, String newAssignedUserId, Instant now) {
        requireNotTerminal();
        this.departmentId = blankToNull(newDepartmentId);
        this.assignedUserId = blankToNull(newAssignedUserId);
        if (status == ProcedureStatus.OPEN && this.assignedUserId != null) {
            status = ProcedureStatus.IN_PROGRESS;
        }
        touch(now);
    }

    /** Move to the next workflow step. The caller resolved the step through the workflow engine. */
    public void moveToStep(String nextStepKey, Instant now) {
        requireNotTerminal();
        this.currentStepKey = blankToNull(nextStepKey);
        if (status == ProcedureStatus.OPEN
                || status == ProcedureStatus.WAITING_REQUIREMENTS
                || status == ProcedureStatus.WAITING_PAYMENT) {
            status = ProcedureStatus.IN_PROGRESS;
        }
        touch(now);
    }

    /** Park the case because a mandatory requirement is missing (§56). */
    public void blockOnRequirements(Instant now) {
        requireNotTerminal();
        status = ProcedureStatus.WAITING_REQUIREMENTS;
        touch(now);
    }

    /** Park the case at a payment checkpoint until the invoice is settled. */
    public void blockOnPayment(Instant now) {
        requireNotTerminal();
        status = ProcedureStatus.WAITING_PAYMENT;
        touch(now);
    }

    /** Unpark a blocked case once the blocker cleared. */
    public void resume(Instant now) {
        requireNotTerminal();
        if (status.isBlocked()) {
            status = ProcedureStatus.IN_PROGRESS;
            touch(now);
        }
    }

    /**
     * Record the decision. Approval leaves the case open for delivery; rejection and cancellation
     * are terminal (§28).
     */
    public void decide(ProcedureOutcome decision, String reason, Instant now) {
        requireNotTerminal();
        Objects.requireNonNull(decision, "decision");
        if (decision == ProcedureOutcome.REJECTED && blankToNull(reason) == null) {
            throw new IllegalArgumentException("A rejection must carry a reason");
        }
        this.outcome = decision;
        this.outcomeReason = blankToNull(reason);
        switch (decision) {
            case APPROVED -> status = ProcedureStatus.APPROVED;
            case REJECTED -> {
                status = ProcedureStatus.REJECTED;
                closedAt = now;
                currentStepKey = null;
            }
            case CANCELLED -> {
                status = ProcedureStatus.CANCELLED;
                closedAt = now;
                currentStepKey = null;
            }
            case DELIVERED -> {
                status = ProcedureStatus.DELIVERED;
                closedAt = now;
                currentStepKey = null;
            }
        }
        touch(now);
    }

    /** Close a finished case without a decision of its own (e.g. a resolved complaint). */
    public void close(Instant now) {
        requireNotTerminal();
        status = ProcedureStatus.CLOSED;
        closedAt = now;
        currentStepKey = null;
        touch(now);
    }

    /** Cancel the case; always allowed while it is not already terminal. */
    public void cancel(String reason, Instant now) {
        decide(ProcedureOutcome.CANCELLED, reason, now);
    }

    /** Reopen a terminal case at the given step; the previous outcome is cleared. */
    public void reopen(String stepKey, Instant now) {
        if (!status.isTerminal()) {
            throw new IllegalStateException("Only a terminal procedure can be reopened");
        }
        status = ProcedureStatus.IN_PROGRESS;
        currentStepKey = blankToNull(stepKey);
        closedAt = null;
        outcome = null;
        outcomeReason = null;
        touch(now);
    }

    private void requireNotTerminal() {
        if (status.isTerminal()) {
            throw new IllegalStateException(
                    "Procedure " + code + " is " + status + " and cannot be modified");
        }
    }

    private void touch(Instant now) {
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String v = value.strip();
        if (v.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return v;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String v = value.strip();
        return v.isEmpty() ? null : v;
    }
}
