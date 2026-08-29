// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.finance;

import java.util.Objects;
import java.util.Optional;
import org.sirmax.shared.Money;

/**
 * One billed line of an {@link Invoice} (master prompt §59A.4).
 *
 * <p>Immutable, and frozen with the invoice: {@code lineTotal} is stored rather than recomputed on
 * read, so a reprint years later shows exactly what the citizen paid even if the fee rules, the
 * rounding policy or the service itself have since changed (§59F).
 *
 * @param lineNumber 1-based position on the printed document
 * @param unit what the quantity counts — "m²", "día", "unidad"; free text set by the fee rule
 */
public record InvoiceLine(
        String id,
        String invoiceId,
        int lineNumber,
        String concept,
        Optional<String> description,
        ChargeType chargeType,
        long quantity,
        Optional<String> unit,
        Money unitPrice,
        Money discount,
        Money surcharge,
        Money lineTotal) {

    public InvoiceLine {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(invoiceId, "invoiceId");
        Objects.requireNonNull(concept, "concept");
        Objects.requireNonNull(chargeType, "chargeType");
        Objects.requireNonNull(unitPrice, "unitPrice");
        Objects.requireNonNull(discount, "discount");
        Objects.requireNonNull(surcharge, "surcharge");
        Objects.requireNonNull(lineTotal, "lineTotal");
        if (lineNumber < 1) {
            throw new IllegalArgumentException("lineNumber must be >= 1");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be >= 0");
        }
        description = description == null ? Optional.empty() : description;
        unit = unit == null ? Optional.empty() : unit;
        requireSameCurrency(unitPrice, discount, surcharge, lineTotal);
    }

    /** Build a line from a fee-engine {@link ChargeLine}, computing and freezing its total. */
    public static InvoiceLine from(String id, String invoiceId, int lineNumber, ChargeLine charge) {
        return new InvoiceLine(
                id,
                invoiceId,
                lineNumber,
                charge.concept(),
                Optional.empty(),
                charge.chargeType(),
                charge.quantity(),
                Optional.empty(),
                charge.unitPrice(),
                charge.discount(),
                charge.surcharge(),
                charge.lineTotal());
    }

    private static void requireSameCurrency(Money... amounts) {
        var currency = amounts[0].currency();
        for (Money amount : amounts) {
            if (!amount.currency().equals(currency)) {
                throw new IllegalArgumentException("An invoice line must use one currency");
            }
        }
    }
}
