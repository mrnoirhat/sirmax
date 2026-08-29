// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.BackupEngine;
import org.sirmax.application.port.BackupRepository;
import org.sirmax.application.port.CloudBackupTarget;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.NumberingRepository;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.backup.BackupKind;
import org.sirmax.domain.backup.BackupRecord;
import org.sirmax.domain.backup.BackupSchedule;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Result;

/**
 * Runs the §41 backup pipeline and records the result.
 *
 * <p>Snapshot, compress, encrypt, hash — then <b>immediately validate</b> by re-reading the file.
 * A backup that has never been read back is a promise, not a copy, and the moment to discover a
 * failing disk is now rather than during a restore.
 *
 * <p>Uploading off-site only happens when the schedule says so and a folder has been chosen. §41 is
 * explicit that data must never leave silently, so this use case will not invent a destination.
 *
 * <p>A failed upload does not fail the backup. The local copy exists and is valid; losing the
 * off-site leg is worth a warning, not throwing away a good archive.
 */
public final class CreateBackup implements UseCase<CreateBackup.Command, BackupRecord> {

    private static final String SEQUENCE = "BKP";
    private static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();

    /**
     * @param passphrase encrypts the archive; the schedule decides whether one is expected
     * @param reason recorded on the backup, e.g. why a manual one was taken
     */
    public record Command(
            Session session,
            BackupKind kind,
            Optional<char[]> passphrase,
            Optional<String> reason,
            String source) {}

    private final BackupRepository backups;
    private final BackupEngine engine;
    private final CloudBackupTarget cloud;
    private final NumberingRepository numbering;
    private final IdGenerator ids;
    private final Clock clock;
    private final Audit audit;

    public CreateBackup(
            BackupRepository backups,
            BackupEngine engine,
            CloudBackupTarget cloud,
            NumberingRepository numbering,
            IdGenerator ids,
            Clock clock,
            Audit audit) {
        this.backups = backups;
        this.engine = engine;
        this.cloud = cloud;
        this.numbering = numbering;
        this.ids = ids;
        this.clock = clock;
        this.audit = audit;
    }

    @Override
    public Result<BackupRecord> execute(Command c) {
        if (!c.session().can(Permission.BACKUP_RUN)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }

        BackupSchedule schedule = backups.loadSchedule();
        if (schedule.encrypt() && c.passphrase().isEmpty()) {
            return Result.err("PASSPHRASE_REQUIRED", "backup.passphrase_required");
        }

        Instant now = clock.now();
        int year = LocalDate.ofInstant(now, LOCAL_ZONE).getYear();
        String code = numbering.allocate(SEQUENCE, SEQUENCE, year);
        String fileName = code + ".sirmax";

        BackupEngine.Archive archive;
        try {
            archive = engine.create(fileName, c.passphrase());
        } catch (RuntimeException e) {
            audit.record(
                    c.session().audit(c.source()),
                    "backup.failed",
                    "Backup",
                    code,
                    null,
                    null,
                    e.getMessage());
            return Result.err("BACKUP_FAILED", "backup.failed");
        }

        BackupRecord record =
                BackupRecord.created(
                        ids.newId(),
                        code,
                        c.kind(),
                        archive.storagePath(),
                        archive.sizeBytes(),
                        archive.sha256(),
                        archive.compressed(),
                        archive.encrypted(),
                        archive.schemaVersion(),
                        archive.rowCounts(),
                        c.session().user().id(),
                        now);

        // Read it back straight away. A backup nobody has read is not yet a backup.
        if (engine.validate(archive.storagePath(), archive.sha256())) {
            record.markValidated(now);
        } else {
            record.markCorrupt("La verificación inmediata no coincidió", now);
        }

        if (record.isRestorable() && schedule.uploadToDrive() && cloud.isConfigured()) {
            uploadQuietly(record, schedule, fileName, now);
        }

        backups.save(record);
        backups.save(recordRun(schedule, now));

        audit.record(
                c.session().audit(c.source()),
                "backup.created",
                "Backup",
                record.id(),
                null,
                code + " " + archive.sizeBytes() + " bytes " + record.status(),
                c.reason().orElse(null));
        return Result.ok(record);
    }

    /** A backup that could not be uploaded is still a backup; the operator is told, not blocked. */
    private void uploadQuietly(
            BackupRecord record, BackupSchedule schedule, String fileName, Instant now) {
        try {
            String fileId =
                    cloud.upload(
                            record.storagePath(), fileName, schedule.driveFolderId().orElseThrow());
            record.markUploaded(cloud.provider(), fileId, now);
        } catch (RuntimeException e) {
            record.markValidated(now); // keep the local verdict; only the off-site leg failed
        }
    }

    private BackupSchedule recordRun(BackupSchedule schedule, Instant now) {
        schedule.recordRun(now);
        return schedule;
    }
}
