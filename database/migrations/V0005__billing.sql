-- SPDX-License-Identifier: AGPL-3.0-or-later
-- SIRMAX billing, payments and cash (Phase 6, master prompt §20, §59A).
--
-- Money is stored as integer minor units plus an ISO-4217 code — never a REAL.
-- A `*_minor INTEGER` + `currency TEXT(3)` pair maps to shared.Money.
--
-- Financial history is append-only in effect: an issued invoice's lines and
-- totals are frozen (enforced in the domain), and corrections go through void,
-- refund or adjustment, each of which leaves its own row.

-- ─────────────────────────────────────────────────────────────
-- Cash session (drawer) — §20, §59A.1
-- ─────────────────────────────────────────────────────────────
CREATE TABLE cash_session (
    id                 TEXT PRIMARY KEY,
    code               TEXT NOT NULL,           -- e.g. CAJA-2026-000001
    cashier_user_id    TEXT NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    department_id      TEXT REFERENCES department(id) ON DELETE SET NULL,
    status             TEXT NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','CLOSED')),
    currency           TEXT NOT NULL DEFAULT 'DOP',
    opening_float_minor INTEGER NOT NULL DEFAULT 0,
    -- what the cashier physically counted at close; the difference against the
    -- expected total is the reconciliation figure, kept rather than corrected
    counted_total_minor INTEGER,
    opened_at          TEXT NOT NULL,
    closed_at          TEXT,
    notes              TEXT
);
CREATE UNIQUE INDEX ux_cash_session_code ON cash_session(code);
CREATE INDEX ix_cash_session_cashier ON cash_session(cashier_user_id, status);

-- ─────────────────────────────────────────────────────────────
-- Invoice — §59A.1
-- ─────────────────────────────────────────────────────────────
CREATE TABLE invoice (
    id                 TEXT PRIMARY KEY,
    number             TEXT,                    -- allocated at issue, never before
    series             TEXT NOT NULL DEFAULT 'A',
    fiscal_year        INTEGER,

    procedure_id       TEXT REFERENCES procedure(id) ON DELETE RESTRICT,
    service_definition_id TEXT REFERENCES service_definition(id) ON DELETE RESTRICT,

    customer_type      TEXT NOT NULL CHECK (customer_type IN ('PERSON','ORGANIZATION')),
    customer_id        TEXT NOT NULL,
    customer_name      TEXT NOT NULL,           -- snapshot: an invoice must reprint identically
    customer_id_number TEXT,                    -- snapshot of the cédula/RNC shown on the document

    status             TEXT NOT NULL DEFAULT 'DRAFT'
                       CHECK (status IN ('DRAFT','ISSUED','PARTIALLY_PAID','PAID','VOIDED','REFUNDED')),
    currency           TEXT NOT NULL DEFAULT 'DOP',
    subtotal_minor     INTEGER NOT NULL DEFAULT 0,
    discount_minor     INTEGER NOT NULL DEFAULT 0,
    surcharge_minor    INTEGER NOT NULL DEFAULT 0,
    total_minor        INTEGER NOT NULL DEFAULT 0,
    paid_minor         INTEGER NOT NULL DEFAULT 0,

    cashier_user_id    TEXT REFERENCES app_user(id) ON DELETE SET NULL,
    cash_session_id    TEXT REFERENCES cash_session(id) ON DELETE SET NULL,

    issued_at          TEXT,
    voided_at          TEXT,
    void_reason        TEXT,
    notes              TEXT,
    created_at         TEXT NOT NULL,
    updated_at         TEXT NOT NULL
);
CREATE UNIQUE INDEX ux_invoice_number ON invoice(number) WHERE number IS NOT NULL;
CREATE INDEX ix_invoice_procedure ON invoice(procedure_id);
CREATE INDEX ix_invoice_customer ON invoice(customer_type, customer_id);
CREATE INDEX ix_invoice_status ON invoice(status);
CREATE INDEX ix_invoice_session ON invoice(cash_session_id);

CREATE TABLE invoice_line (
    id               TEXT PRIMARY KEY,
    invoice_id       TEXT NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
    line_number      INTEGER NOT NULL,
    concept          TEXT NOT NULL,
    description      TEXT,
    charge_type      TEXT NOT NULL,             -- domain.finance.ChargeType
    quantity         INTEGER NOT NULL DEFAULT 1 CHECK (quantity >= 0),
    unit             TEXT,
    unit_price_minor INTEGER NOT NULL DEFAULT 0,
    discount_minor   INTEGER NOT NULL DEFAULT 0,
    surcharge_minor  INTEGER NOT NULL DEFAULT 0,
    line_total_minor INTEGER NOT NULL DEFAULT 0,
    UNIQUE (invoice_id, line_number)
);
CREATE INDEX ix_invoice_line_invoice ON invoice_line(invoice_id);

-- ─────────────────────────────────────────────────────────────
-- Payment — §59A.5
-- ─────────────────────────────────────────────────────────────
CREATE TABLE payment (
    id               TEXT PRIMARY KEY,
    code             TEXT NOT NULL,             -- receipt number, e.g. REC-2026-000001
    invoice_id       TEXT NOT NULL REFERENCES invoice(id) ON DELETE RESTRICT,
    cash_session_id  TEXT REFERENCES cash_session(id) ON DELETE SET NULL,
    method           TEXT NOT NULL
                     CHECK (method IN ('CASH','BANK_TRANSFER','CARD','CHECK','OTHER')),
    currency         TEXT NOT NULL DEFAULT 'DOP',
    amount_minor     INTEGER NOT NULL CHECK (amount_minor > 0),
    -- what the payer handed over in cash; change = tendered - amount. Null for
    -- non-cash methods, where the notion does not apply.
    tendered_minor   INTEGER,
    reference        TEXT,                      -- transfer/cheque/authorization number
    payer_name       TEXT,
    status           TEXT NOT NULL DEFAULT 'SETTLED'
                     CHECK (status IN ('SETTLED','REVERSED')),
    received_by      TEXT REFERENCES app_user(id) ON DELETE SET NULL,
    received_at      TEXT NOT NULL,
    notes            TEXT
);
CREATE UNIQUE INDEX ux_payment_code ON payment(code);
CREATE INDEX ix_payment_invoice ON payment(invoice_id);
CREATE INDEX ix_payment_session ON payment(cash_session_id);

-- A refund reverses money already collected. It never edits the payment row:
-- both stay, so the drawer and the audit trail can be reconciled afterwards.
CREATE TABLE refund (
    id             TEXT PRIMARY KEY,
    code           TEXT NOT NULL,
    payment_id     TEXT NOT NULL REFERENCES payment(id) ON DELETE RESTRICT,
    invoice_id     TEXT NOT NULL REFERENCES invoice(id) ON DELETE RESTRICT,
    cash_session_id TEXT REFERENCES cash_session(id) ON DELETE SET NULL,
    currency       TEXT NOT NULL DEFAULT 'DOP',
    amount_minor   INTEGER NOT NULL CHECK (amount_minor > 0),
    reason         TEXT NOT NULL,
    authorized_by  TEXT REFERENCES app_user(id) ON DELETE SET NULL,
    refunded_at    TEXT NOT NULL
);
CREATE UNIQUE INDEX ux_refund_code ON refund(code);
CREATE INDEX ix_refund_invoice ON refund(invoice_id);
CREATE INDEX ix_refund_session ON refund(cash_session_id);

-- ─────────────────────────────────────────────────────────────
-- Numbering sequences used by billing (§27, §59A.3)
-- ─────────────────────────────────────────────────────────────
INSERT INTO numbering_sequence (code, prefix, padding, yearly_reset, period_year, next_value, updated_at)
VALUES
    ('FACT', 'FACT', 6, 1, 0, 1, '1970-01-01T00:00:00Z'),
    ('REC',  'REC',  6, 1, 0, 1, '1970-01-01T00:00:00Z'),
    ('DEV',  'DEV',  6, 1, 0, 1, '1970-01-01T00:00:00Z'),
    ('CAJA', 'CAJA', 6, 1, 0, 1, '1970-01-01T00:00:00Z');
