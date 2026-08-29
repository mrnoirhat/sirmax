// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.sirmax.application.port.BackupEngine;
import org.sirmax.application.port.BackupRepository;
import org.sirmax.application.port.CloudBackupTarget;
import org.sirmax.application.port.Clock;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.backup.BackupKind;
import org.sirmax.domain.backup.BackupRecord;
import org.sirmax.domain.backup.BackupSchedule;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Result;

/**
 * The backup policy and the housekeeping around it (master prompt §41).
 *
 * <p>Three jobs that share a repository and belong to the same screen: configuring the schedule,
 * running it when due, and sweeping old copies. Splitting them would triple the wiring for no gain.
 *
 * <p>The retention sweep only ever removes <em>routine</em> copies. Emergency and pre-migration
 * backups exist precisely for the moment something went wrong, which is exactly when a retention
 * rule would otherwise have thrown them away.
 */
public final class ManageBackupPolicy {

    private static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();

    public record ConfigureCommand(
            Session session,
            boolean enabled,
            BackupSchedule.Frequency frequency,
            int hourOfDay,
            int keepCopies,
            boolean encrypt,
            String source) {}

    /**
     * @param folderId the Drive folder the municipality chose; empty turns off-site copies off
     */
    public record DriveCommand(Session session, Optional<String> folderId, String source) {}

    private final BackupRepository backups;
    private final BackupEngine engine;
    private final CloudBackupTarget cloud;
    private final CreateBackup createBackup;
    private final Clock clock;
    private final Audit audit;

    public ManageBackupPolicy(
            BackupRepository backups,
            BackupEngine engine,
            CloudBackupTarget cloud,
            CreateBackup createBackup,
            Clock clock,
            Audit audit) {
        this.backups = backups;
        this.engine = engine;
        this.cloud = cloud;
        this.createBackup = createBackup;
        this.clock = clock;
        this.audit = audit;
    }

    public BackupSchedule schedule() {
        return backups.loadSchedule();
    }

    public Result<BackupSchedule> configure(ConfigureCommand c) {
        if (!c.session().can(Permission.CONFIG_MANAGE)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        BackupSchedule schedule = backups.loadSchedule();
        try {
            schedule.configure(
                    c.enabled(),
                    c.frequency(),
                    c.hourOfDay(),
                    c.keepCopies(),
                    c.encrypt(),
                    clock.now());
        } catch (IllegalArgumentException e) {
            return Result.err("INVALID_SCHEDULE", "backup.invalid_schedule");
        }
        backups.save(schedule);
        audit.record(
                c.session().audit(c.source()),
                "backup.schedule_changed",
                "BackupSchedule",
                "1",
                null,
                c.frequency() + " " + c.hourOfDay() + "h keep=" + c.keepCopies(),
                null);
        return Result.ok(schedule);
    }

    /**
     * Turn off-site copies on or off. Turning them on is a deliberate act with a named folder —
     * §41 forbids sending a citizen register anywhere silently.
     */
    public Result<BackupSchedule> configureDrive(DriveCommand c) {
        if (!c.session().can(Permission.CONFIG_MANAGE)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        BackupSchedule schedule = backups.loadSchedule();
        Instant now = clock.now();

        if (c.folderId().isEmpty()) {
            schedule.disableDriveUpload(now);
        } else {
            if (!cloud.isConfigured()) {
                return Result.err("DRIVE_NOT_CONNECTED", "backup.drive_not_connected");
            }
            schedule.enableDriveUpload(c.folderId().get(), now);
        }
        backups.save(schedule);
        audit.record(
                c.session().audit(c.source()),
                c.folderId().isPresent() ? "backup.drive_enabled" : "backup.drive_disabled",
                "BackupSchedule",
                "1",
                null,
                c.folderId().orElse(null),
                null);
        return Result.ok(schedule);
    }

    /**
     * Run the scheduled backup if one is due, then sweep old copies.
     *
     * <p>Called at start-up and on a timer. Returns empty when nothing was due, which is the normal
     * case and not worth a notification.
     */
    public Optional<BackupRecord> runIfDue(Session session, Optional<char[]> passphrase) {
        BackupSchedule schedule = backups.loadSchedule();
        if (!schedule.isDue(clock.now(), LOCAL_ZONE)) {
            return Optional.empty();
        }
        Result<BackupRecord> result =
                createBackup.execute(
                        new CreateBackup.Command(
                                session,
                                BackupKind.SCHEDULED,
                                passphrase,
                                Optional.empty(),
                                "schedule"));
        result.optional().ifPresent(r -> sweep(schedule));
        return result.optional();
    }

    /**
     * Delete routine archives beyond the retention count. The {@link BackupRecord} rows stay: the
     * history of what was backed up and when is worth keeping long after the bytes are gone.
     */
    public int sweep(BackupSchedule schedule) {
        List<BackupRecord> stale = backups.routineBackupsBeyond(schedule.keepCopies());
        int removed = 0;
        Instant now = clock.now();
        for (BackupRecord record : stale) {
            engine.deleteArchive(record.storagePath());
            // PRUNED, not FAILED: this backup was fine, its file is simply gone. The row keeps the
            // hash, the fingerprint and the remote file id, so an off-site copy stays reachable.
            record.markPruned(now);
            backups.save(record);
            removed++;
        }
        return removed;
    }
}
