// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.finance;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.shared.Money;

/**
 * Money returned to a payer (master prompt §59A.6).
 *
 * <p>A refund is its own row rather than an edit of the {@link Payment}: after the fact, the drawer
 * must show both that the money came in and that it went back out. A partial refund is allowed; the
 * sum of refunds against one payment can never exceed it, which the use case enforces.
 */
public record Refund(
        String id,
        String code,
        String paymentId,
        String invoiceId,
        Optional<String> cashSessionId,
        Money amount,
        String reason,
        Optional<String> authorizedBy,
        Instant refundedAt) {

    public Refund {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(paymentId, "paymentId");
        Objects.requireNonNull(invoiceId, "invoiceId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(refundedAt, "refundedAt");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("A refund must be positive");
        }
        Objects.requireNonNull(reason, "reason");
        if (reason.isBlank()) {
            throw new IllegalArgumentException("A refund must carry a reason");
        }
        reason = reason.strip();
        cashSessionId = cashSessionId == null ? Optional.empty() : cashSessionId;
        authorizedBy = authorizedBy == null ? Optional.empty() : authorizedBy;
    }
}
