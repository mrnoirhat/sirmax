-- SPDX-License-Identifier: AGPL-3.0-or-later
-- SIRMAX backup, recovery and off-site copies (Phase 9, master prompt §41, §42).
--
-- The pipeline §41 prescribes, in order:
--   snapshot → validate → compress → encrypt → hash → local → optional Drive
--
-- Two rules shape this schema. First, "never silently upload sensitive data to
-- an external service": uploading is opt-in per installation and recorded per
-- backup, so an operator can always answer "has any of this left the building?"
-- Second, §42's restore sequence is not advice — an emergency backup of the
-- current state is taken *before* any restore, which is why EMERGENCY is a
-- first-class backup kind rather than a note in a log.

CREATE TABLE backup_record (
    id              TEXT PRIMARY KEY,
    code            TEXT NOT NULL,              -- e.g. BKP-2026-000001
    kind            TEXT NOT NULL
                    CHECK (kind IN ('MANUAL','SCHEDULED','EMERGENCY','PRE_MIGRATION')),
    status          TEXT NOT NULL DEFAULT 'CREATED'
                    CHECK (status IN ('CREATED','VALIDATED','UPLOADED','FAILED','CORRUPT','PRUNED')),

    storage_path    TEXT NOT NULL,              -- under the app data directory
    size_bytes      INTEGER NOT NULL DEFAULT 0 CHECK (size_bytes >= 0),
    -- SHA-256 of the file as written (after compression and encryption). This is
    -- what validation re-computes; a mismatch means the archive is not the one
    -- SIRMAX wrote, whether through disk rot or tampering.
    sha256          TEXT NOT NULL,
    compressed      INTEGER NOT NULL DEFAULT 1 CHECK (compressed IN (0,1)),
    encrypted       INTEGER NOT NULL DEFAULT 0 CHECK (encrypted IN (0,1)),

    -- the schema version the snapshot was taken at; restoring an archive from a
    -- newer schema into an older binary would corrupt it silently
    schema_version  INTEGER NOT NULL DEFAULT 0,
    row_counts_json TEXT NOT NULL DEFAULT '{}', -- a coarse fingerprint for validation

    created_at      TEXT NOT NULL,
    created_by      TEXT REFERENCES app_user(id) ON DELETE SET NULL,
    validated_at    TEXT,

    -- off-site copy (§41). Null means this backup never left the building.
    remote_provider TEXT CHECK (remote_provider IS NULL OR remote_provider IN ('GOOGLE_DRIVE')),
    remote_file_id  TEXT,
    uploaded_at     TEXT,

    notes           TEXT
);
CREATE UNIQUE INDEX ux_backup_record_code ON backup_record(code);
CREATE INDEX ix_backup_record_created ON backup_record(created_at DESC);
CREATE INDEX ix_backup_record_kind ON backup_record(kind, status);

-- Every restore, successful or not. §42 step 7: a restore is the single most
-- destructive act available in SIRMAX and must never be deniable.
CREATE TABLE restore_record (
    id                  TEXT PRIMARY KEY,
    backup_record_id    TEXT NOT NULL REFERENCES backup_record(id) ON DELETE RESTRICT,
    -- the emergency backup taken of the state being replaced (§42 step 1)
    emergency_backup_id TEXT REFERENCES backup_record(id) ON DELETE SET NULL,
    status              TEXT NOT NULL
                        CHECK (status IN ('COMPLETED','FAILED','ABORTED')),
    started_at          TEXT NOT NULL,
    finished_at         TEXT,
    performed_by        TEXT REFERENCES app_user(id) ON DELETE SET NULL,
    failure_reason      TEXT
);
CREATE INDEX ix_restore_record_backup ON restore_record(backup_record_id);

-- One row: the installation's backup policy (§41 automatic scheduling).
CREATE TABLE backup_schedule (
    id                  INTEGER PRIMARY KEY CHECK (id = 1),
    enabled             INTEGER NOT NULL DEFAULT 1 CHECK (enabled IN (0,1)),
    frequency           TEXT NOT NULL DEFAULT 'DAILY'
                        CHECK (frequency IN ('DAILY','WEEKLY','MONTHLY')),
    hour_of_day         INTEGER NOT NULL DEFAULT 20 CHECK (hour_of_day BETWEEN 0 AND 23),
    keep_copies         INTEGER NOT NULL DEFAULT 30 CHECK (keep_copies >= 1),
    encrypt             INTEGER NOT NULL DEFAULT 1 CHECK (encrypt IN (0,1)),
    -- Off by default. §41: never silently upload sensitive data anywhere.
    upload_to_drive     INTEGER NOT NULL DEFAULT 0 CHECK (upload_to_drive IN (0,1)),
    drive_folder_id     TEXT,
    last_run_at         TEXT,
    updated_at          TEXT NOT NULL
);
INSERT INTO backup_schedule (id, enabled, frequency, hour_of_day, keep_copies, encrypt,
                             upload_to_drive, updated_at)
VALUES (1, 1, 'DAILY', 20, 30, 1, 0, '1970-01-01T00:00:00Z');

INSERT INTO numbering_sequence (code, prefix, padding, yearly_reset, period_year, next_value, updated_at)
VALUES ('BKP', 'BKP', 6, 1, 0, 1, '1970-01-01T00:00:00Z');
