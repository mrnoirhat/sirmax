// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.document;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.shared.Money;

/**
 * Everything needed to reproduce a printed document, frozen at the moment it was issued (master
 * prompt §59F).
 *
 * <p>This is the load-bearing idea of the whole printing phase. A reprint renders <b>this</b>, never
 * today's data: the municipality's logo and address are copied in, not looked up, so a rebrand in
 * 2029 leaves the 2026 invoices exactly as they were issued. The same goes for the citizen's name —
 * people marry, correct their cédula, and get merged into other records, and none of that may
 * rewrite a document already in someone's hands.
 *
 * <p>Every amount is a {@link Money}, so a reprint cannot drift by a rounding rule that changed
 * after the fact.
 */
public record DocumentSnapshot(
        DocumentKind kind,
        String documentNumber,
        Instant issuedAt,
        Institution institution,
        Customer customer,
        List<Line> lines,
        Totals totals,
        Optional<PaymentInfo> payment,
        Optional<String> reference,
        Optional<String> issuedByName,
        Optional<String> footerNote,
        String verificationCode) {

    /** The municipality's identity as it was when the document was issued (§46, §59C). */
    public record Institution(
            String name,
            Optional<String> department,
            Optional<String> municipality,
            Optional<String> legalIdentifier,
            Optional<String> address,
            Optional<String> phone,
            Optional<String> email,
            Optional<String> website,
            Optional<String> logoPath) {

        public Institution {
            Objects.requireNonNull(name, "name");
            department = orEmpty(department);
            municipality = orEmpty(municipality);
            legalIdentifier = orEmpty(legalIdentifier);
            address = orEmpty(address);
            phone = orEmpty(phone);
            email = orEmpty(email);
            website = orEmpty(website);
            logoPath = orEmpty(logoPath);
        }

        public static Institution named(String name) {
            return new Institution(
                    name,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty());
        }
    }

    /** The citizen or business as named on the document (§59A.1, §59B.2). */
    public record Customer(
            String name,
            Optional<String> identificationType,
            Optional<String> identificationNumber,
            Optional<String> address,
            Optional<String> phone) {

        public Customer {
            Objects.requireNonNull(name, "name");
            identificationType = orEmpty(identificationType);
            identificationNumber = orEmpty(identificationNumber);
            address = orEmpty(address);
            phone = orEmpty(phone);
        }

        public static Customer named(String name) {
            return new Customer(
                    name, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        }
    }

    /** One detail row, matching the §59B.2 column set. */
    public record Line(
            String concept,
            Optional<String> description,
            long quantity,
            Optional<String> unit,
            Money unitPrice,
            Money discount,
            Money surcharge,
            Money lineTotal) {

        public Line {
            Objects.requireNonNull(concept, "concept");
            Objects.requireNonNull(unitPrice, "unitPrice");
            Objects.requireNonNull(discount, "discount");
            Objects.requireNonNull(surcharge, "surcharge");
            Objects.requireNonNull(lineTotal, "lineTotal");
            description = orEmpty(description);
            unit = orEmpty(unit);
        }
    }

    /** The totals block (§59B.2). */
    public record Totals(
            Money subtotal, Money discount, Money surcharge, Money total, Money paid, Money balance) {

        public Totals {
            Objects.requireNonNull(subtotal, "subtotal");
            Objects.requireNonNull(discount, "discount");
            Objects.requireNonNull(surcharge, "surcharge");
            Objects.requireNonNull(total, "total");
            Objects.requireNonNull(paid, "paid");
            Objects.requireNonNull(balance, "balance");
        }

        public boolean isSettled() {
            return balance.isZero();
        }
    }

    /** The payment block (§59B.2), present once money has been collected. */
    public record PaymentInfo(
            String method,
            Money amount,
            Optional<Money> tendered,
            Optional<Money> change,
            Optional<String> reference,
            Instant paidAt,
            Optional<String> cashierName) {

        public PaymentInfo {
            Objects.requireNonNull(method, "method");
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(paidAt, "paidAt");
            tendered = tendered == null ? Optional.empty() : tendered;
            change = change == null ? Optional.empty() : change;
            reference = orEmpty(reference);
            cashierName = orEmpty(cashierName);
        }
    }

    public DocumentSnapshot {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(documentNumber, "documentNumber");
        Objects.requireNonNull(issuedAt, "issuedAt");
        Objects.requireNonNull(institution, "institution");
        Objects.requireNonNull(customer, "customer");
        Objects.requireNonNull(totals, "totals");
        Objects.requireNonNull(verificationCode, "verificationCode");
        lines = lines == null ? List.of() : List.copyOf(lines);
        payment = payment == null ? Optional.empty() : payment;
        reference = orEmpty(reference);
        issuedByName = orEmpty(issuedByName);
        footerNote = orEmpty(footerNote);
    }

    private static Optional<String> orEmpty(Optional<String> v) {
        if (v == null || v.isEmpty()) {
            return Optional.empty();
        }
        String s = v.get().strip();
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }
}
