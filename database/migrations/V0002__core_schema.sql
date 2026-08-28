-- SPDX-License-Identifier: AGPL-3.0-or-later
-- SIRMAX core schema: organization, departments, users/roles/permissions,
-- people, organizations, identifications, addresses and contacts.
-- See docs/domain/glossary.md and docs/domain/erd.md.
--
-- Conventions (DATABASE.md): TEXT UUIDv7 primary keys; ISO-8601 UTC
-- timestamps; enums as TEXT with CHECK; archive status instead of DELETE.
-- Applied by MigrationRunner inside a single transaction with
-- PRAGMA foreign_keys = ON.

-- ─────────────────────────────────────────────────────────────
-- Organization / institution
-- ─────────────────────────────────────────────────────────────
CREATE TABLE organization_unit (
    id             TEXT PRIMARY KEY,
    name           TEXT NOT NULL,
    short_name     TEXT,
    municipality   TEXT NOT NULL,
    province       TEXT,
    country        TEXT NOT NULL DEFAULT 'DO',          -- ISO 3166-1 alpha-2
    archive_status TEXT NOT NULL DEFAULT 'ACTIVE'
                   CHECK (archive_status IN ('ACTIVE','COMPLETED','CLOSED','ARCHIVED','VOID','CANCELLED')),
    created_at     TEXT NOT NULL,
    updated_at     TEXT NOT NULL
);

-- 1:1 branding / contact profile for documents and invoices (master prompt §59C).
CREATE TABLE institution_profile (
    organization_unit_id TEXT PRIMARY KEY REFERENCES organization_unit(id) ON DELETE CASCADE,
    legal_identifier     TEXT,                          -- RNC or country equivalent
    address              TEXT,
    phone                TEXT,
    email                TEXT,
    website              TEXT,
    logo_path            TEXT,
    secondary_logo_path  TEXT,
    color_primary        TEXT,
    color_secondary      TEXT,
    color_accent         TEXT,
    color_text           TEXT,
    color_background     TEXT,
    invoice_footer       TEXT,
    document_header      TEXT,
    verification_json    TEXT NOT NULL DEFAULT '{}',
    updated_at           TEXT NOT NULL
);

CREATE TABLE department (
    id                   TEXT PRIMARY KEY,
    organization_unit_id TEXT NOT NULL REFERENCES organization_unit(id) ON DELETE CASCADE,
    name                 TEXT NOT NULL,
    code                 TEXT NOT NULL,
    archive_status       TEXT NOT NULL DEFAULT 'ACTIVE'
                         CHECK (archive_status IN ('ACTIVE','COMPLETED','CLOSED','ARCHIVED','VOID','CANCELLED')),
    created_at           TEXT NOT NULL,
    UNIQUE (organization_unit_id, code)
);
CREATE INDEX ix_department_org ON department(organization_unit_id);

-- ─────────────────────────────────────────────────────────────
-- Users, roles, permissions (RBAC)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE app_user (
    id             TEXT PRIMARY KEY,
    username       TEXT NOT NULL,
    display_name   TEXT NOT NULL,
    password_hash  TEXT NOT NULL,
    password_algo  TEXT NOT NULL,                       -- e.g. 'PBKDF2-HMAC-SHA256'
    status         TEXT NOT NULL DEFAULT 'ACTIVE'
                   CHECK (status IN ('ACTIVE','DISABLED','LOCKED')),
    department_id  TEXT REFERENCES department(id) ON DELETE SET NULL,
    created_at     TEXT NOT NULL,
    updated_at     TEXT NOT NULL,
    last_login_at  TEXT
);
CREATE UNIQUE INDEX ux_app_user_username ON app_user(lower(username));
CREATE INDEX ix_app_user_department ON app_user(department_id);

CREATE TABLE role (
    id          TEXT PRIMARY KEY,
    name        TEXT NOT NULL,
    description TEXT,
    is_system   INTEGER NOT NULL DEFAULT 0 CHECK (is_system IN (0,1)),
    created_at  TEXT NOT NULL
);
CREATE UNIQUE INDEX ux_role_name ON role(lower(name));

CREATE TABLE permission (
    key         TEXT PRIMARY KEY,
    description TEXT NOT NULL
);

CREATE TABLE user_role (
    user_id TEXT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role_id TEXT NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE role_permission (
    role_id        TEXT NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    permission_key TEXT NOT NULL REFERENCES permission(key) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_key)
);

-- ─────────────────────────────────────────────────────────────
-- People and organizations (parties)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE person (
    id             TEXT PRIMARY KEY,
    given_names    TEXT NOT NULL,
    family_names   TEXT NOT NULL,
    full_name      TEXT NOT NULL,                       -- denormalized for search
    birth_date     TEXT,                                -- ISO-8601 date
    sex            TEXT CHECK (sex IN ('F','M','X') OR sex IS NULL),
    notes          TEXT,
    archive_status TEXT NOT NULL DEFAULT 'ACTIVE'
                   CHECK (archive_status IN ('ACTIVE','COMPLETED','CLOSED','ARCHIVED','VOID','CANCELLED')),
    created_at     TEXT NOT NULL,
    updated_at     TEXT NOT NULL
);
CREATE INDEX ix_person_full_name ON person(full_name);
CREATE INDEX ix_person_family ON person(family_names);

CREATE TABLE organization_party (
    id             TEXT PRIMARY KEY,
    legal_name     TEXT NOT NULL,
    trade_name     TEXT,
    kind           TEXT NOT NULL DEFAULT 'BUSINESS'
                   CHECK (kind IN ('BUSINESS','COMMUNITY','INSTITUTION','OTHER')),
    notes          TEXT,
    archive_status TEXT NOT NULL DEFAULT 'ACTIVE'
                   CHECK (archive_status IN ('ACTIVE','COMPLETED','CLOSED','ARCHIVED','VOID','CANCELLED')),
    created_at     TEXT NOT NULL,
    updated_at     TEXT NOT NULL
);
CREATE INDEX ix_organization_party_legal_name ON organization_party(legal_name);

-- Polymorphic owner: party_type + party_id points at person OR organization_party.
-- Cross-table FK is not expressible in SQLite; integrity is enforced in the
-- application layer and the party_type is CHECK-constrained here.

CREATE TABLE identification (
    id         TEXT PRIMARY KEY,
    party_type TEXT NOT NULL CHECK (party_type IN ('PERSON','ORGANIZATION')),
    party_id   TEXT NOT NULL,
    id_type    TEXT NOT NULL
               CHECK (id_type IN ('CEDULA','RNC','PASSPORT','RESIDENT_ID','OTHER')),
    id_number  TEXT NOT NULL,
    is_primary INTEGER NOT NULL DEFAULT 0 CHECK (is_primary IN (0,1)),
    created_at TEXT NOT NULL
);
CREATE INDEX ix_identification_owner ON identification(party_type, party_id);
CREATE INDEX ix_identification_lookup ON identification(id_type, id_number);

CREATE TABLE postal_address (
    id              TEXT PRIMARY KEY,
    party_type      TEXT NOT NULL CHECK (party_type IN ('PERSON','ORGANIZATION')),
    party_id        TEXT NOT NULL,
    municipality    TEXT,
    district_sector TEXT,
    neighborhood    TEXT,
    street          TEXT,
    street_number   TEXT,
    reference       TEXT,
    postal_code     TEXT,
    latitude        REAL,
    longitude       REAL,
    is_primary      INTEGER NOT NULL DEFAULT 0 CHECK (is_primary IN (0,1)),
    created_at      TEXT NOT NULL
);
CREATE INDEX ix_postal_address_owner ON postal_address(party_type, party_id);

CREATE TABLE contact_point (
    id         TEXT PRIMARY KEY,
    party_type TEXT NOT NULL CHECK (party_type IN ('PERSON','ORGANIZATION')),
    party_id   TEXT NOT NULL,
    kind       TEXT NOT NULL CHECK (kind IN ('PHONE','MOBILE','EMAIL','WHATSAPP','OTHER')),
    value      TEXT NOT NULL,
    is_primary INTEGER NOT NULL DEFAULT 0 CHECK (is_primary IN (0,1)),
    created_at TEXT NOT NULL
);
CREATE INDEX ix_contact_point_owner ON contact_point(party_type, party_id);

-- ─────────────────────────────────────────────────────────────
-- Seed: permission catalog + system roles (structure, not sample data)
-- ─────────────────────────────────────────────────────────────
INSERT INTO permission (key, description) VALUES
    ('person.read',        'Ver ciudadanos y organizaciones'),
    ('person.write',       'Crear y editar ciudadanos y organizaciones'),
    ('department.manage',  'Gestionar departamentos'),
    ('user.manage',        'Gestionar usuarios'),
    ('role.manage',        'Gestionar roles y permisos'),
    ('service.read',       'Ver el catálogo de servicios'),
    ('service.configure',  'Configurar servicios, requisitos, flujos y tarifas'),
    ('procedure.read',     'Ver trámites'),
    ('procedure.work',     'Trabajar trámites (requisitos, tareas, revisiones)'),
    ('procedure.decide',   'Aprobar, rechazar o devolver trámites'),
    ('fee.override',       'Ajustar manualmente una tasa con autorización'),
    ('invoice.issue',      'Emitir facturas'),
    ('invoice.void',       'Anular facturas'),
    ('invoice.reprint',    'Reimprimir facturas y recibos'),
    ('payment.register',   'Registrar pagos'),
    ('payment.refund',     'Registrar reembolsos'),
    ('cash.session.open',  'Abrir caja'),
    ('cash.session.close', 'Cerrar caja y conciliar'),
    ('document.register',  'Registrar documentos oficiales'),
    ('document.certify',   'Emitir copias certificadas y certificaciones'),
    ('config.manage',      'Configuración de la institución y del sistema'),
    ('audit.read',         'Consultar el registro de auditoría'),
    ('backup.run',         'Crear copias de seguridad'),
    ('backup.restore',     'Restaurar copias de seguridad'),
    ('report.view',        'Ver reportes');

INSERT INTO role (id, name, description, is_system, created_at) VALUES
    ('00000000-0000-7000-8000-000000000001','ADMINISTRADOR','Acceso completo a la configuración y a la operación', 1, '2026-01-01T00:00:00Z'),
    ('00000000-0000-7000-8000-000000000002','SUPERVISOR','Supervisión de trámites, decisiones y reportes',          1, '2026-01-01T00:00:00Z'),
    ('00000000-0000-7000-8000-000000000003','OPERADOR','Atención de ciudadanos y trámites en el mostrador',         1, '2026-01-01T00:00:00Z'),
    ('00000000-0000-7000-8000-000000000004','CAJERA','Cobro, caja y reimpresión de recibos',                        1, '2026-01-01T00:00:00Z');

-- ADMINISTRADOR: every permission.
INSERT INTO role_permission (role_id, permission_key)
    SELECT '00000000-0000-7000-8000-000000000001', key FROM permission;

-- SUPERVISOR
INSERT INTO role_permission (role_id, permission_key) VALUES
    ('00000000-0000-7000-8000-000000000002','person.read'),
    ('00000000-0000-7000-8000-000000000002','service.read'),
    ('00000000-0000-7000-8000-000000000002','procedure.read'),
    ('00000000-0000-7000-8000-000000000002','procedure.work'),
    ('00000000-0000-7000-8000-000000000002','procedure.decide'),
    ('00000000-0000-7000-8000-000000000002','fee.override'),
    ('00000000-0000-7000-8000-000000000002','invoice.void'),
    ('00000000-0000-7000-8000-000000000002','payment.refund'),
    ('00000000-0000-7000-8000-000000000002','audit.read'),
    ('00000000-0000-7000-8000-000000000002','report.view');

-- OPERADOR
INSERT INTO role_permission (role_id, permission_key) VALUES
    ('00000000-0000-7000-8000-000000000003','person.read'),
    ('00000000-0000-7000-8000-000000000003','person.write'),
    ('00000000-0000-7000-8000-000000000003','service.read'),
    ('00000000-0000-7000-8000-000000000003','procedure.read'),
    ('00000000-0000-7000-8000-000000000003','procedure.work'),
    ('00000000-0000-7000-8000-000000000003','document.register'),
    ('00000000-0000-7000-8000-000000000003','report.view');

-- CAJERA
INSERT INTO role_permission (role_id, permission_key) VALUES
    ('00000000-0000-7000-8000-000000000004','person.read'),
    ('00000000-0000-7000-8000-000000000004','invoice.issue'),
    ('00000000-0000-7000-8000-000000000004','invoice.reprint'),
    ('00000000-0000-7000-8000-000000000004','payment.register'),
    ('00000000-0000-7000-8000-000000000004','cash.session.open'),
    ('00000000-0000-7000-8000-000000000004','cash.session.close'),
    ('00000000-0000-7000-8000-000000000004','report.view');
