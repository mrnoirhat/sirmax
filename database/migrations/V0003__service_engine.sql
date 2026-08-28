-- SPDX-License-Identifier: AGPL-3.0-or-later
-- SIRMAX configurable service engine (Phase 4): the service catalog, versioned
-- service definitions, and the per-version configuration (requirements, form
-- schema, workflow, fee rules, output documents, authorization, SLA, validity).
--
-- Design: typed columns for the stable parts (code, category, status,
-- department); validated JSON for the flexible parts (docs/adr/0006). A
-- procedure records the version it was opened with, so published versions are
-- immutable (enforced in the domain).

-- ─────────────────────────────────────────────────────────────
-- Service catalog
-- ─────────────────────────────────────────────────────────────
CREATE TABLE service_category (
    id             TEXT PRIMARY KEY,
    code           TEXT NOT NULL,
    name           TEXT NOT NULL,
    sort_order     INTEGER NOT NULL DEFAULT 0,
    archive_status TEXT NOT NULL DEFAULT 'ACTIVE'
                   CHECK (archive_status IN ('ACTIVE','COMPLETED','CLOSED','ARCHIVED','VOID','CANCELLED')),
    created_at     TEXT NOT NULL
);
CREATE UNIQUE INDEX ux_service_category_code ON service_category(lower(code));

CREATE TABLE service_definition (
    id                          TEXT PRIMARY KEY,
    code                        TEXT NOT NULL,
    category_id                 TEXT NOT NULL REFERENCES service_category(id) ON DELETE RESTRICT,
    name                        TEXT NOT NULL,
    description                 TEXT,
    service_type                TEXT NOT NULL DEFAULT 'CON_TASA'
                                CHECK (service_type IN ('GRATUITO','CON_TASA','TASA_CONDICIONAL','PAGO_EXTERNO')),
    department_id               TEXT REFERENCES department(id) ON DELETE SET NULL,
    country_scope               TEXT NOT NULL DEFAULT 'DO',
    municipal_override_allowed  INTEGER NOT NULL DEFAULT 1 CHECK (municipal_override_allowed IN (0,1)),
    current_version_id          TEXT,              -- FK added as a trigger-free soft reference
    archive_status              TEXT NOT NULL DEFAULT 'ACTIVE'
                                CHECK (archive_status IN ('ACTIVE','COMPLETED','CLOSED','ARCHIVED','VOID','CANCELLED')),
    created_at                  TEXT NOT NULL,
    updated_at                  TEXT NOT NULL
);
CREATE UNIQUE INDEX ux_service_definition_code ON service_definition(lower(code));
CREATE INDEX ix_service_definition_category ON service_definition(category_id);

CREATE TABLE service_definition_version (
    id                       TEXT PRIMARY KEY,
    service_definition_id    TEXT NOT NULL REFERENCES service_definition(id) ON DELETE CASCADE,
    version_number           INTEGER NOT NULL,
    status                   TEXT NOT NULL DEFAULT 'DRAFT'
                             CHECK (status IN ('DRAFT','ACTIVE','INACTIVE','ARCHIVED')),
    requires_payment         INTEGER NOT NULL DEFAULT 0 CHECK (requires_payment IN (0,1)),
    numbering_sequence_code  TEXT,
    notes                    TEXT,

    -- flexible configuration (validated JSON; see docs/adr/0006-0008)
    requirements_json        TEXT NOT NULL DEFAULT '[]',
    form_schema_json         TEXT NOT NULL DEFAULT '{}',
    workflow_json            TEXT NOT NULL DEFAULT '{}',
    fee_rules_json           TEXT NOT NULL DEFAULT '[]',
    output_documents_json    TEXT NOT NULL DEFAULT '[]',
    authorization_json       TEXT NOT NULL DEFAULT '{}',
    sla_json                 TEXT NOT NULL DEFAULT '{}',
    validity_json            TEXT NOT NULL DEFAULT '{}',

    created_at               TEXT NOT NULL,
    published_at             TEXT,

    UNIQUE (service_definition_id, version_number)
);
CREATE INDEX ix_sdv_definition ON service_definition_version(service_definition_id);
CREATE INDEX ix_sdv_status ON service_definition_version(status);
