-- SPDX-License-Identifier: AGPL-3.0-or-later
-- SIRMAX documents, PDF and printing (Phase 8, master prompt §46, §47, §59B–§59F).
--
-- The rule that shapes this file is §59F: a future logo or address change must
-- not silently rewrite historical invoices. So an issued document stores a
-- *snapshot* of everything needed to reproduce it — institution branding,
-- customer identity, lines, totals — and reprinting renders that snapshot,
-- never today's data.

-- ─────────────────────────────────────────────────────────────
-- Printer profiles (§59D)
-- ─────────────────────────────────────────────────────────────
-- A workstation configures its printers once; after that, printing a receipt
-- takes no dialog. "Which physical printer" is a per-machine fact, so the
-- Windows printer name is stored here rather than in a shared setting.
CREATE TABLE printer_profile (
    id              TEXT PRIMARY KEY,
    name            TEXT NOT NULL,               -- operator-facing, e.g. "Caja 1 – recibos"
    printer_name    TEXT,                        -- the Windows print queue; null = system default
    paper_format    TEXT NOT NULL
                    CHECK (paper_format IN ('LETTER','A4','NARROW_58','NARROW_80')),
    workstation     TEXT,                        -- hostname; null = any workstation
    is_default      INTEGER NOT NULL DEFAULT 0 CHECK (is_default IN (0,1)),
    copies          INTEGER NOT NULL DEFAULT 1 CHECK (copies BETWEEN 1 AND 5),
    -- skip the OS print dialog for this profile (§59D: "minimize unnecessary
    -- dialogs once the workstation has a configured printer profile")
    silent          INTEGER NOT NULL DEFAULT 0 CHECK (silent IN (0,1)),
    created_at      TEXT NOT NULL,
    updated_at      TEXT NOT NULL
);
CREATE INDEX ix_printer_profile_format ON printer_profile(paper_format, is_default);

-- ─────────────────────────────────────────────────────────────
-- Document templates (§46)
-- ─────────────────────────────────────────────────────────────
-- Never hard-code one municipality's name or logo: a template is data. The
-- body is a token-substituted text layout the renderer lays out; the built-in
-- invoice and receipt layouts are code because their arithmetic must be exact.
CREATE TABLE document_template (
    id              TEXT PRIMARY KEY,
    code            TEXT NOT NULL,
    name            TEXT NOT NULL,
    kind            TEXT NOT NULL
                    CHECK (kind IN ('INVOICE','RECEIPT','CERTIFICATE','OFFICIAL_LETTER',
                                    'PERMIT','REGISTRY_COPY','OTHER')),
    paper_format    TEXT NOT NULL DEFAULT 'LETTER'
                    CHECK (paper_format IN ('LETTER','A4','NARROW_58','NARROW_80')),
    -- the body, with {{tokens}} the renderer substitutes from the snapshot
    body            TEXT NOT NULL DEFAULT '',
    header_note     TEXT,
    footer_note     TEXT,
    show_qr         INTEGER NOT NULL DEFAULT 1 CHECK (show_qr IN (0,1)),
    archive_status  TEXT NOT NULL DEFAULT 'ACTIVE'
                    CHECK (archive_status IN ('ACTIVE','COMPLETED','CLOSED','ARCHIVED','VOID','CANCELLED')),
    created_at      TEXT NOT NULL,
    updated_at      TEXT NOT NULL
);
CREATE UNIQUE INDEX ux_document_template_code ON document_template(lower(code));

-- ─────────────────────────────────────────────────────────────
-- Issued documents (§47, §59F)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE issued_document (
    id                  TEXT PRIMARY KEY,
    document_number     TEXT NOT NULL,           -- e.g. DOC-2026-000001
    kind                TEXT NOT NULL
                        CHECK (kind IN ('INVOICE','RECEIPT','CERTIFICATE','OFFICIAL_LETTER',
                                        'PERMIT','REGISTRY_COPY','OTHER')),
    template_id         TEXT REFERENCES document_template(id) ON DELETE SET NULL,
    paper_format        TEXT NOT NULL
                        CHECK (paper_format IN ('LETTER','A4','NARROW_58','NARROW_80')),

    -- what this document is about; at most one is set
    invoice_id          TEXT REFERENCES invoice(id) ON DELETE RESTRICT,
    payment_id          TEXT REFERENCES payment(id) ON DELETE RESTRICT,
    procedure_id        TEXT REFERENCES procedure(id) ON DELETE RESTRICT,
    registered_document_id TEXT REFERENCES registered_document(id) ON DELETE RESTRICT,

    -- §47: a public verification code, and the timestamp it attests
    verification_code   TEXT NOT NULL,
    issued_at           TEXT NOT NULL,
    issued_by           TEXT REFERENCES app_user(id) ON DELETE SET NULL,

    -- §59F: everything needed to reproduce this document years later, frozen.
    -- Institution branding included: a later logo change must not rewrite it.
    snapshot_json       TEXT NOT NULL,
    storage_path        TEXT,                    -- the generated PDF, if kept on disk
    sha256              TEXT,

    -- §59D: reprints never renumber; they increment this and are audited
    print_count         INTEGER NOT NULL DEFAULT 0 CHECK (print_count >= 0),
    last_printed_at     TEXT,
    voided              INTEGER NOT NULL DEFAULT 0 CHECK (voided IN (0,1)),
    created_at          TEXT NOT NULL
);
CREATE UNIQUE INDEX ux_issued_document_number ON issued_document(document_number);
CREATE UNIQUE INDEX ux_issued_document_verification ON issued_document(verification_code);
CREATE INDEX ix_issued_document_invoice ON issued_document(invoice_id);
CREATE INDEX ix_issued_document_procedure ON issued_document(procedure_id);

-- Every physical output, including the first. §59D requires reprints to be
-- auditable; keeping the first print in the same table means "how many times
-- has this been printed and by whom" is one query, not a special case.
CREATE TABLE document_print (
    id                  TEXT PRIMARY KEY,
    issued_document_id  TEXT NOT NULL REFERENCES issued_document(id) ON DELETE CASCADE,
    printed_at          TEXT NOT NULL,
    printed_by          TEXT REFERENCES app_user(id) ON DELETE SET NULL,
    printer_profile_id  TEXT REFERENCES printer_profile(id) ON DELETE SET NULL,
    is_reprint          INTEGER NOT NULL DEFAULT 0 CHECK (is_reprint IN (0,1)),
    reason              TEXT
);
CREATE INDEX ix_document_print_document ON document_print(issued_document_id, printed_at);

INSERT INTO numbering_sequence (code, prefix, padding, yearly_reset, period_year, next_value, updated_at)
VALUES ('DOC', 'DOC', 6, 1, 0, 1, '1970-01-01T00:00:00Z');
