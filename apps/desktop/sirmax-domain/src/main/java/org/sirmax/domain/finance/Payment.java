// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.finance;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.shared.Money;

/**
 * Money received against an invoice (master prompt §59A.5).
 *
 * <p>Immutable. A payment is never edited — a mistake is corrected by a {@link Refund}, so the
 * drawer's history stays reconstructible from the rows alone.
 *
 * @param tendered what the payer physically handed over; only meaningful for cash
 * @param reference transfer/cheque/authorization number for non-cash methods
 */
public record Payment(
        String id,
        String code,
        String invoiceId,
        Optional<String> cashSessionId,
        PaymentMethod method,
        Money amount,
        Optional<Money> tendered,
        Optional<String> reference,
        Optional<String> payerName,
        Status status,
        Optional<String> receivedBy,
        Instant receivedAt,
        Optional<String> notes) {

    /** A payment either stands or has been reversed by a refund; it is never deleted. */
    public enum Status {
        SETTLED,
        REVERSED
    }

    public Payment {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(invoiceId, "invoiceId");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(receivedAt, "receivedAt");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("A payment must be positive");
        }
        cashSessionId = orEmpty(cashSessionId);
        tendered = orEmpty(tendered);
        reference = orEmpty(reference);
        payerName = orEmpty(payerName);
        receivedBy = orEmpty(receivedBy);
        notes = orEmpty(notes);
        if (tendered.isPresent() && tendered.get().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Tendered cannot be less than the amount paid");
        }
    }

    public static Payment of(
            String id,
            String code,
            String invoiceId,
            String cashSessionId,
            PaymentMethod method,
            Money amount,
            String receivedBy,
            Instant at) {
        return new Payment(
                id,
                code,
                invoiceId,
                Optional.ofNullable(cashSessionId),
                method,
                amount,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Status.SETTLED,
                Optional.ofNullable(receivedBy),
                at,
                Optional.empty());
    }

    /** Change owed back to the payer; zero unless cash was tendered above the amount due. */
    public Money change() {
        return tendered.map(t -> t.minus(amount)).orElse(Money.zero(amount.currency()));
    }

    public boolean isSettled() {
        return status == Status.SETTLED;
    }

    /** The same payment marked reversed; the original row is replaced, never removed. */
    public Payment reversed() {
        return new Payment(
                id,
                code,
                invoiceId,
                cashSessionId,
                method,
                amount,
                tendered,
                reference,
                payerName,
                Status.REVERSED,
                receivedBy,
                receivedAt,
                notes);
    }

    private static <T> Optional<T> orEmpty(Optional<T> v) {
        return v == null ? Optional.empty() : v;
    }
}
