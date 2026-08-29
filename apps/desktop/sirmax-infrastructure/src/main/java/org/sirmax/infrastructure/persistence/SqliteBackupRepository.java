// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.sirmax.infrastructure.persistence.JdbcHelper.bool;
import static org.sirmax.infrastructure.persistence.JdbcHelper.instant;
import static org.sirmax.infrastructure.persistence.JdbcHelper.str;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sirmax.application.port.BackupRepository;
import org.sirmax.domain.backup.BackupKind;
import org.sirmax.domain.backup.BackupRecord;
import org.sirmax.domain.backup.BackupSchedule;
import org.sirmax.domain.backup.BackupStatus;
import org.sirmax.shared.SirmaxException;

/** SQLite persistence for the backup history, the restore log and the backup policy. */
public final class SqliteBackupRepository implements BackupRepository {

    private static final ObjectMapper M = new ObjectMapper();

    private final SqliteDatabase db;

    public SqliteBackupRepository(SqliteDatabase db) {
        this.db = db;
    }

    @Override
    public void save(BackupRecord r) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO backup_record"
                    + " (id, code, kind, status, storage_path, size_bytes, sha256, compressed,"
                    + "  encrypted, schema_version, row_counts_json, created_at, created_by,"
                    + "  validated_at, remote_provider, remote_file_id, uploaded_at, notes)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT(id) DO UPDATE SET status=excluded.status,"
                    + " validated_at=excluded.validated_at,"
                    + " remote_provider=excluded.remote_provider,"
                    + " remote_file_id=excluded.remote_file_id, uploaded_at=excluded.uploaded_at,"
                    + " notes=excluded.notes",
                r.id(),
                r.code(),
                r.kind().name(),
                r.status().name(),
                r.storagePath(),
                r.sizeBytes(),
                r.sha256(),
                r.compressed(),
                r.encrypted(),
                r.schemaVersion(),
                rowCountsToJson(r.rowCounts()),
                r.createdAt(),
                r.createdBy().orElse(null),
                r.validatedAt().orElse(null),
                r.remoteProvider().orElse(null),
                r.remoteFileId().orElse(null),
                r.uploadedAt().orElse(null),
                r.notes().orElse(null));
    }

    @Override
    public Optional<BackupRecord> findById(String id) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM backup_record WHERE id = ?",
                SqliteBackupRepository::map,
                id);
    }

    @Override
    public Optional<BackupRecord> findByCode(String code) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM backup_record WHERE code = ?",
                SqliteBackupRepository::map,
                code);
    }

    @Override
    public List<BackupRecord> list(int limit, int offset) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM backup_record ORDER BY created_at DESC LIMIT ? OFFSET ?",
                SqliteBackupRepository::map,
                limit,
                offset);
    }

    @Override
    public List<BackupRecord> routineBackupsBeyond(int keep) {
        // Only MANUAL and SCHEDULED, and only ones that still have a local file to delete.
        // Emergency and pre-migration copies are exempt from retention by design.
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM backup_record"
                        + " WHERE kind IN ('MANUAL','SCHEDULED')"
                        + " AND status NOT IN ('PRUNED','FAILED')"
                        + " ORDER BY created_at DESC LIMIT -1 OFFSET ?",
                SqliteBackupRepository::map,
                keep);
    }

    @Override
    public Optional<BackupRecord> mostRecent(BackupKind kind) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM backup_record WHERE kind = ? ORDER BY created_at DESC LIMIT 1",
                SqliteBackupRepository::map,
                kind.name());
    }

    // ── restore log ──

    @Override
    public void recordRestore(
            String id,
            String backupRecordId,
            String emergencyBackupId,
            String status,
            Instant startedAt,
            Instant finishedAt,
            String performedBy,
            String failureReason) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO restore_record"
                        + " (id, backup_record_id, emergency_backup_id, status, started_at,"
                        + "  finished_at, performed_by, failure_reason)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                backupRecordId,
                emergencyBackupId,
                status,
                startedAt,
                finishedAt,
                performedBy,
                failureReason);
    }

    @Override
    public List<RestoreEntry> restoreHistory(int limit) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM restore_record ORDER BY started_at DESC LIMIT ?",
                SqliteBackupRepository::mapRestore,
                limit);
    }

    // ── policy ──

    @Override
    public BackupSchedule loadSchedule() {
        return JdbcHelper.queryOne(
                        db.connection(),
                        "SELECT * FROM backup_schedule WHERE id = 1",
                        SqliteBackupRepository::mapSchedule)
                .orElseGet(() -> BackupSchedule.defaults(Instant.EPOCH));
    }

    @Override
    public void save(BackupSchedule s) {
        JdbcHelper.update(
                db.connection(),
                "UPDATE backup_schedule SET enabled = ?, frequency = ?, hour_of_day = ?,"
                        + " keep_copies = ?, encrypt = ?, upload_to_drive = ?, drive_folder_id = ?,"
                        + " last_run_at = ?, updated_at = ? WHERE id = 1",
                s.enabled(),
                s.frequency().name(),
                s.hourOfDay(),
                s.keepCopies(),
                s.encrypt(),
                s.uploadToDrive(),
                s.driveFolderId().orElse(null),
                s.lastRunAt().orElse(null),
                s.updatedAt());
    }

    // ── mapping ──

    private static String rowCountsToJson(Map<String, Long> counts) {
        ObjectNode node = M.createObjectNode();
        counts.forEach(node::put);
        return node.toString();
    }

    private static Map<String, Long> rowCountsFromJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            JsonNode root = M.readTree(json);
            Map<String, Long> counts = new LinkedHashMap<>();
            root.fields().forEachRemaining(e -> counts.put(e.getKey(), e.getValue().asLong()));
            return Map.copyOf(counts);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new SirmaxException("Could not read the backup fingerprint", e);
        }
    }

    private static BackupRecord map(ResultSet rs) throws SQLException {
        return new BackupRecord(
                rs.getString("id"),
                rs.getString("code"),
                BackupKind.valueOf(rs.getString("kind")),
                BackupStatus.valueOf(rs.getString("status")),
                rs.getString("storage_path"),
                rs.getLong("size_bytes"),
                rs.getString("sha256"),
                bool(rs, "compressed"),
                bool(rs, "encrypted"),
                rs.getInt("schema_version"),
                rowCountsFromJson(rs.getString("row_counts_json")),
                instant(rs, "created_at"),
                str(rs, "created_by"),
                instant(rs, "validated_at"),
                str(rs, "remote_provider"),
                str(rs, "remote_file_id"),
                instant(rs, "uploaded_at"),
                str(rs, "notes"));
    }

    private static RestoreEntry mapRestore(ResultSet rs) throws SQLException {
        return new RestoreEntry(
                rs.getString("id"),
                rs.getString("backup_record_id"),
                Optional.ofNullable(str(rs, "emergency_backup_id")),
                rs.getString("status"),
                instant(rs, "started_at"),
                Optional.ofNullable(instant(rs, "finished_at")),
                Optional.ofNullable(str(rs, "performed_by")),
                Optional.ofNullable(str(rs, "failure_reason")));
    }

    private static BackupSchedule mapSchedule(ResultSet rs) throws SQLException {
        return new BackupSchedule(
                bool(rs, "enabled"),
                BackupSchedule.Frequency.valueOf(rs.getString("frequency")),
                rs.getInt("hour_of_day"),
                rs.getInt("keep_copies"),
                bool(rs, "encrypt"),
                bool(rs, "upload_to_drive"),
                str(rs, "drive_folder_id"),
                instant(rs, "last_run_at"),
                instant(rs, "updated_at"));
    }
}
