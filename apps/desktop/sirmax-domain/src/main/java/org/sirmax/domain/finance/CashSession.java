// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.finance;

import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.shared.Money;

/**
 * A cashier's drawer session (master prompt §20 — cash drawer, reconciliation).
 *
 * <p>Opens with a float, collects payments, and closes against a physical count. The difference
 * between what the system expected and what the cashier counted is <b>recorded, not corrected</b>:
 * a session that closes 200 pesos short is a fact the municipality needs to see, and silently
 * adjusting it would destroy the only signal that something went wrong.
 *
 * <p>Only cash moves the drawer. Transfers and cards are collected against invoices in the same
 * session but never counted at close (see {@link PaymentMethod#affectsCashDrawer()}).
 */
public final class CashSession {

    private final String id;
    private final String code;
    private final String cashierUserId;
    private final String departmentId; // nullable
    private Status status;
    private final Currency currency;
    private final Money openingFloat;
    private Money countedTotal; // nullable until closed
    private final Instant openedAt;
    private Instant closedAt; // nullable
    private String notes; // nullable

    public enum Status {
        OPEN,
        CLOSED
    }

    public CashSession(
            String id,
            String code,
            String cashierUserId,
            String departmentId,
            Status status,
            Currency currency,
            Money openingFloat,
            Money countedTotal,
            Instant openedAt,
            Instant closedAt,
            String notes) {
        this.id = requireText(id, "id");
        this.code = requireText(code, "code");
        this.cashierUserId = requireText(cashierUserId, "cashierUserId");
        this.departmentId = blankToNull(departmentId);
        this.status = Objects.requireNonNull(status, "status");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.openingFloat = Objects.requireNonNull(openingFloat, "openingFloat");
        this.countedTotal = countedTotal;
        this.openedAt = Objects.requireNonNull(openedAt, "openedAt");
        this.closedAt = closedAt;
        this.notes = blankToNull(notes);
        if (openingFloat.isNegative()) {
            throw new IllegalArgumentException("The opening float must not be negative");
        }
    }

    public static CashSession open(
            String id,
            String code,
            String cashierUserId,
            String departmentId,
            Money openingFloat,
            Instant now) {
        return new CashSession(
                id,
                code,
                cashierUserId,
                departmentId,
                Status.OPEN,
                openingFloat.currency(),
                openingFloat,
                null,
                now,
                null,
                null);
    }

    public String id() {
        return id;
    }

    public String code() {
        return code;
    }

    public String cashierUserId() {
        return cashierUserId;
    }

    public Optional<String> departmentId() {
        return Optional.ofNullable(departmentId);
    }

    public Status status() {
        return status;
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    public Currency currency() {
        return currency;
    }

    public Money openingFloat() {
        return openingFloat;
    }

    public Optional<Money> countedTotal() {
        return Optional.ofNullable(countedTotal);
    }

    public Instant openedAt() {
        return openedAt;
    }

    public Optional<Instant> closedAt() {
        return Optional.ofNullable(closedAt);
    }

    public Optional<String> notes() {
        return Optional.ofNullable(notes);
    }

    /**
     * What the drawer should hold: the opening float plus cash collected, minus cash refunded.
     * Non-cash collections are excluded — they never entered the drawer.
     */
    public Money expectedCash(Money cashCollected, Money cashRefunded) {
        return openingFloat.plus(cashCollected).minus(cashRefunded);
    }

    /**
     * Close against a physical count.
     *
     * @return the reconciliation difference — positive when the drawer holds more than expected,
     *     negative when it is short. Never zeroed out or hidden.
     */
    public Money close(Money counted, Money expected, String closingNotes, Instant now) {
        if (status == Status.CLOSED) {
            throw new IllegalStateException("Cash session " + code + " is already closed");
        }
        Objects.requireNonNull(counted, "counted");
        if (counted.isNegative()) {
            throw new IllegalArgumentException("The counted total must not be negative");
        }
        if (!counted.currency().equals(currency)) {
            throw new IllegalArgumentException(
                    "The count must be in " + currency.getCurrencyCode());
        }
        this.countedTotal = counted;
        this.status = Status.CLOSED;
        this.closedAt = now;
        this.notes = blankToNull(closingNotes);
        return counted.minus(expected);
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
        return o instanceof CashSession other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
