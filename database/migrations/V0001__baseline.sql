-- SPDX-License-Identifier: AGPL-3.0-or-later
-- SIRMAX baseline migration.
--
-- Phase 1: establishes only the migration bookkeeping table and the audit
-- table (audit is a Phase 0 foundation per master prompt §40). The core
-- domain schema — organization, departments, users/roles/permissions,
-- people/organizations, addresses, services, procedures, documents,
-- finance — is added in Phase 3+ as V0002, V0003, ... See docs/domain/erd.md.
--
-- Runner contract: applied inside a single transaction with
--   PRAGMA foreign_keys = ON;
-- already set on the connection (see SqliteConnectionFactory).

-- ─────────────────────────────────────────────────────────────
-- Migration bookkeeping
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS schema_migrations (
    version      INTEGER PRIMARY KEY,
    description  TEXT    NOT NULL,
    checksum     TEXT    NOT NULL,
    applied_at   TEXT    NOT NULL,   -- ISO-8601 UTC
    success      INTEGER NOT NULL DEFAULT 1 CHECK (success IN (0, 1))
);

-- ─────────────────────────────────────────────────────────────
-- Audit trail (append-only; never updated or deleted from the app)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_event (
    id            TEXT PRIMARY KEY,          -- UUIDv7 (time-ordered)
    when_at       TEXT NOT NULL,             -- ISO-8601 UTC
    actor_user_id TEXT,                      -- NULL for system actions
    action        TEXT NOT NULL,             -- e.g. 'invoice.void'
    entity_type   TEXT NOT NULL,             -- e.g. 'Invoice'
    entity_id     TEXT NOT NULL,
    before_json   TEXT,
    after_json    TEXT,
    reason        TEXT,
    session_id    TEXT NOT NULL,
    source        TEXT NOT NULL              -- device/host/channel
);

CREATE INDEX IF NOT EXISTS ix_audit_event_entity
    ON audit_event (entity_type, entity_id, when_at);
CREATE INDEX IF NOT EXISTS ix_audit_event_when
    ON audit_event (when_at);
CREATE INDEX IF NOT EXISTS ix_audit_event_actor
    ON audit_event (actor_user_id, when_at);

-- Guard: block UPDATE/DELETE on audit_event at the database level.
CREATE TRIGGER IF NOT EXISTS audit_event_no_update
BEFORE UPDATE ON audit_event
BEGIN
    SELECT RAISE(ABORT, 'audit_event is append-only');
END;

CREATE TRIGGER IF NOT EXISTS audit_event_no_delete
BEFORE DELETE ON audit_event
BEGIN
    SELECT RAISE(ABORT, 'audit_event is append-only');
END;

-- ─────────────────────────────────────────────────────────────
-- Application settings (typed value as JSON + data classification)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS app_setting (
    key            TEXT PRIMARY KEY,
    value_json     TEXT NOT NULL,
    classification TEXT NOT NULL DEFAULT 'INTERNAL'
                   CHECK (classification IN ('PUBLIC', 'INTERNAL', 'CONFIDENTIAL', 'RESTRICTED')),
    updated_at     TEXT NOT NULL
);
