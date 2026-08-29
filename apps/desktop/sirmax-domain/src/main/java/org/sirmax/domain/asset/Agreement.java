// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.asset;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.shared.Money;

/**
 * A contract between the municipality and a party over an asset (master prompt §26).
 *
 * <p>One model for every arrangement the spec lists — municipal property lease, market stall
 * assignment, cemetery concession, public-space permit — because they share the whole lifecycle:
 * they start, they may renew, they may be transferred to another holder, and they end. Only the
 * {@link AgreementKind} and the amount differ.
 *
 * <p>A <b>transfer</b> creates a new agreement pointing back at the old one rather than swapping the
 * holder in place. "Traspaso de contrato de arrendamiento" and "traspaso por herencia" are services
 * Santiago actually offers; each needs its own dated record, and a mutable holder column would erase
 * exactly the chain a later dispute turns on.
 */
public final class Agreement {

    private final String id;
    private final String code;
    private final AgreementKind kind;
    private final String assetId; // nullable — a permit need not name a registered asset
    private final String procedureId; // nullable — the case that produced it

    private PartyRef holder;
    private Status status;
    private LocalDate startDate;
    private LocalDate endDate; // nullable — indefinite (a perpetual concession)
    private boolean renewable;

    private Money amount;
    private BillingFrequency billingFrequency;

    private final String transferredFromId; // nullable
    private Instant terminatedAt; // nullable
    private String terminationReason; // nullable
    private String notes; // nullable
    private final Instant createdAt;
    private Instant updatedAt;

    /** Where the contract stands (§26 — status, transfer, termination). */
    public enum Status {
        DRAFT,
        ACTIVE,
        /** Temporarily not in force — unpaid, under dispute, or the asset is unusable. */
        SUSPENDED,
        /** Superseded by a transfer; the successor agreement points back at this one. */
        TRANSFERRED,
        EXPIRED,
        TERMINATED,
        CANCELLED;

        public boolean isFinished() {
            return this == TRANSFERRED
                    || this == EXPIRED
                    || this == TERMINATED
                    || this == CANCELLED;
        }
    }

    /** How often the recurring amount falls due (§26). */
    public enum BillingFrequency {
        ONCE,
        MONTHLY,
        QUARTERLY,
        ANNUAL,
        /** No recurring charge — a free permit, or one billed entirely through procedures. */
        NONE;

        /** How many times a year this frequency bills; {@code 0} for one-off and free. */
        public int perYear() {
            return switch (this) {
                case MONTHLY -> 12;
                case QUARTERLY -> 4;
                case ANNUAL -> 1;
                case ONCE, NONE -> 0;
            };
        }
    }

    public Agreement(
            String id,
            String code,
            AgreementKind kind,
            String assetId,
            String procedureId,
            PartyRef holder,
            Status status,
            LocalDate startDate,
            LocalDate endDate,
            boolean renewable,
            Money amount,
            BillingFrequency billingFrequency,
            String transferredFromId,
            Instant terminatedAt,
            String terminationReason,
            String notes,
            Instant createdAt,
            Instant updatedAt) {
        this.id = requireText(id, "id");
        this.code = requireText(code, "code");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.assetId = blankToNull(assetId);
        this.procedureId = blankToNull(procedureId);
        this.holder = Objects.requireNonNull(holder, "holder");
        this.status = Objects.requireNonNull(status, "status");
        this.startDate = Objects.requireNonNull(startDate, "startDate");
        this.endDate = endDate;
        this.renewable = renewable;
        this.amount = Objects.requireNonNull(amount, "amount");
        this.billingFrequency = Objects.requireNonNull(billingFrequency, "billingFrequency");
        this.transferredFromId = blankToNull(transferredFromId);
        this.terminatedAt = terminatedAt;
        this.terminationReason = blankToNull(terminationReason);
        this.notes = blankToNull(notes);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not precede startDate");
        }
        if (amount.isNegative()) {
            throw new IllegalArgumentException("An agreement amount must not be negative");
        }
    }

    public static Agreement draft(
            String id,
            String code,
            AgreementKind kind,
            String assetId,
            PartyRef holder,
            LocalDate startDate,
            Money amount,
            BillingFrequency frequency,
            Instant now) {
        return new Agreement(
                id,
                code,
                kind,
                assetId,
                null,
                holder,
                Status.DRAFT,
                startDate,
                null,
                true,
                amount,
                frequency,
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

    public AgreementKind kind() {
        return kind;
    }

    public Optional<String> assetId() {
        return Optional.ofNullable(assetId);
    }

    public Optional<String> procedureId() {
        return Optional.ofNullable(procedureId);
    }

    public PartyRef holder() {
        return holder;
    }

    public Status status() {
        return status;
    }

    public LocalDate startDate() {
        return startDate;
    }

    public Optional<LocalDate> endDate() {
        return Optional.ofNullable(endDate);
    }

    public boolean renewable() {
        return renewable;
    }

    public Money amount() {
        return amount;
    }

    public BillingFrequency billingFrequency() {
        return billingFrequency;
    }

    public Optional<String> transferredFromId() {
        return Optional.ofNullable(transferredFromId);
    }

    public Optional<Instant> terminatedAt() {
        return Optional.ofNullable(terminatedAt);
    }

    public Optional<String> terminationReason() {
        return Optional.ofNullable(terminationReason);
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

    /** {@code true} when the contract is in force on {@code date}. */
    public boolean isInForceOn(LocalDate date) {
        return status == Status.ACTIVE
                && !date.isBefore(startDate)
                && (endDate == null || !date.isAfter(endDate));
    }

    /** {@code true} once the end date has passed, whether or not anyone has marked it expired. */
    public boolean hasLapsedBy(LocalDate date) {
        return endDate != null && date.isAfter(endDate);
    }

    /** What one billing period costs; zero for one-off and free agreements. */
    public Money periodAmount() {
        return billingFrequency.perYear() == 0 ? Money.zero(amount.currency()) : amount;
    }

    /** What a full year of this agreement costs, for budgeting and arrears reports. */
    public Money annualAmount() {
        return amount.times(billingFrequency.perYear());
    }

    public void activate(Instant now) {
        if (status != Status.DRAFT && status != Status.SUSPENDED) {
            throw new IllegalStateException("Only a draft or suspended agreement can be activated");
        }
        this.status = Status.ACTIVE;
        touch(now);
    }

    public void suspend(String reason, Instant now) {
        requireInForce();
        this.status = Status.SUSPENDED;
        this.notes = blankToNull(reason);
        touch(now);
    }

    public void setTerm(LocalDate newStart, LocalDate newEnd, boolean isRenewable, Instant now) {
        if (status.isFinished()) {
            throw new IllegalStateException("A finished agreement's term cannot be changed");
        }
        Objects.requireNonNull(newStart, "startDate");
        if (newEnd != null && newEnd.isBefore(newStart)) {
            throw new IllegalArgumentException("endDate must not precede startDate");
        }
        this.startDate = newStart;
        this.endDate = newEnd;
        this.renewable = isRenewable;
        touch(now);
    }

    public void setAmount(Money newAmount, BillingFrequency frequency, Instant now) {
        if (status.isFinished()) {
            throw new IllegalStateException("A finished agreement's amount cannot be changed");
        }
        Objects.requireNonNull(newAmount, "amount");
        if (newAmount.isNegative()) {
            throw new IllegalArgumentException("An agreement amount must not be negative");
        }
        this.amount = newAmount;
        this.billingFrequency = Objects.requireNonNull(frequency, "billingFrequency");
        touch(now);
    }

    /** Extend a renewable agreement to a new end date. */
    public void renew(LocalDate newEnd, Instant now) {
        requireInForce();
        if (!renewable) {
            throw new IllegalStateException("Agreement " + code + " is not renewable");
        }
        Objects.requireNonNull(newEnd, "newEnd");
        if (endDate != null && !newEnd.isAfter(endDate)) {
            throw new IllegalArgumentException("A renewal must extend the term");
        }
        this.endDate = newEnd;
        this.status = Status.ACTIVE;
        touch(now);
    }

    /**
     * Build the successor of a transfer. This agreement is closed as TRANSFERRED and the returned
     * one carries the new holder, so the chain stays walkable in both directions.
     */
    public Agreement transferTo(
            String newId, String newCode, PartyRef newHolder, LocalDate effective, Instant now) {
        requireInForce();
        Objects.requireNonNull(newHolder, "newHolder");
        this.status = Status.TRANSFERRED;
        this.terminatedAt = now;
        this.terminationReason = "Traspaso";
        touch(now);

        return new Agreement(
                newId,
                newCode,
                kind,
                assetId,
                procedureId,
                newHolder,
                Status.ACTIVE,
                effective,
                endDate,
                renewable,
                amount,
                billingFrequency,
                id,
                null,
                null,
                notes,
                now,
                now);
    }

    public void expire(Instant now) {
        if (status.isFinished()) {
            throw new IllegalStateException("Agreement " + code + " is already " + status);
        }
        this.status = Status.EXPIRED;
        this.terminatedAt = now;
        touch(now);
    }

    public void terminate(String reason, Instant now) {
        if (status.isFinished()) {
            throw new IllegalStateException("Agreement " + code + " is already " + status);
        }
        String why = blankToNull(reason);
        if (why == null) {
            throw new IllegalArgumentException("A termination must carry a reason");
        }
        this.status = Status.TERMINATED;
        this.terminationReason = why;
        this.terminatedAt = now;
        touch(now);
    }

    public void setNotes(String value, Instant now) {
        this.notes = blankToNull(value);
        touch(now);
    }

    private void requireInForce() {
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("Agreement " + code + " is " + status + ", not active");
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
        return o instanceof Agreement other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
