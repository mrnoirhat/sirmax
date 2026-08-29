-- SPDX-License-Identifier: AGPL-3.0-or-later
-- SIRMAX security and reliability hardening (Phase 10, master prompt §40, §43).
--
-- Three things this migration adds, in order of how much they matter:
--
-- 1. An integrity chain over `audit_event`. The append-only triggers from V0001
--    stop UPDATE and DELETE through SQL, but they are themselves droppable by
--    anyone holding the file. A hash chain does not prevent tampering — it makes
--    tampering *detectable*, which is the property §40 actually asks for.
-- 2. Account lockout after repeated failed sign-ins, with the attempts recorded
--    so an administrator can tell a forgotten password from an attack.
-- 3. A session policy: idle timeout and lock, because a municipal counter PC is
--    left unattended constantly.

-- ─────────────────────────────────────────────────────────────
-- Audit integrity chain (§40)
-- ─────────────────────────────────────────────────────────────
-- Each entry's hash covers its own content *and* the previous entry's hash, so
-- altering or removing any entry breaks every hash after it. Rows written before
-- this migration have no hash; the verifier reports where the chain begins
-- rather than pretending to vouch for what came earlier.
ALTER TABLE audit_event ADD COLUMN prev_hash TEXT;
ALTER TABLE audit_event ADD COLUMN entry_hash TEXT;
CREATE INDEX ix_audit_event_hash ON audit_event(entry_hash);

-- ─────────────────────────────────────────────────────────────
-- Sign-in attempts and account lockout (§43)
-- ─────────────────────────────────────────────────────────────
-- Every attempt, successful or not. Recording the successes too is what lets an
-- administrator distinguish "she keeps mistyping it" from "someone is trying
-- usernames at 3am".
CREATE TABLE login_attempt (
    id            TEXT PRIMARY KEY,
    username      TEXT NOT NULL,               -- as typed; the account may not exist
    user_id       TEXT REFERENCES app_user(id) ON DELETE SET NULL,
    succeeded     INTEGER NOT NULL CHECK (succeeded IN (0,1)),
    attempted_at  TEXT NOT NULL,
    source        TEXT NOT NULL,
    failure_kind  TEXT CHECK (failure_kind IS NULL OR failure_kind IN
                  ('UNKNOWN_USER','BAD_PASSWORD','LOCKED','DISABLED'))
);
CREATE INDEX ix_login_attempt_username ON login_attempt(lower(username), attempted_at DESC);
CREATE INDEX ix_login_attempt_when ON login_attempt(attempted_at DESC);

-- Lockout state lives on the account, so a lock survives a restart and is
-- visible wherever the user is administered.
ALTER TABLE app_user ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE app_user ADD COLUMN locked_until TEXT;

-- ─────────────────────────────────────────────────────────────
-- Security policy (§43)
-- ─────────────────────────────────────────────────────────────
-- One row. Defaults are deliberately mild: a municipal counter that locks every
-- five minutes gets its policy switched off entirely, which is worse than a
-- longer timeout somebody keeps.
CREATE TABLE security_policy (
    id                       INTEGER PRIMARY KEY CHECK (id = 1),
    min_password_length      INTEGER NOT NULL DEFAULT 12 CHECK (min_password_length >= 8),
    max_failed_attempts      INTEGER NOT NULL DEFAULT 5 CHECK (max_failed_attempts >= 3),
    lockout_minutes          INTEGER NOT NULL DEFAULT 15 CHECK (lockout_minutes >= 1),
    idle_lock_minutes        INTEGER NOT NULL DEFAULT 20 CHECK (idle_lock_minutes >= 1),
    session_max_hours        INTEGER NOT NULL DEFAULT 12 CHECK (session_max_hours >= 1),
    -- attachment safety (§43 "safe file validation")
    max_attachment_mb        INTEGER NOT NULL DEFAULT 25 CHECK (max_attachment_mb >= 1),
    updated_at               TEXT NOT NULL
);
INSERT INTO security_policy (id, updated_at) VALUES (1, '1970-01-01T00:00:00Z');
