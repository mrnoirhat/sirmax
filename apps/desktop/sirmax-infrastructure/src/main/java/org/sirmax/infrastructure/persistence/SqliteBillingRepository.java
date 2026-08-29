// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.sirmax.infrastructure.persistence.JdbcHelper.instant;
import static org.sirmax.infrastructure.persistence.JdbcHelper.str;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import org.sirmax.application.port.BillingRepository;
import org.sirmax.application.port.ProcedureFinance;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.common.PartyType;
import org.sirmax.domain.finance.CashSession;
import org.sirmax.domain.finance.ChargeType;
import org.sirmax.domain.finance.Invoice;
import org.sirmax.domain.finance.InvoiceLine;
import org.sirmax.domain.finance.InvoiceStatus;
import org.sirmax.domain.finance.Payment;
import org.sirmax.domain.finance.PaymentMethod;
import org.sirmax.domain.finance.Refund;
import org.sirmax.shared.Money;

/**
 * SQLite persistence for billing (master prompt §59A).
 *
 * <p>Money crosses this boundary as {@code *_minor INTEGER} plus a {@code currency TEXT(3)} — never
 * a REAL. The totals an invoice computes are written alongside its lines so a reprint reads back the
 * frozen figures instead of recomputing them from rules that may since have changed (§59F).
 *
 * <p>Also implements {@link ProcedureFinance}: the three booleans the workflow's payment checkpoint
 * needs, answered from the invoice table rather than by duplicating billing state onto the case.
 */
public final class SqliteBillingRepository implements BillingRepository, ProcedureFinance {

    private final SqliteDatabase db;

    public SqliteBillingRepository(SqliteDatabase db) {
        this.db = db;
    }

    // ── invoices ──

    @Override
    public void save(Invoice invoice) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO invoice"
                    + " (id, number, series, fiscal_year, procedure_id, service_definition_id,"
                    + "  customer_type, customer_id, customer_name, customer_id_number, status,"
                    + "  currency, subtotal_minor, discount_minor, surcharge_minor, total_minor,"
                    + "  paid_minor, cashier_user_id, cash_session_id, issued_at, voided_at,"
                    + "  void_reason, notes, created_at, updated_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT(id) DO UPDATE SET number=excluded.number,"
                    + " fiscal_year=excluded.fiscal_year, customer_name=excluded.customer_name,"
                    + " customer_id_number=excluded.customer_id_number, status=excluded.status,"
                    + " subtotal_minor=excluded.subtotal_minor, discount_minor=excluded.discount_minor,"
                    + " surcharge_minor=excluded.surcharge_minor, total_minor=excluded.total_minor,"
                    + " paid_minor=excluded.paid_minor, cashier_user_id=excluded.cashier_user_id,"
                    + " cash_session_id=excluded.cash_session_id, issued_at=excluded.issued_at,"
                    + " voided_at=excluded.voided_at, void_reason=excluded.void_reason,"
                    + " notes=excluded.notes, updated_at=excluded.updated_at",
                invoice.id(),
                invoice.number().orElse(null),
                invoice.series(),
                invoice.fiscalYear().orElse(null),
                invoice.procedureId().orElse(null),
                invoice.serviceDefinitionId().orElse(null),
                invoice.customer().type().name(),
                invoice.customer().id(),
                invoice.customerName(),
                invoice.customerIdNumber().orElse(null),
                invoice.status().name(),
                invoice.currency().getCurrencyCode(),
                invoice.subtotal().minorUnits(),
                invoice.discount().minorUnits(),
                invoice.surcharge().minorUnits(),
                invoice.total().minorUnits(),
                invoice.paid().minorUnits(),
                invoice.cashierUserId().orElse(null),
                invoice.cashSessionId().orElse(null),
                invoice.issuedAt().orElse(null),
                invoice.voidedAt().orElse(null),
                invoice.voidReason().orElse(null),
                invoice.notes().orElse(null),
                invoice.createdAt(),
                invoice.updatedAt());

        // Lines are frozen once issued, so a full replace is safe and keeps the two in step.
        JdbcHelper.update(
                db.connection(), "DELETE FROM invoice_line WHERE invoice_id = ?", invoice.id());
        for (InvoiceLine line : invoice.lines()) {
            saveLine(line);
        }
    }

    private void saveLine(InvoiceLine line) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO invoice_line"
                        + " (id, invoice_id, line_number, concept, description, charge_type,"
                        + "  quantity, unit, unit_price_minor, discount_minor, surcharge_minor,"
                        + "  line_total_minor)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                line.id(),
                line.invoiceId(),
                line.lineNumber(),
                line.concept(),
                line.description().orElse(null),
                line.chargeType().name(),
                line.quantity(),
                line.unit().orElse(null),
                line.unitPrice().minorUnits(),
                line.discount().minorUnits(),
                line.surcharge().minorUnits(),
                line.lineTotal().minorUnits());
    }

    @Override
    public Optional<Invoice> findInvoiceById(String id) {
        return JdbcHelper.queryOne(
                        db.connection(),
                        "SELECT * FROM invoice WHERE id = ?",
                        SqliteBillingRepository::mapInvoice,
                        id)
                .map(this::withLines);
    }

    @Override
    public Optional<Invoice> findInvoiceByNumber(String number) {
        return JdbcHelper.queryOne(
                        db.connection(),
                        "SELECT * FROM invoice WHERE number = ?",
                        SqliteBillingRepository::mapInvoice,
                        number)
                .map(this::withLines);
    }

    @Override
    public List<Invoice> findInvoicesByProcedure(String procedureId) {
        return JdbcHelper.queryList(
                        db.connection(),
                        "SELECT * FROM invoice WHERE procedure_id = ? ORDER BY created_at",
                        SqliteBillingRepository::mapInvoice,
                        procedureId)
                .stream()
                .map(this::withLines)
                .toList();
    }

    @Override
    public List<Invoice> findInvoicesByCustomer(PartyRef customer, int limit) {
        return JdbcHelper.queryList(
                        db.connection(),
                        "SELECT * FROM invoice WHERE customer_type = ? AND customer_id = ?"
                                + " ORDER BY created_at DESC LIMIT ?",
                        SqliteBillingRepository::mapInvoice,
                        customer.type().name(),
                        customer.id(),
                        limit)
                .stream()
                .map(this::withLines)
                .toList();
    }

    @Override
    public List<Invoice> listInvoices(List<InvoiceStatus> statuses, int limit, int offset) {
        if (statuses == null || statuses.isEmpty()) {
            return JdbcHelper.queryList(
                            db.connection(),
                            "SELECT * FROM invoice ORDER BY created_at DESC LIMIT ? OFFSET ?",
                            SqliteBillingRepository::mapInvoice,
                            limit,
                            offset)
                    .stream()
                    .map(this::withLines)
                    .toList();
        }
        List<Object> params = new ArrayList<>(statuses.stream().map(Enum::name).toList());
        params.add(limit);
        params.add(offset);
        return JdbcHelper.queryList(
                        db.connection(),
                        "SELECT * FROM invoice WHERE status IN ("
                                + String.join(",", java.util.Collections.nCopies(statuses.size(), "?"))
                                + ") ORDER BY created_at DESC LIMIT ? OFFSET ?",
                        SqliteBillingRepository::mapInvoice,
                        params.toArray())
                .stream()
                .map(this::withLines)
                .toList();
    }

    @Override
    public List<InvoiceLine> findLines(String invoiceId) {
        // The invoice's currency is joined in: a line never differs from its parent, and
        // carrying it here avoids a per-row lookup.
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT l.*, i.currency FROM invoice_line l"
                        + " JOIN invoice i ON i.id = l.invoice_id"
                        + " WHERE l.invoice_id = ? ORDER BY l.line_number",
                SqliteBillingRepository::mapLine,
                invoiceId);
    }

    private Invoice withLines(Invoice invoice) {
        invoice.restoreLines(findLines(invoice.id()));
        return invoice;
    }

    // ── payments and refunds ──

    @Override
    public void save(Payment p) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO payment"
                        + " (id, code, invoice_id, cash_session_id, method, currency, amount_minor,"
                        + "  tendered_minor, reference, payer_name, status, received_by,"
                        + "  received_at, notes)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        + " ON CONFLICT(id) DO UPDATE SET status=excluded.status,"
                        + " notes=excluded.notes",
                p.id(),
                p.code(),
                p.invoiceId(),
                p.cashSessionId().orElse(null),
                p.method().name(),
                p.amount().currency().getCurrencyCode(),
                p.amount().minorUnits(),
                p.tendered().map(Money::minorUnits).orElse(null),
                p.reference().orElse(null),
                p.payerName().orElse(null),
                p.status().name(),
                p.receivedBy().orElse(null),
                p.receivedAt(),
                p.notes().orElse(null));
    }

    @Override
    public Optional<Payment> findPaymentById(String id) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM payment WHERE id = ?",
                SqliteBillingRepository::mapPayment,
                id);
    }

    @Override
    public List<Payment> findPaymentsByInvoice(String invoiceId) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM payment WHERE invoice_id = ? ORDER BY received_at",
                SqliteBillingRepository::mapPayment,
                invoiceId);
    }

    @Override
    public List<Payment> findPaymentsBySession(String cashSessionId) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM payment WHERE cash_session_id = ? ORDER BY received_at",
                SqliteBillingRepository::mapPayment,
                cashSessionId);
    }

    @Override
    public void save(Refund r) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO refund"
                        + " (id, code, payment_id, invoice_id, cash_session_id, currency,"
                        + "  amount_minor, reason, authorized_by, refunded_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                r.id(),
                r.code(),
                r.paymentId(),
                r.invoiceId(),
                r.cashSessionId().orElse(null),
                r.amount().currency().getCurrencyCode(),
                r.amount().minorUnits(),
                r.reason(),
                r.authorizedBy().orElse(null),
                r.refundedAt());
    }

    @Override
    public List<Refund> findRefundsByPayment(String paymentId) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM refund WHERE payment_id = ? ORDER BY refunded_at",
                SqliteBillingRepository::mapRefund,
                paymentId);
    }

    @Override
    public List<Refund> findRefundsBySession(String cashSessionId) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM refund WHERE cash_session_id = ? ORDER BY refunded_at",
                SqliteBillingRepository::mapRefund,
                cashSessionId);
    }

    // ── cash sessions ──

    @Override
    public void save(CashSession s) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO cash_session"
                        + " (id, code, cashier_user_id, department_id, status, currency,"
                        + "  opening_float_minor, counted_total_minor, opened_at, closed_at, notes)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        + " ON CONFLICT(id) DO UPDATE SET status=excluded.status,"
                        + " counted_total_minor=excluded.counted_total_minor,"
                        + " closed_at=excluded.closed_at, notes=excluded.notes",
                s.id(),
                s.code(),
                s.cashierUserId(),
                s.departmentId().orElse(null),
                s.status().name(),
                s.currency().getCurrencyCode(),
                s.openingFloat().minorUnits(),
                s.countedTotal().map(Money::minorUnits).orElse(null),
                s.openedAt(),
                s.closedAt().orElse(null),
                s.notes().orElse(null));
    }

    @Override
    public Optional<CashSession> findSessionById(String id) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM cash_session WHERE id = ?",
                SqliteBillingRepository::mapSession,
                id);
    }

    @Override
    public Optional<CashSession> findOpenSessionFor(String cashierUserId) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM cash_session WHERE cashier_user_id = ? AND status = 'OPEN'"
                        + " ORDER BY opened_at DESC LIMIT 1",
                SqliteBillingRepository::mapSession,
                cashierUserId);
    }

    @Override
    public List<CashSession> listSessions(int limit, int offset) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM cash_session ORDER BY opened_at DESC LIMIT ? OFFSET ?",
                SqliteBillingRepository::mapSession,
                limit,
                offset);
    }

    @Override
    public Money cashCollectedIn(String cashSessionId, String currencyCode) {
        long minor =
                JdbcHelper.queryLong(
                        db.connection(),
                        "SELECT coalesce(sum(amount_minor), 0) FROM payment"
                                + " WHERE cash_session_id = ? AND method = 'CASH'"
                                + " AND status = 'SETTLED' AND currency = ?",
                        cashSessionId,
                        currencyCode);
        return new Money(minor, Currency.getInstance(currencyCode));
    }

    @Override
    public Money cashRefundedIn(String cashSessionId, String currencyCode) {
        // A refund is attributed to the session that took the money, which may not be the session
        // open when it was returned; reconciling the original drawer is what matters.
        long minor =
                JdbcHelper.queryLong(
                        db.connection(),
                        "SELECT coalesce(sum(r.amount_minor), 0) FROM refund r"
                                + " JOIN payment p ON p.id = r.payment_id"
                                + " WHERE r.cash_session_id = ? AND p.method = 'CASH'"
                                + " AND r.currency = ?",
                        cashSessionId,
                        currencyCode);
        return new Money(minor, Currency.getInstance(currencyCode));
    }

    // ── ProcedureFinance ──

    @Override
    public PaymentState stateOf(String procedureId) {
        List<Invoice> invoices =
                findInvoicesByProcedure(procedureId).stream()
                        .filter(i -> i.status() != InvoiceStatus.VOIDED)
                        .toList();
        if (invoices.isEmpty()) {
            return PaymentState.notInvoiced();
        }
        boolean allSettled = invoices.stream().allMatch(Invoice::isSettled);
        boolean anythingCollected = invoices.stream().anyMatch(i -> i.paid().isPositive());
        return new PaymentState(true, allSettled, !allSettled && anythingCollected);
    }

    // ── row mappers ──

    private static Invoice mapInvoice(ResultSet rs) throws SQLException {
        Currency currency = Currency.getInstance(rs.getString("currency"));
        Integer fiscalYear = rs.getObject("fiscal_year") == null ? null : rs.getInt("fiscal_year");
        return new Invoice(
                rs.getString("id"),
                str(rs, "number"),
                rs.getString("series"),
                fiscalYear,
                str(rs, "procedure_id"),
                str(rs, "service_definition_id"),
                new PartyRef(
                        PartyType.valueOf(rs.getString("customer_type")),
                        rs.getString("customer_id")),
                rs.getString("customer_name"),
                str(rs, "customer_id_number"),
                InvoiceStatus.valueOf(rs.getString("status")),
                currency,
                new Money(rs.getLong("discount_minor"), currency),
                new Money(rs.getLong("surcharge_minor"), currency),
                new Money(rs.getLong("paid_minor"), currency),
                str(rs, "cashier_user_id"),
                str(rs, "cash_session_id"),
                instant(rs, "issued_at"),
                instant(rs, "voided_at"),
                str(rs, "void_reason"),
                str(rs, "notes"),
                instant(rs, "created_at"),
                instant(rs, "updated_at"));
    }

    private static InvoiceLine mapLine(ResultSet rs) throws SQLException {
        Currency currency = Currency.getInstance(rs.getString("currency"));
        return new InvoiceLine(
                rs.getString("id"),
                rs.getString("invoice_id"),
                rs.getInt("line_number"),
                rs.getString("concept"),
                Optional.ofNullable(str(rs, "description")),
                ChargeType.valueOf(rs.getString("charge_type")),
                rs.getLong("quantity"),
                Optional.ofNullable(str(rs, "unit")),
                new Money(rs.getLong("unit_price_minor"), currency),
                new Money(rs.getLong("discount_minor"), currency),
                new Money(rs.getLong("surcharge_minor"), currency),
                new Money(rs.getLong("line_total_minor"), currency));
    }

    private static Payment mapPayment(ResultSet rs) throws SQLException {
        Currency currency = Currency.getInstance(rs.getString("currency"));
        Long tendered =
                rs.getObject("tendered_minor") == null ? null : rs.getLong("tendered_minor");
        return new Payment(
                rs.getString("id"),
                rs.getString("code"),
                rs.getString("invoice_id"),
                Optional.ofNullable(str(rs, "cash_session_id")),
                PaymentMethod.valueOf(rs.getString("method")),
                new Money(rs.getLong("amount_minor"), currency),
                Optional.ofNullable(tendered).map(t -> new Money(t, currency)),
                Optional.ofNullable(str(rs, "reference")),
                Optional.ofNullable(str(rs, "payer_name")),
                Payment.Status.valueOf(rs.getString("status")),
                Optional.ofNullable(str(rs, "received_by")),
                instant(rs, "received_at"),
                Optional.ofNullable(str(rs, "notes")));
    }

    private static Refund mapRefund(ResultSet rs) throws SQLException {
        Currency currency = Currency.getInstance(rs.getString("currency"));
        return new Refund(
                rs.getString("id"),
                rs.getString("code"),
                rs.getString("payment_id"),
                rs.getString("invoice_id"),
                Optional.ofNullable(str(rs, "cash_session_id")),
                new Money(rs.getLong("amount_minor"), currency),
                rs.getString("reason"),
                Optional.ofNullable(str(rs, "authorized_by")),
                instant(rs, "refunded_at"));
    }

    private static CashSession mapSession(ResultSet rs) throws SQLException {
        Currency currency = Currency.getInstance(rs.getString("currency"));
        Long counted =
                rs.getObject("counted_total_minor") == null
                        ? null
                        : rs.getLong("counted_total_minor");
        return new CashSession(
                rs.getString("id"),
                rs.getString("code"),
                rs.getString("cashier_user_id"),
                str(rs, "department_id"),
                CashSession.Status.valueOf(rs.getString("status")),
                currency,
                new Money(rs.getLong("opening_float_minor"), currency),
                counted == null ? null : new Money(counted, currency),
                instant(rs, "opened_at"),
                instant(rs, "closed_at"),
                str(rs, "notes"));
    }
}
