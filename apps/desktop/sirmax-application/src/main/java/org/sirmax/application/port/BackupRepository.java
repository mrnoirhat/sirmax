// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.sirmax.domain.backup.BackupKind;
import org.sirmax.domain.backup.BackupRecord;
import org.sirmax.domain.backup.BackupSchedule;

/** Persistence for the backup history, the restore log and the installation's backup policy. */
public interface BackupRepository {

    void save(BackupRecord record);

    Optional<BackupRecord> findById(String id);

    Optional<BackupRecord> findByCode(String code);

    /** The backup history, newest first (§41 — an operator must be able to see what exists). */
    List<BackupRecord> list(int limit, int offset);

    /**
     * Routine copies older than the newest {@code keep}, oldest first — what the retention sweep
     * may delete. Emergency and pre-migration backups never appear here.
     */
    List<BackupRecord> routineBackupsBeyond(int keep);

    Optional<BackupRecord> mostRecent(BackupKind kind);

    // ── restore log (§42 step 7) ──

    void recordRestore(
            String id,
            String backupRecordId,
            String emergencyBackupId,
            String status,
            Instant startedAt,
            Instant finishedAt,
            String performedBy,
            String failureReason);

    List<RestoreEntry> restoreHistory(int limit);

    record RestoreEntry(
            String id,
            String backupRecordId,
            Optional<String> emergencyBackupId,
            String status,
            Instant startedAt,
            Optional<Instant> finishedAt,
            Optional<String> performedBy,
            Optional<String> failureReason) {}

    // ── policy ──

    BackupSchedule loadSchedule();

    void save(BackupSchedule schedule);
}
