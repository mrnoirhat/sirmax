// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.shared.Money;

class InvoiceTest {

    private static final Instant NOW = Instant.parse("2026-05-04T10:00:00Z");
    private static final Currency DOP = Currency.getInstance("DOP");

    private static Money dop(String amount) {
        return Money.of(amount, "DOP");
    }

    private static Invoice draft() {
        return Invoice.draft(
                "inv-1",
                PartyRef.person("per-1"),
                "José Pérez",
                "001-1234567-8",
                "proc-1",
                "svc-1",
                DOP,
                NOW);
    }

    private static Invoice issued() {
        Invoice invoice = draft();
        invoice.addLine("l-1", ChargeLine.of("Certificación", ChargeType.TASA, 1, dop("500.00")));
        invoice.issue("FACT-2026-000001", 2026, "u-1", "cash-1", NOW);
        return invoice;
    }

    @Test
    void totalsAreExactIntegerArithmetic() {
        Invoice invoice = draft();
        invoice.addLine("l-1", ChargeLine.of("Tasa", ChargeType.TASA, 3, dop("333.33")));
        invoice.setSurcharge(dop("0.01"), NOW);

        assertThat(invoice.subtotal()).isEqualTo(dop("999.99"));
        assertThat(invoice.total()).isEqualTo(dop("1000.00"));
        assertThat(invoice.total().minorUnits()).isEqualTo(100_000L);
    }

    @Test
    void issuingFreezesTheInvoiceAgainstFurtherEditing() {
        Invoice invoice = issued();

        assertThat(invoice.status()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(invoice.number()).contains("FACT-2026-000001");
        assertThatThrownBy(
                        () ->
                                invoice.addLine(
                                        "l-2",
                                        ChargeLine.of("Extra", ChargeType.TASA, 1, dop("100.00"))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> invoice.setDiscount(dop("50.00"), NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void anEmptyOrZeroInvoiceCannotBeIssued() {
        Invoice empty = draft();
        assertThatThrownBy(() -> empty.issue("FACT-1", 2026, "u-1", null, NOW))
                .isInstanceOf(IllegalStateException.class);

        Invoice free = draft();
        free.addLine("l-1", ChargeLine.of("Gratis", ChargeType.TASA, 1, Money.zero(DOP)));
        assertThatThrownBy(() -> free.issue("FACT-1", 2026, "u-1", null, NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aPartialPaymentLeavesABalanceAndMovesTheStatus() {
        Invoice invoice = issued();

        Money change = invoice.applyPayment(dop("200.00"), NOW);

        assertThat(change.isZero()).isTrue();
        assertThat(invoice.status()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
        assertThat(invoice.balance()).isEqualTo(dop("300.00"));
        assertThat(invoice.isSettled()).isFalse();
    }

    @Test
    void overpaymentBecomesChangeRatherThanMunicipalIncome() {
        Invoice invoice = issued();

        Money change = invoice.applyPayment(dop("1000.00"), NOW);

        assertThat(change).isEqualTo(dop("500.00"));
        assertThat(invoice.paid()).isEqualTo(dop("500.00"));
        assertThat(invoice.balance().isZero()).isTrue();
        assertThat(invoice.status()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void aDraftCannotBePaidAndAVoidedInvoiceCannotBeEither() {
        Invoice draft = draft();
        draft.addLine("l-1", ChargeLine.of("Tasa", ChargeType.TASA, 1, dop("500.00")));
        assertThatThrownBy(() -> draft.applyPayment(dop("100.00"), NOW))
                .isInstanceOf(IllegalStateException.class);

        Invoice voided = issued();
        voided.voidInvoice("Emitida por error", NOW);
        assertThatThrownBy(() -> voided.applyPayment(dop("100.00"), NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void voidingNeedsAReasonAndRefusesWhileMoneyIsHeld() {
        Invoice invoice = issued();
        assertThatThrownBy(() -> invoice.voidInvoice("  ", NOW))
                .isInstanceOf(IllegalArgumentException.class);

        invoice.applyPayment(dop("500.00"), NOW);
        assertThatThrownBy(() -> invoice.voidInvoice("Error", NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Refund");
    }

    @Test
    void reversingEverythingCollectedMarksTheInvoiceRefunded() {
        Invoice invoice = issued();
        invoice.applyPayment(dop("500.00"), NOW);

        invoice.reversePayment(dop("500.00"), NOW);

        assertThat(invoice.status()).isEqualTo(InvoiceStatus.REFUNDED);
        assertThat(invoice.paid().isZero()).isTrue();
        assertThat(invoice.balance()).isEqualTo(dop("500.00"));
    }

    @Test
    void aDiscountCannotExceedWhatIsBilled() {
        Invoice invoice = draft();
        invoice.addLine("l-1", ChargeLine.of("Tasa", ChargeType.TASA, 1, dop("500.00")));

        assertThatThrownBy(() -> invoice.setDiscount(dop("500.01"), NOW))
                .isInstanceOf(IllegalArgumentException.class);

        invoice.setDiscount(dop("500.00"), NOW);
        assertThat(invoice.total().isZero()).isTrue();
    }

    @Test
    void aLineInAnotherCurrencyIsRefused() {
        Invoice invoice = draft();

        assertThatThrownBy(
                        () ->
                                invoice.addLine(
                                        "l-1",
                                        ChargeLine.of(
                                                "Tasa",
                                                ChargeType.TASA,
                                                1,
                                                Money.of("10.00", "USD"))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
