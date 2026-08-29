// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.registry;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A site visit against a case (master prompt §29).
 *
 * <p>One reusable capability rather than one per module: construction, commercial permits,
 * environmental cases, public-space use and market compliance all schedule someone to go and look,
 * record what they found, and produce a pass/fail that the workflow reads.
 *
 * <p>The checklist is shaped by the service's configuration, so it is carried as typed answers here
 * and validated JSON at rest — the same contract the dynamic form uses (ADR 0006).
 */
public final class Inspection {

    private final String id;
    private final String code;
    private final String procedureId;
    private String assetId; // nullable
    private String inspectorUserId; // nullable until assigned

    private Status status;
    private Result result; // nullable until completed
    private LocalDate scheduledDate; // nullable
    private Instant performedAt; // nullable
    private String location; // nullable
    private String findings; // nullable
    private List<ChecklistAnswer> checklist;
    private LocalDate followUpDate; // nullable
    private final Instant createdAt;
    private Instant updatedAt;

    public enum Status {
        SCHEDULED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED;

        public boolean isFinished() {
            return this == COMPLETED || this == CANCELLED;
        }
    }

    /** The verdict the workflow reads (§29). */
    public enum Result {
        PASSED,
        FAILED,
        /** Approved provided the conditions in {@code findings} are met. */
        PASSED_WITH_CONDITIONS,
        NOT_APPLICABLE;

        /** {@code true} when the case may proceed on this result. */
        public boolean allowsProgress() {
            return this != FAILED;
        }
    }

    /**
     * One checked item.
     *
     * @param compliant {@code null} means "not assessed", which is different from "failed"
     */
    public record ChecklistAnswer(String key, String label, Boolean compliant, Optional<String> note) {
        public ChecklistAnswer {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(label, "label");
            note = note == null ? Optional.empty() : note;
        }

        public boolean isAssessed() {
            return compliant != null;
        }

        public boolean isBreach() {
            return Boolean.FALSE.equals(compliant);
        }
    }

    public Inspection(
            String id,
            String code,
            String procedureId,
            String assetId,
            String inspectorUserId,
            Status status,
            Result result,
            LocalDate scheduledDate,
            Instant performedAt,
            String location,
            String findings,
            List<ChecklistAnswer> checklist,
            LocalDate followUpDate,
            Instant createdAt,
            Instant updatedAt) {
        this.id = requireText(id, "id");
        this.code = requireText(code, "code");
        this.procedureId = requireText(procedureId, "procedureId");
        this.assetId = blankToNull(assetId);
        this.inspectorUserId = blankToNull(inspectorUserId);
        this.status = Objects.requireNonNull(status, "status");
        this.result = result;
        this.scheduledDate = scheduledDate;
        this.performedAt = performedAt;
        this.location = blankToNull(location);
        this.findings = blankToNull(findings);
        this.checklist = checklist == null ? List.of() : List.copyOf(checklist);
        this.followUpDate = followUpDate;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Inspection schedule(
            String id,
            String code,
            String procedureId,
            String inspectorUserId,
            LocalDate scheduledDate,
            Instant now) {
        return new Inspection(
                id,
                code,
                procedureId,
                null,
                inspectorUserId,
                Status.SCHEDULED,
                null,
                scheduledDate,
                null,
                null,
                null,
                List.of(),
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

    public String procedureId() {
        return procedureId;
    }

    public Optional<String> assetId() {
        return Optional.ofNullable(assetId);
    }

    public Optional<String> inspectorUserId() {
        return Optional.ofNullable(inspectorUserId);
    }

    public Status status() {
        return status;
    }

    public Optional<Result> result() {
        return Optional.ofNullable(result);
    }

    public Optional<LocalDate> scheduledDate() {
        return Optional.ofNullable(scheduledDate);
    }

    public Optional<Instant> performedAt() {
        return Optional.ofNullable(performedAt);
    }

    public Optional<String> location() {
        return Optional.ofNullable(location);
    }

    public Optional<String> findings() {
        return Optional.ofNullable(findings);
    }

    public List<ChecklistAnswer> checklist() {
        return checklist;
    }

    public Optional<LocalDate> followUpDate() {
        return Optional.ofNullable(followUpDate);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    /** The items the inspector marked non-compliant — what a failed report has to explain. */
    public List<ChecklistAnswer> breaches() {
        return checklist.stream().filter(ChecklistAnswer::isBreach).toList();
    }

    /** {@code true} when the visit is past its scheduled date and still has not happened. */
    public boolean isOverdue(LocalDate today) {
        return scheduledDate != null && !status.isFinished() && today.isAfter(scheduledDate);
    }

    public void assignTo(String userId, Instant now) {
        requireOpen();
        this.inspectorUserId = blankToNull(userId);
        touch(now);
    }

    public void reschedule(LocalDate date, Instant now) {
        requireOpen();
        this.scheduledDate = date;
        this.status = Status.SCHEDULED;
        touch(now);
    }

    public void relateToAsset(String newAssetId, Instant now) {
        requireOpen();
        this.assetId = blankToNull(newAssetId);
        touch(now);
    }

    public void begin(String whereItIs, Instant now) {
        requireOpen();
        this.status = Status.IN_PROGRESS;
        this.location = blankToNull(whereItIs);
        touch(now);
    }

    public void recordChecklist(List<ChecklistAnswer> answers, Instant now) {
        requireOpen();
        this.checklist = answers == null ? List.of() : List.copyOf(answers);
        touch(now);
    }

    /**
     * Close the visit with a verdict. A FAILED or conditional result must say why — the citizen is
     * entitled to know what was wrong, and the case's decision will quote it.
     */
    public void complete(Result verdict, String whatWasFound, LocalDate followUp, Instant now) {
        requireOpen();
        Objects.requireNonNull(verdict, "result");
        String found = blankToNull(whatWasFound);
        if (verdict != Result.PASSED && verdict != Result.NOT_APPLICABLE && found == null) {
            throw new IllegalArgumentException("A " + verdict + " inspection must record findings");
        }
        this.result = verdict;
        this.findings = found;
        this.followUpDate = followUp;
        this.status = Status.COMPLETED;
        this.performedAt = now;
        touch(now);
    }

    public void cancel(String reason, Instant now) {
        requireOpen();
        this.status = Status.CANCELLED;
        this.findings = blankToNull(reason);
        touch(now);
    }

    private void requireOpen() {
        if (status.isFinished()) {
            throw new IllegalStateException("Inspection " + code + " is " + status);
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

    @Override
    public boolean equals(Object o) {
        return o instanceof Inspection other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
