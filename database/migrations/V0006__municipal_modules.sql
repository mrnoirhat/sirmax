-- SPDX-License-Identifier: AGPL-3.0-or-later
-- SIRMAX municipal specialty modules (Phase 7, master prompt §25, §26, §28, §29,
-- and the Conservaduría / cemetery / market sections §4, §6, §7).
--
-- The instruction that shapes this file is §7-Phase: "do not hard-code every
-- service as its own unrelated architecture". So three shared models carry all
-- ten priority modules:
--
--   municipal_asset  — anything the municipality registers a *place* for: a
--                      parcel, a cemetery niche, a market stall, a kiosk. Kind
--                      plus a self-reference for containment (cementerio →
--                      sección → nicho), so a new asset kind is configuration,
--                      not a new table.
--   agreement        — any contract over an asset held by a party: lease,
--                      cemetery concession, stall assignment, public-space
--                      permit. One transfer/termination story for all of them.
--   inspection       — the reusable site visit any service can require.
--
-- Alongside them, two things that genuinely are their own thing:
--   registered_document — the Conservaduría register. §4 is explicit that an
--                         officially registered document is NOT a file attached
--                         to a case, and it has a book/folio identity of its own.
--   decision            — the recorded approval act (§28), separate from the
--                         procedure's coarse outcome because a case can collect
--                         several decisions from several roles.

-- ─────────────────────────────────────────────────────────────
-- Municipal assets (§25 property/cadastre, §6 cemeteries, §7 markets)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE municipal_asset (
    id                  TEXT PRIMARY KEY,
    code                TEXT NOT NULL,          -- parcel id, niche number, stall number
    kind                TEXT NOT NULL
                        CHECK (kind IN ('PARCEL','BUILDING','CEMETERY','CEMETERY_SECTION',
                                        'CEMETERY_PLOT','MARKET','MARKET_STALL','KIOSK',
                                        'PUBLIC_SPACE','ROAD','OTHER')),
    name                TEXT NOT NULL,
    -- containment: a niche belongs to a section, a section to a cemetery, a
    -- stall to a market. One self-reference replaces four parallel hierarchies.
    parent_id           TEXT REFERENCES municipal_asset(id) ON DELETE RESTRICT,

    -- §24 location model; kept as plain columns because every module reads them
    address_line        TEXT,
    sector              TEXT,
    municipality        TEXT,
    province            TEXT,
    latitude            REAL,
    longitude           REAL,

    area_sq_m           INTEGER,                -- whole square metres; fees bill on this
    municipally_owned   INTEGER NOT NULL DEFAULT 0 CHECK (municipally_owned IN (0,1)),
    -- free for the module that owns the asset kind: cadastral references, niche
    -- capacity, stall trade. Validated JSON, same contract as ADR 0006.
    attributes_json     TEXT NOT NULL DEFAULT '{}',

    availability        TEXT NOT NULL DEFAULT 'AVAILABLE'
                        CHECK (availability IN ('AVAILABLE','OCCUPIED','RESERVED','UNAVAILABLE')),
    archive_status      TEXT NOT NULL DEFAULT 'ACTIVE'
                        CHECK (archive_status IN ('ACTIVE','COMPLETED','CLOSED','ARCHIVED','VOID','CANCELLED')),
    notes               TEXT,
    created_at          TEXT NOT NULL,
    updated_at          TEXT NOT NULL
);
CREATE UNIQUE INDEX ux_municipal_asset_code ON municipal_asset(kind, lower(code));
CREATE INDEX ix_municipal_asset_parent ON municipal_asset(parent_id);
CREATE INDEX ix_municipal_asset_kind ON municipal_asset(kind, availability);

-- Who holds an asset, and how. An owner is a fact about the cadastre; a
-- concession-holder is a fact about a contract — both are recorded here so a
-- parcel's history survives changes of either.
CREATE TABLE asset_holder (
    id            TEXT PRIMARY KEY,
    asset_id      TEXT NOT NULL REFERENCES municipal_asset(id) ON DELETE CASCADE,
    party_type    TEXT NOT NULL CHECK (party_type IN ('PERSON','ORGANIZATION')),
    party_id      TEXT NOT NULL,
    role          TEXT NOT NULL
                  CHECK (role IN ('OWNER','CO_OWNER','LESSEE','CONCESSIONAIRE','OCCUPANT',
                                  'REPRESENTATIVE','HEIR')),
    share_percent INTEGER CHECK (share_percent IS NULL OR (share_percent BETWEEN 1 AND 100)),
    from_date     TEXT NOT NULL,
    to_date       TEXT,                         -- null = current
    legal_reference TEXT,
    created_at    TEXT NOT NULL
);
CREATE INDEX ix_asset_holder_asset ON asset_holder(asset_id, to_date);
CREATE INDEX ix_asset_holder_party ON asset_holder(party_type, party_id);

-- ─────────────────────────────────────────────────────────────
-- Agreements: leases, concessions, stall assignments, permits (§26)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE agreement (
    id                 TEXT PRIMARY KEY,
    code               TEXT NOT NULL,           -- e.g. CONT-2026-000001
    kind               TEXT NOT NULL
                       CHECK (kind IN ('LEASE','CONCESSION','STALL_ASSIGNMENT',
                                       'PUBLIC_SPACE_PERMIT','OTHER')),
    asset_id           TEXT REFERENCES municipal_asset(id) ON DELETE RESTRICT,
    procedure_id       TEXT REFERENCES procedure(id) ON DELETE SET NULL,

    holder_type        TEXT NOT NULL CHECK (holder_type IN ('PERSON','ORGANIZATION')),
    holder_id          TEXT NOT NULL,

    status             TEXT NOT NULL DEFAULT 'DRAFT'
                       CHECK (status IN ('DRAFT','ACTIVE','SUSPENDED','TRANSFERRED',
                                         'EXPIRED','TERMINATED','CANCELLED')),
    start_date         TEXT NOT NULL,
    end_date           TEXT,                    -- null = indefinite (perpetual concession)
    renewable          INTEGER NOT NULL DEFAULT 1 CHECK (renewable IN (0,1)),

    -- recurring amount; the one-off fees a contract triggers are ordinary invoices
    currency           TEXT NOT NULL DEFAULT 'DOP',
    amount_minor       INTEGER NOT NULL DEFAULT 0,
    billing_frequency  TEXT NOT NULL DEFAULT 'MONTHLY'
                       CHECK (billing_frequency IN ('ONCE','MONTHLY','QUARTERLY','ANNUAL','NONE')),

    -- a transfer preserves the chain: the new contract points back at the old
    transferred_from_id TEXT REFERENCES agreement(id) ON DELETE SET NULL,
    terminated_at      TEXT,
    termination_reason TEXT,

    notes              TEXT,
    created_at         TEXT NOT NULL,
    updated_at         TEXT NOT NULL
);
CREATE UNIQUE INDEX ux_agreement_code ON agreement(code);
CREATE INDEX ix_agreement_asset ON agreement(asset_id, status);
CREATE INDEX ix_agreement_holder ON agreement(holder_type, holder_id);
CREATE INDEX ix_agreement_status ON agreement(status, end_date);

-- ─────────────────────────────────────────────────────────────
-- Registered documents — Conservaduría (§4)
-- ─────────────────────────────────────────────────────────────
-- Explicitly NOT procedure_attachment: that is a scan someone brought in, this
-- is an act of the municipal register with a book/folio identity and its own
-- certified-copy trail.
CREATE TABLE registered_document (
    id                 TEXT PRIMARY KEY,
    registration_number TEXT NOT NULL,          -- e.g. REG-2026-000001
    document_type      TEXT NOT NULL,           -- administrator-configured (venta, poder, acta…)
    title              TEXT NOT NULL,
    procedure_id       TEXT REFERENCES procedure(id) ON DELETE SET NULL,

    document_date      TEXT,                    -- when the document itself was executed
    presented_at       TEXT NOT NULL,           -- when it reached the counter
    registered_at      TEXT,                    -- when it was entered into the register

    book               TEXT,
    volume             TEXT,
    folio              TEXT,
    status             TEXT NOT NULL DEFAULT 'PRESENTED'
                       CHECK (status IN ('PRESENTED','UNDER_REVIEW','REGISTERED','REJECTED','ANNULLED')),

    related_asset_id   TEXT REFERENCES municipal_asset(id) ON DELETE SET NULL,
    storage_path       TEXT,                    -- the scan, under the app data directory
    sha256             TEXT,
    notes              TEXT,
    created_at         TEXT NOT NULL,
    updated_at         TEXT NOT NULL
);
CREATE UNIQUE INDEX ux_registered_document_number ON registered_document(registration_number);
CREATE INDEX ix_registered_document_status ON registered_document(status);
CREATE INDEX ix_registered_document_asset ON registered_document(related_asset_id);

-- The parties named in a registered document (grantor, grantee, witness…).
CREATE TABLE registered_document_party (
    id           TEXT PRIMARY KEY,
    document_id  TEXT NOT NULL REFERENCES registered_document(id) ON DELETE CASCADE,
    party_type   TEXT NOT NULL CHECK (party_type IN ('PERSON','ORGANIZATION')),
    party_id     TEXT NOT NULL,
    role         TEXT NOT NULL,                 -- administrator-configured vocabulary
    UNIQUE (document_id, party_type, party_id, role)
);
CREATE INDEX ix_rdp_document ON registered_document_party(document_id);
CREATE INDEX ix_rdp_party ON registered_document_party(party_type, party_id);

-- Marginal annotations: the register is append-only, so a correction is a note
-- against the entry, never an edit of it.
CREATE TABLE registered_document_annotation (
    id           TEXT PRIMARY KEY,
    document_id  TEXT NOT NULL REFERENCES registered_document(id) ON DELETE CASCADE,
    text         TEXT NOT NULL,
    annotated_by TEXT REFERENCES app_user(id) ON DELETE SET NULL,
    annotated_at TEXT NOT NULL
);
CREATE INDEX ix_rda_document ON registered_document_annotation(document_id, annotated_at);

-- ─────────────────────────────────────────────────────────────
-- Inspections (§29) — reusable by any service that configures one
-- ─────────────────────────────────────────────────────────────
CREATE TABLE inspection (
    id             TEXT PRIMARY KEY,
    code           TEXT NOT NULL,               -- e.g. INSP-2026-000001
    procedure_id   TEXT NOT NULL REFERENCES procedure(id) ON DELETE CASCADE,
    asset_id       TEXT REFERENCES municipal_asset(id) ON DELETE SET NULL,
    inspector_user_id TEXT REFERENCES app_user(id) ON DELETE SET NULL,

    status         TEXT NOT NULL DEFAULT 'SCHEDULED'
                   CHECK (status IN ('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED')),
    result         TEXT CHECK (result IS NULL OR result IN ('PASSED','FAILED','PASSED_WITH_CONDITIONS','NOT_APPLICABLE')),

    scheduled_date TEXT,
    performed_at   TEXT,
    location       TEXT,
    findings       TEXT,
    -- checklist answers, shaped by the service's configuration
    checklist_json TEXT NOT NULL DEFAULT '[]',
    follow_up_date TEXT,
    created_at     TEXT NOT NULL,
    updated_at     TEXT NOT NULL
);
CREATE UNIQUE INDEX ux_inspection_code ON inspection(code);
CREATE INDEX ix_inspection_procedure ON inspection(procedure_id);
CREATE INDEX ix_inspection_inspector ON inspection(inspector_user_id, status);

-- ─────────────────────────────────────────────────────────────
-- Decisions (§28)
-- ─────────────────────────────────────────────────────────────
-- Separate from procedure.outcome: a case can collect several decisions from
-- several roles (technical review, then legal, then the director), and the
-- audit needs each one with its own author and reason.
CREATE TABLE decision (
    id                TEXT PRIMARY KEY,
    procedure_id      TEXT NOT NULL REFERENCES procedure(id) ON DELETE CASCADE,
    step_key          TEXT,                     -- the workflow step it was taken at
    outcome           TEXT NOT NULL
                      CHECK (outcome IN ('APPROVED','REJECTED','RETURNED_FOR_CORRECTION',
                                         'CONDITIONALLY_APPROVED','EXPIRED','CANCELLED')),
    decided_by        TEXT REFERENCES app_user(id) ON DELETE SET NULL,
    decided_by_role   TEXT,
    decided_at        TEXT NOT NULL,
    reason            TEXT,
    comments          TEXT,
    conditions        TEXT,                     -- what a CONDITIONALLY_APPROVED case must still do
    document_id       TEXT REFERENCES registered_document(id) ON DELETE SET NULL
);
CREATE INDEX ix_decision_procedure ON decision(procedure_id, decided_at);

-- ─────────────────────────────────────────────────────────────
-- Numbering for the module documents (§27)
-- ─────────────────────────────────────────────────────────────
INSERT INTO numbering_sequence (code, prefix, padding, yearly_reset, period_year, next_value, updated_at)
VALUES
    ('CONT', 'CONT', 6, 1, 0, 1, '1970-01-01T00:00:00Z'),
    ('REG',  'REG',  6, 1, 0, 1, '1970-01-01T00:00:00Z'),
    ('INSP', 'INSP', 6, 1, 0, 1, '1970-01-01T00:00:00Z');
