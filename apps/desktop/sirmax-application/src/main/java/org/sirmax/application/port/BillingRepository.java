// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.util.List;
import java.util.Optional;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.finance.CashSession;
import org.sirmax.domain.finance.Invoice;
import org.sirmax.domain.finance.InvoiceLine;
import org.sirmax.domain.finance.InvoiceStatus;
import org.sirmax.domain.finance.Payment;
import org.sirmax.domain.finance.Refund;
import org.sirmax.shared.Money;

/**
 * Persistence for invoices, their lines, payments, refunds and cash sessions (master prompt §59A).
 *
 * <p>One port, because these are written together inside a single transaction — issuing an invoice
 * writes its lines, taking a payment updates the invoice — and splitting them would only invite a
 * caller to commit half a financial fact.
 */
public interface BillingRepository {

    // ── invoices ──

    /** Save the invoice and replace its lines. */
    void save(Invoice invoice);

    Optional<Invoice> findInvoiceById(String id);

    Optional<Invoice> findInvoiceByNumber(String number);

    /** The invoices raised for one case; a case can be billed more than once (§59A). */
    List<Invoice> findInvoicesByProcedure(String procedureId);

    List<Invoice> findInvoicesByCustomer(PartyRef customer, int limit);

    /** Invoices in the given statuses, newest first. Empty {@code statuses} means "any". */
    List<Invoice> listInvoices(List<InvoiceStatus> statuses, int limit, int offset);

    List<InvoiceLine> findLines(String invoiceId);

    // ── payments and refunds ──

    void save(Payment payment);

    Optional<Payment> findPaymentById(String id);

    List<Payment> findPaymentsByInvoice(String invoiceId);

    List<Payment> findPaymentsBySession(String cashSessionId);

    void save(Refund refund);

    List<Refund> findRefundsByPayment(String paymentId);

    List<Refund> findRefundsBySession(String cashSessionId);

    // ── cash sessions ──

    void save(CashSession session);

    Optional<CashSession> findSessionById(String id);

    /** The cashier's currently open session, if they have one. A cashier has at most one. */
    Optional<CashSession> findOpenSessionFor(String cashierUserId);

    List<CashSession> listSessions(int limit, int offset);

    /**
     * Settled cash collected in a session, for reconciliation. Only
     * {@link org.sirmax.domain.finance.PaymentMethod#CASH} counts — nothing else entered the drawer.
     */
    Money cashCollectedIn(String cashSessionId, String currencyCode);

    /** Cash paid back out of the drawer in a session. */
    Money cashRefundedIn(String cashSessionId, String currencyCode);
}
