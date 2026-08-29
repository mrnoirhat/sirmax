-- SPDX-License-Identifier: AGPL-3.0-or-later
-- SIRMAX generic procedure model (Phase 5, master prompt §16, §27, §30, §56, §57).
--
-- One shared structure carries certificates, permits, registrations, complaints
-- and operational cases: a `procedure` row bound to the service *version* it was
-- opened with (so published configuration stays interpretable forever), plus its
-- materialized requirement checklist, dynamic form answers, attachments and an
-- append-only event timeline.
--
-- Numbering (§27) lives in `numbering_sequence`: one row per document/service
-- family, allocated under the same transaction as the row it numbers, never
-- reused after a void.

-- ─────────────────────────────────────────────────────────────
-- Document numbering (§27)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE numbering_sequence (
    code          TEXT PRIMARY KEY,                 -- e.g. 'TRM', 'FACT', 'CERT-RES'
    prefix        TEXT NOT NULL,
    padding       INTEGER NOT NULL DEFAULT 6 CHECK (padding BETWEEN 1 AND 12),
    yearly_reset  INTEGER NOT NULL DEFAULT 1 CHECK (yearly_reset IN (0,1)),
    period_year   INTEGER NOT NULL,                 -- the year `next_value` belongs to
    next_value    INTEGER NOT NULL DEFAULT 1 CHECK (next_value >= 1),
    updated_at    TEXT NOT NULL
);

INSERT INTO numbering_sequence (code, prefix, padding, yearly_reset, period_year, next_value, updated_at)
VALUES ('TRM', 'TRM', 6, 1, 0, 1, '1970-01-01T00:00:00Z');

-- ─────────────────────────────────────────────────────────────
-- Procedure (trámite) — the generic municipal case
-- ─────────────────────────────────────────────────────────────
CREATE TABLE procedure (
    id                      TEXT PRIMARY KEY,
    code                    TEXT NOT NULL,          -- public number, e.g. TRM-2026-000001
    service_definition_id   TEXT NOT NULL REFERENCES service_definition(id) ON DELETE RESTRICT,
    service_version_id      TEXT NOT NULL REFERENCES service_definition_version(id) ON DELETE RESTRICT,

    -- applicant: polymorphic party (PERSON | ORGANIZATION), see domain.common.PartyRef
    applicant_type          TEXT NOT NULL CHECK (applicant_type IN ('PERSON','ORGANIZATION')),
    applicant_id            TEXT NOT NULL,

    -- optional subject of the case: a property, a plot, a registered document…
    subject_type            TEXT,
    subject_id              TEXT,

    status                  TEXT NOT NULL DEFAULT 'OPEN'
                            CHECK (status IN ('DRAFT','OPEN','IN_PROGRESS','WAITING_REQUIREMENTS',
                                              'WAITING_PAYMENT','APPROVED','REJECTED','DELIVERED',
                                              'CLOSED','CANCELLED')),
    priority                TEXT NOT NULL DEFAULT 'NORMAL'
                            CHECK (priority IN ('LOW','NORMAL','HIGH','URGENT')),
    current_step_key        TEXT,                   -- null once the case is terminal
    department_id           TEXT REFERENCES department(id) ON DELETE SET NULL,
    assigned_user_id        TEXT REFERENCES app_user(id) ON DELETE SET NULL,

    opened_at               TEXT NOT NULL,
    due_date                TEXT,                   -- ISO date, derived from the version SLA
    closed_at               TEXT,
    outcome                 TEXT CHECK (outcome IS NULL OR outcome IN ('APPROVED','REJECTED','CANCELLED','DELIVERED')),
    outcome_reason          TEXT,

    notes                   TEXT,
    created_at              TEXT NOT NULL,
    updated_at              TEXT NOT NULL
);
CREATE UNIQUE INDEX ux_procedure_code ON procedure(code);
CREATE INDEX ix_procedure_applicant ON procedure(applicant_type, applicant_id);
CREATE INDEX ix_procedure_status ON procedure(status);
CREATE INDEX ix_procedure_assigned ON procedure(assigned_user_id);
CREATE INDEX ix_procedure_department ON procedure(department_id);
CREATE INDEX ix_procedure_service ON procedure(service_definition_id);
CREATE INDEX ix_procedure_due ON procedure(due_date);

-- Materialized checklist: one row per requirement declared by the service version
-- at the moment the procedure was opened (§56 — the operator sees what is missing).
CREATE TABLE procedure_requirement (
    id             TEXT PRIMARY KEY,
    procedure_id   TEXT NOT NULL REFERENCES procedure(id) ON DELETE CASCADE,
    requirement_key TEXT NOT NULL,
    label          TEXT NOT NULL,
    kind           TEXT NOT NULL,
    stage          TEXT NOT NULL,
    required       INTEGER NOT NULL DEFAULT 1 CHECK (required IN (0,1)),
    -- copied from the service version so applicability can be re-evaluated as the
    -- operator fills the form, without re-reading a version that may have moved on
    condition_expression TEXT,
    satisfied      INTEGER NOT NULL DEFAULT 0 CHECK (satisfied IN (0,1)),
    waived         INTEGER NOT NULL DEFAULT 0 CHECK (waived IN (0,1)),
    note           TEXT,
    satisfied_at   TEXT,
    satisfied_by   TEXT REFERENCES app_user(id) ON DELETE SET NULL,
    UNIQUE (procedure_id, requirement_key)
);
CREATE INDEX ix_procedure_requirement_case ON procedure_requirement(procedure_id);

-- Answers to the service version's dynamic form (FormSchema).
CREATE TABLE procedure_form_value (
    procedure_id  TEXT NOT NULL REFERENCES procedure(id) ON DELETE CASCADE,
    field_key     TEXT NOT NULL,
    value_text    TEXT,
    PRIMARY KEY (procedure_id, field_key)
);

-- Append-only timeline: status/step changes, assignments, notes, decisions (§40).
CREATE TABLE procedure_event (
    id             TEXT PRIMARY KEY,
    procedure_id   TEXT NOT NULL REFERENCES procedure(id) ON DELETE CASCADE,
    occurred_at    TEXT NOT NULL,
    actor_user_id  TEXT REFERENCES app_user(id) ON DELETE SET NULL,
    kind           TEXT NOT NULL
                   CHECK (kind IN ('OPENED','REQUIREMENT_UPDATED','FORM_UPDATED','ASSIGNED',
                                   'STEP_ADVANCED','NOTE','INVOICED','PAID','DOCUMENT_ISSUED',
                                   'DECIDED','CLOSED','REOPENED','CANCELLED')),
    from_step_key  TEXT,
    to_step_key    TEXT,
    detail         TEXT
);
CREATE INDEX ix_procedure_event_case ON procedure_event(procedure_id, occurred_at);

-- Files attached to a case. Content lives on disk under the app data directory;
-- the hash lets the integrity check in Phase 10 detect tampering.
CREATE TABLE procedure_attachment (
    id              TEXT PRIMARY KEY,
    procedure_id    TEXT NOT NULL REFERENCES procedure(id) ON DELETE CASCADE,
    requirement_key TEXT,
    file_name       TEXT NOT NULL,
    content_type    TEXT,
    size_bytes      INTEGER NOT NULL DEFAULT 0 CHECK (size_bytes >= 0),
    storage_path    TEXT NOT NULL,
    sha256          TEXT,
    uploaded_at     TEXT NOT NULL,
    uploaded_by     TEXT REFERENCES app_user(id) ON DELETE SET NULL
);
CREATE INDEX ix_procedure_attachment_case ON procedure_attachment(procedure_id);

-- ─────────────────────────────────────────────────────────────
-- Citizen search normalization (§23, §32)
-- ─────────────────────────────────────────────────────────────
-- SQLite's LIKE is case-insensitive for ASCII only and knows nothing about
-- diacritics, so "Pena" would never find "Peña". Persist a folded key beside
-- the display name and search on that; shared.text.Normalization produces it.
ALTER TABLE person ADD COLUMN search_name TEXT NOT NULL DEFAULT '';
CREATE INDEX ix_person_search_name ON person(search_name);

-- Backfill what exists. Only the accents actually used in Dominican names are
-- folded here; every future write goes through Normalization.fold in Java.
UPDATE person SET search_name = lower(
    replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(
    replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(
        full_name,
        'á','a'),'é','e'),'í','i'),'ó','o'),'ú','u'),'ü','u'),'ñ','n'),
        'Á','a'),'É','e'),'Í','i'),'Ó','o'),'Ú','u'),'Ü','u'),'Ñ','n'),
        'à','a'),'è','e'),'ì','i'),'ò','o'),'ù','u'),'ç','c')
);
