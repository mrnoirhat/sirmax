// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.util.Optional;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.BackupEngine;
import org.sirmax.application.port.BackupRepository;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.backup.BackupKind;
import org.sirmax.domain.backup.BackupRecord;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Result;

/**
 * Replaces the live database with a backup, following the §42 safety sequence exactly:
 *
 * <ol>
 *   <li>take an emergency backup of the current state;
 *   <li>validate the target archive;
 *   <li>the operator has already confirmed — this use case is only reached from a confirmed dialog;
 *   <li>restore;
 *   <li>integrity-check the result (the engine runs SQLite's own check before overwriting);
 *   <li>the restored file replaces the live one atomically, WAL included;
 *   <li>record the restore.
 * </ol>
 *
 * <p>Steps 1 and 2 come before anything destructive on purpose. A restore is the single most
 * dangerous action in SIRMAX: it discards everything entered since the backup was taken. The
 * emergency copy is the only way back if the restore itself turns out to be the mistake, and it is
 * taken whether or not anyone remembered to ask for one.
 *
 * <p>Afterwards the application must be restarted: the database it was talking to no longer exists.
 * {@link Outcome#requiresRestart()} says so rather than pretending the running process can carry on.
 */
public final class RestoreBackup implements UseCase<RestoreBackup.Command, RestoreBackup.Outcome> {

    /**
     * @param confirmed the operator has seen what will be lost and said yes (§42 step 3)
     * @param passphrase needed when the archive is encrypted
     */
    public record Command(
            Session session,
            String backupRecordId,
            boolean confirmed,
            Optional<char[]> passphrase,
            Optional<char[]> emergencyPassphrase,
            String source) {}

    /**
     * @param emergencyBackup the copy taken of the state that was replaced — keep this code
     */
    public record Outcome(
            BackupRecord restoredFrom, Optional<BackupRecord> emergencyBackup) {

        /** Always true: the live database was swapped underneath the running application. */
        public boolean requiresRestart() {
            return true;
        }
    }

    private final BackupRepository backups;
    private final BackupEngine engine;
    private final CreateBackup createBackup;
    private final IdGenerator ids;
    private final Clock clock;
    private final Audit audit;

    public RestoreBackup(
            BackupRepository backups,
            BackupEngine engine,
            CreateBackup createBackup,
            IdGenerator ids,
            Clock clock,
            Audit audit) {
        this.backups = backups;
        this.engine = engine;
        this.createBackup = createBackup;
        this.ids = ids;
        this.clock = clock;
        this.audit = audit;
    }

    @Override
    public Result<Outcome> execute(Command c) {
        if (!c.session().can(Permission.BACKUP_RESTORE)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        if (!c.confirmed()) {
            return Result.err("NOT_CONFIRMED", "backup.restore_needs_confirmation");
        }

        Optional<BackupRecord> found = backups.findById(c.backupRecordId());
        if (found.isEmpty()) {
            return Result.err("BACKUP_NOT_FOUND", "backup.not_found");
        }
        BackupRecord target = found.get();
        if (target.encrypted() && c.passphrase().isEmpty()) {
            return Result.err("PASSPHRASE_REQUIRED", "backup.passphrase_required");
        }

        Instant startedAt = clock.now();

        // §42 step 2 — validate the target before destroying anything.
        if (!engine.validate(target.storagePath(), target.sha256())) {
            target.markCorrupt("Verificación previa a la restauración fallida", startedAt);
            backups.save(target);
            return Result.err("BACKUP_CORRUPT", "backup.corrupt");
        }
        target.markValidated(startedAt);
        backups.save(target);

        // §42 step 1 — the way back, taken before the point of no return.
        Optional<BackupRecord> emergency = takeEmergencyBackup(c);
        if (emergency.isEmpty()) {
            return Result.err("EMERGENCY_BACKUP_FAILED", "backup.emergency_failed");
        }

        try {
            engine.restore(target.storagePath(), c.passphrase());
        } catch (RuntimeException e) {
            backups.recordRestore(
                    ids.newId(),
                    target.id(),
                    emergency.get().id(),
                    "FAILED",
                    startedAt,
                    clock.now(),
                    c.session().user().id(),
                    e.getMessage());
            audit.record(
                    c.session().audit(c.source()),
                    "backup.restore_failed",
                    "Backup",
                    target.id(),
                    null,
                    null,
                    e.getMessage());
            return Result.err("RESTORE_FAILED", "backup.restore_failed");
        }

        // The restored database is an *older* one: it predates both the backup it was restored
        // from and the emergency copy just taken, so it knows about neither. Re-inscribing them
        // is what lets an operator open a restored system and answer "where did this come from,
        // and where is what it replaced?" — without which the emergency copy is unfindable.
        backups.save(target);
        backups.save(emergency.get());

        // §42 step 7.
        backups.recordRestore(
                ids.newId(),
                target.id(),
                emergency.get().id(),
                "COMPLETED",
                startedAt,
                clock.now(),
                c.session().user().id(),
                null);
        audit.record(
                c.session().audit(c.source()),
                "backup.restored",
                "Backup",
                target.id(),
                emergency.get().code(),
                target.code(),
                null);

        return Result.ok(new Outcome(target, emergency));
    }

    private Optional<BackupRecord> takeEmergencyBackup(Command c) {
        Result<BackupRecord> emergency =
                createBackup.execute(
                        new CreateBackup.Command(
                                c.session(),
                                BackupKind.EMERGENCY,
                                c.emergencyPassphrase().or(c::passphrase),
                                Optional.of("Copia previa a restaurar " + c.backupRecordId()),
                                c.source()));
        return emergency.optional().filter(BackupRecord::isRestorable);
    }
}
