// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.finance;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.shared.Money;

/**
 * A municipal invoice (master prompt §59A).
 *
 * <p>The central rule, and the reason this class guards so much: <b>an issued invoice's financial
 * history never changes silently.</b> Lines can only be added while the invoice is {@code DRAFT};
 * once issued, the totals are frozen and every correction leaves a trace — a void, a refund, or a
 * new invoice. That is what makes the drawer reconcilable and the archive trustworthy.
 *
 * <p>The customer's name and identification are <em>snapshots</em> taken at issue time, not
 * lookups: reprinting a 2026 invoice in 2031 must show what it showed in 2026, even if the citizen
 * has since married, corrected their cédula, or been merged into another record.
 *
 * <p>All arithmetic is integer minor units through {@link Money}; there is no floating point
 * anywhere in this file (§2.3).
 */
public final class Invoice {

    private final String id;
    private String number; // null until issued — a number is never spent on a draft
    private final String series;
    private Integer fiscalYear; // set at issue

    private final String procedureId; // nullable — a counter sale need not have a case
    private final String serviceDefinitionId; // nullable

    private final PartyRef customer;
    private String customerName;
    private String customerIdNumber; // nullable

    private InvoiceStatus status;
    private final Currency currency;
    private final List<InvoiceLine> lines = new ArrayList<>();
    private Money discount;
    private Money surcharge;
    private Money paid;

    private String cashierUserId; // nullable until issued
    private String cashSessionId; // nullable

    private Instant issuedAt; // nullable
    private Instant voidedAt; // nullable
    private String voidReason; // nullable
    private String notes; // nullable
    private final Instant createdAt;
    private Instant updatedAt;

    public Invoice(
            String id,
            String number,
            String series,
            Integer fiscalYear,
            String procedureId,
            String serviceDefinitionId,
            PartyRef customer,
            String customerName,
            String customerIdNumber,
            InvoiceStatus status,
            Currency currency,
            Money discount,
            Money surcharge,
            Money paid,
            String cashierUserId,
            String cashSessionId,
            Instant issuedAt,
            Instant voidedAt,
            String voidReason,
            String notes,
            Instant createdAt,
            Instant updatedAt) {
        this.id = requireText(id, "id");
        this.number = blankToNull(number);
        this.series = requireText(series, "series");
        this.fiscalYear = fiscalYear;
        this.procedureId = blankToNull(procedureId);
        this.serviceDefinitionId = blankToNull(serviceDefinitionId);
        this.customer = Objects.requireNonNull(customer, "customer");
        this.customerName = requireText(customerName, "customerName");
        this.customerIdNumber = blankToNull(customerIdNumber);
        this.status = Objects.requireNonNull(status, "status");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.discount = requireCurrency(discount, currency, "discount");
        this.surcharge = requireCurrency(surcharge, currency, "surcharge");
        this.paid = requireCurrency(paid, currency, "paid");
        this.cashierUserId = blankToNull(cashierUserId);
        this.cashSessionId = blankToNull(cashSessionId);
        this.issuedAt = issuedAt;
        this.voidedAt = voidedAt;
        this.voidReason = blankToNull(voidReason);
        this.notes = blankToNull(notes);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** A new draft for a citizen, with no lines yet. */
    public static Invoice draft(
            String id,
            PartyRef customer,
            String customerName,
            String customerIdNumber,
            String procedureId,
            String serviceDefinitionId,
            Currency currency,
            Instant now) {
        Money zero = Money.zero(currency);
        return new Invoice(
                id,
                null,
                "A",
                null,
                procedureId,
                serviceDefinitionId,
                customer,
                customerName,
                customerIdNumber,
                InvoiceStatus.DRAFT,
                currency,
                zero,
                zero,
                zero,
                null,
                null,
                null,
                null,
                null,
                null,
                now,
                now);
    }

    // ── identity and read model ──

    public String id() {
        return id;
    }

    /** The public invoice number; empty while the invoice is still a draft. */
    public Optional<String> number() {
        return Optional.ofNullable(number);
    }

    public String series() {
        return series;
    }

    public Optional<Integer> fiscalYear() {
        return Optional.ofNullable(fiscalYear);
    }

    public Optional<String> procedureId() {
        return Optional.ofNullable(procedureId);
    }

    public Optional<String> serviceDefinitionId() {
        return Optional.ofNullable(serviceDefinitionId);
    }

    public PartyRef customer() {
        return customer;
    }

    public String customerName() {
        return customerName;
    }

    public Optional<String> customerIdNumber() {
        return Optional.ofNullable(customerIdNumber);
    }

    public InvoiceStatus status() {
        return status;
    }

    public Currency currency() {
        return currency;
    }

    public List<InvoiceLine> lines() {
        return List.copyOf(lines);
    }

    /** Sum of the line totals, before invoice-level discount and surcharge. */
    public Money subtotal() {
        Money sum = Money.zero(currency);
        for (InvoiceLine line : lines) {
            sum = sum.plus(line.lineTotal());
        }
        return sum;
    }

    public Money discount() {
        return discount;
    }

    public Money surcharge() {
        return surcharge;
    }

    /** What the citizen owes: {@code subtotal − discount + surcharge}. */
    public Money total() {
        return subtotal().minus(discount).plus(surcharge);
    }

    public Money paid() {
        return paid;
    }

    /** What is still owed; never negative — an overpayment shows as change, not a negative balance. */
    public Money balance() {
        Money outstanding = total().minus(paid);
        return outstanding.isNegative() ? Money.zero(currency) : outstanding;
    }

    /** Collected beyond the total — the change the cashier owes back, or a credit to refund. */
    public Money overpayment() {
        Money excess = paid.minus(total());
        return excess.isPositive() ? excess : Money.zero(currency);
    }

    public boolean isSettled() {
        return balance().isZero();
    }

    public Optional<String> cashierUserId() {
        return Optional.ofNullable(cashierUserId);
    }

    public Optional<String> cashSessionId() {
        return Optional.ofNullable(cashSessionId);
    }

    public Optional<Instant> issuedAt() {
        return Optional.ofNullable(issuedAt);
    }

    public Optional<Instant> voidedAt() {
        return Optional.ofNullable(voidedAt);
    }

    public Optional<String> voidReason() {
        return Optional.ofNullable(voidReason);
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

    // ── draft editing ──

    /** Append a line. Only a draft can gain lines (§59A.2). */
    public InvoiceLine addLine(String lineId, ChargeLine charge) {
        requireDraft();
        if (!charge.unitPrice().currency().equals(currency)) {
            throw new IllegalArgumentException(
                    "Line currency " + charge.unitPrice().currency().getCurrencyCode()
                            + " does not match invoice currency " + currency.getCurrencyCode());
        }
        InvoiceLine line = InvoiceLine.from(lineId, id, lines.size() + 1, charge);
        lines.add(line);
        return line;
    }

    /** Restore persisted lines when rehydrating; bypasses the draft guard by design. */
    public void restoreLines(List<InvoiceLine> persisted) {
        lines.clear();
        lines.addAll(persisted);
    }

    /**
     * An invoice-level discount, e.g. an exemption a supervisor approved. Requires
     * {@code fee.override} at the use-case boundary; here it only has to be legal arithmetic.
     */
    public void setDiscount(Money value, Instant now) {
        requireDraft();
        Money amount = requireCurrency(value, currency, "discount");
        if (amount.isNegative()) {
            throw new IllegalArgumentException("A discount must not be negative");
        }
        if (amount.compareTo(subtotal().plus(surcharge)) > 0) {
            throw new IllegalArgumentException("A discount must not exceed the amount billed");
        }
        this.discount = amount;
        touch(now);
    }

    public void setSurcharge(Money value, Instant now) {
        requireDraft();
        Money amount = requireCurrency(value, currency, "surcharge");
        if (amount.isNegative()) {
            throw new IllegalArgumentException("A surcharge must not be negative");
        }
        this.surcharge = amount;
        touch(now);
    }

    public void setCustomerSnapshot(String name, String idNumber, Instant now) {
        requireDraft();
        this.customerName = requireText(name, "customerName");
        this.customerIdNumber = blankToNull(idNumber);
        touch(now);
    }

    public void setNotes(String value, Instant now) {
        this.notes = blankToNull(value);
        touch(now);
    }

    // ── lifecycle ──

    /**
     * Issue the invoice: assign its public number and freeze it. A zero-total invoice is refused —
     * a free service should not produce a document that asks for nothing (§12).
     */
    public void issue(String allocatedNumber, int year, String cashier, String sessionId, Instant now) {
        requireDraft();
        if (lines.isEmpty()) {
            throw new IllegalStateException("An invoice must have at least one line");
        }
        if (!total().isPositive()) {
            throw new IllegalStateException("An invoice total must be positive");
        }
        this.number = requireText(allocatedNumber, "number");
        this.fiscalYear = year;
        this.cashierUserId = blankToNull(cashier);
        this.cashSessionId = blankToNull(sessionId);
        this.issuedAt = now;
        this.status = InvoiceStatus.ISSUED;
        touch(now);
    }

    /**
     * Record money collected. Returns the change owed when the citizen paid more than the balance.
     *
     * @throws IllegalStateException if the invoice cannot accept payment
     */
    public Money applyPayment(Money amount, Instant now) {
        if (status == InvoiceStatus.VOIDED || status == InvoiceStatus.REFUNDED) {
            throw new IllegalStateException("Invoice " + number + " is " + status);
        }
        if (status == InvoiceStatus.DRAFT) {
            throw new IllegalStateException("A draft invoice cannot be paid");
        }
        Money value = requireCurrency(amount, currency, "amount");
        if (!value.isPositive()) {
            throw new IllegalArgumentException("A payment must be positive");
        }

        Money change =
                value.compareTo(balance()) > 0 ? value.minus(balance()) : Money.zero(currency);
        // Only what was actually owed is recorded against the invoice; the excess is change handed
        // back at the counter, not municipal income.
        paid = paid.plus(value.minus(change));
        status = isSettled() ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID;
        touch(now);
        return change;
    }

    /** Reverse a payment (a refund, or a mistaken entry the cashier is undoing). */
    public void reversePayment(Money amount, Instant now) {
        Money value = requireCurrency(amount, currency, "amount");
        if (value.compareTo(paid) > 0) {
            throw new IllegalArgumentException("Cannot reverse more than was collected");
        }
        paid = paid.minus(value);
        if (paid.isZero()) {
            status = status == InvoiceStatus.VOIDED ? InvoiceStatus.VOIDED : InvoiceStatus.REFUNDED;
        } else {
            status = isSettled() ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID;
        }
        touch(now);
    }

    /**
     * Void the invoice. A paid invoice must be refunded first: voiding one with money against it
     * would leave the drawer holding cash no document accounts for.
     */
    public void voidInvoice(String reason, Instant now) {
        if (status == InvoiceStatus.VOIDED) {
            throw new IllegalStateException("The invoice is already voided");
        }
        if (paid.isPositive()) {
            throw new IllegalStateException("Refund the collected amount before voiding");
        }
        String why = blankToNull(reason);
        if (why == null) {
            throw new IllegalArgumentException("A void must carry a reason");
        }
        this.status = InvoiceStatus.VOIDED;
        this.voidedAt = now;
        this.voidReason = why;
        touch(now);
        // The number stays spent: §27 forbids reuse after a void.
    }

    private void requireDraft() {
        if (status.isFinanciallyFrozen()) {
            throw new IllegalStateException(
                    "Invoice " + Optional.ofNullable(number).orElse(id) + " is " + status
                            + " and its financial history must not change");
        }
    }

    private void touch(Instant now) {
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    private static Money requireCurrency(Money value, Currency expected, String field) {
        Objects.requireNonNull(value, field);
        if (!value.currency().equals(expected)) {
            throw new IllegalArgumentException(
                    field + " must be in " + expected.getCurrencyCode());
        }
        return value;
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
        return o instanceof Invoice other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
