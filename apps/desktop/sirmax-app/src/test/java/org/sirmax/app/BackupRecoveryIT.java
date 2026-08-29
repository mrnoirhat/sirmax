// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sirmax.application.security.Session;
import org.sirmax.application.usecase.Authenticate;
import org.sirmax.application.usecase.CreateBackup;
import org.sirmax.application.usecase.ManageBackupPolicy;
import org.sirmax.application.usecase.ProvisionInitialAdmin;
import org.sirmax.application.usecase.RegisterPerson;
import org.sirmax.application.usecase.RestoreBackup;
import org.sirmax.domain.backup.BackupKind;
import org.sirmax.domain.backup.BackupRecord;
import org.sirmax.domain.backup.BackupSchedule;
import org.sirmax.domain.backup.BackupStatus;
import org.sirmax.infrastructure.AppPaths;
import org.sirmax.infrastructure.persistence.SqliteDatabase;
import org.sirmax.shared.Result;

/**
 * Backup and recovery against the real graph, on a real file — master prompt §41, §42.
 *
 * <p>The §42 sequence is the thing being proved: an emergency copy of the current state is taken
 * before anything is overwritten, the target is validated first, and the whole act is recorded. A
 * restore is the most destructive operation SIRMAX offers, and none of those steps may be optional.
 */
class BackupRecoveryIT {

    private static final char[] PASSPHRASE = "una-frase-de-respaldo-larga".toCharArray();

    @TempDir Path root;

    private AppPaths paths;
    private SqliteDatabase database;
    private CompositionRoot app;
    private Session admin;

    @BeforeEach
    void setUp() {
        paths = AppPaths.under(root);
        database = SqliteDatabase.openAt(paths.databaseFile());
        app = CompositionRoot.bootstrap(database, paths);

        app.provisionInitialAdmin()
                .execute(
                        new ProvisionInitialAdmin.Command(
                                "Ayuntamiento de Santiago",
                                "Santiago",
                                "DO",
                                "admin",
                                "Administradora",
                                "una-contrasena-larga".toCharArray()));
        admin =
                app.authenticate()
                        .execute(
                                new Authenticate.Command(
                                        "admin", "una-contrasena-larga".toCharArray(), "test"))
                        .orElseThrow();
    }

    @AfterEach
    void tearDown() {
        try {
            database.close();
        } catch (RuntimeException alreadyClosedByRestore) {
            // a restore closes the connection deliberately
        }
    }

    private void registerCitizen(String given, String family) {
        app.registerPerson()
                .execute(
                        new RegisterPerson.Command(
                                admin,
                                given,
                                family,
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                "test"))
                .orElseThrow();
    }

    private Result<BackupRecord> backup(BackupKind kind) {
        return app.createBackup()
                .execute(
                        new CreateBackup.Command(
                                admin, kind, Optional.of(PASSPHRASE), Optional.empty(), "test"));
    }

    @Test
    void aBackupIsCreatedNumberedAndImmediatelyValidated() {
        registerCitizen("Ana", "Rodríguez Cruz");

        BackupRecord record = backup(BackupKind.MANUAL).orElseThrow();

        assertThat(record.code()).isEqualTo("BKP-2026-000001");
        assertThat(record.status()).isEqualTo(BackupStatus.VALIDATED);
        assertThat(record.validatedAt()).isPresent();
        assertThat(record.encrypted()).isTrue();
        assertThat(record.isOffsite()).isFalse();
        assertThat(record.rowCounts()).containsEntry("person", 1L);
        assertThat(Path.of(record.storagePath())).exists();
    }

    @Test
    void encryptionIsRefusedWithoutAPassphraseWhenThePolicyRequiresIt() {
        Result<BackupRecord> result =
                app.createBackup()
                        .execute(
                                new CreateBackup.Command(
                                        admin,
                                        BackupKind.MANUAL,
                                        Optional.empty(),
                                        Optional.empty(),
                                        "test"));

        assertThat(((Result.Err<?>) result).messageKey()).isEqualTo("backup.passphrase_required");
    }

    /** The §42 sequence, in full. */
    @Test
    void aRestoreTakesAnEmergencyCopyFirstAndIsRecorded() {
        registerCitizen("Ana", "Rodríguez Cruz");
        BackupRecord target = backup(BackupKind.MANUAL).orElseThrow();

        // Work done after the backup — this is what a restore discards, and what the emergency
        // copy is the only way back to.
        registerCitizen("Pedro", "Martínez");
        assertThat(app.people().countSearch("")).isEqualTo(2);

        RestoreBackup.Outcome outcome =
                app.restoreBackup()
                        .execute(
                                new RestoreBackup.Command(
                                        admin,
                                        target.id(),
                                        true,
                                        Optional.of(PASSPHRASE),
                                        Optional.empty(),
                                        "test"))
                        .orElseThrow();

        assertThat(outcome.requiresRestart()).isTrue();
        assertThat(outcome.emergencyBackup()).isPresent();
        BackupRecord emergency = outcome.emergencyBackup().orElseThrow();
        assertThat(emergency.kind()).isEqualTo(BackupKind.EMERGENCY);
        // The emergency copy holds the state that was discarded, not the state restored.
        assertThat(emergency.rowCounts()).containsEntry("person", 2L);

        // The live file is now the older one.
        assertThat(personCount(paths.databaseFile())).isEqualTo(1);
    }

    @Test
    void aRestoreWithoutConfirmationIsRefused() {
        BackupRecord target = backup(BackupKind.MANUAL).orElseThrow();

        Result<?> result =
                app.restoreBackup()
                        .execute(
                                new RestoreBackup.Command(
                                        admin,
                                        target.id(),
                                        false,
                                        Optional.of(PASSPHRASE),
                                        Optional.empty(),
                                        "test"));

        assertThat(((Result.Err<?>) result).messageKey())
                .isEqualTo("backup.restore_needs_confirmation");
    }

    @Test
    void aCorruptTargetIsCaughtBeforeTheDatabaseIsTouched() throws IOException {
        registerCitizen("Ana", "Rodríguez Cruz");
        BackupRecord target = backup(BackupKind.MANUAL).orElseThrow();

        Path archive = Path.of(target.storagePath());
        byte[] bytes = Files.readAllBytes(archive);
        bytes[bytes.length - 10] ^= 0x3C;
        Files.write(archive, bytes);

        Result<?> result =
                app.restoreBackup()
                        .execute(
                                new RestoreBackup.Command(
                                        admin,
                                        target.id(),
                                        true,
                                        Optional.of(PASSPHRASE),
                                        Optional.empty(),
                                        "test"));

        assertThat(((Result.Err<?>) result).messageKey()).isEqualTo("backup.corrupt");
        assertThat(app.backups().findById(target.id()).orElseThrow().status())
                .isEqualTo(BackupStatus.CORRUPT);
        // Nothing was replaced, and no emergency copy was needed.
        assertThat(personCount(paths.databaseFile())).isEqualTo(1);
    }

    @Test
    void offsiteUploadStaysOffUntilAFolderIsChosen() {
        BackupSchedule schedule = app.manageBackupPolicy().schedule();
        assertThat(schedule.uploadToDrive()).isFalse();

        // No Google account is connected in a test environment, so enabling it is refused rather
        // than half-configured.
        Result<?> result =
                app.manageBackupPolicy()
                        .configureDrive(
                                new ManageBackupPolicy.DriveCommand(
                                        admin, Optional.of("folder-123"), "test"));

        assertThat(((Result.Err<?>) result).messageKey()).isEqualTo("backup.drive_not_connected");
        assertThat(app.manageBackupPolicy().schedule().uploadToDrive()).isFalse();
    }

    @Test
    void retentionSweepsRoutineCopiesButKeepsEmergencyOnes() {
        backup(BackupKind.MANUAL).orElseThrow();
        BackupRecord emergency = backup(BackupKind.EMERGENCY).orElseThrow();
        BackupRecord newest = backup(BackupKind.MANUAL).orElseThrow();

        app.manageBackupPolicy()
                .configure(
                        new ManageBackupPolicy.ConfigureCommand(
                                admin, true, BackupSchedule.Frequency.DAILY, 20, 1, true, "test"));
        int removed = app.manageBackupPolicy().sweep(app.manageBackupPolicy().schedule());

        assertThat(removed).isEqualTo(1);
        assertThat(app.backups().findById(emergency.id()).orElseThrow().status())
                .isEqualTo(BackupStatus.VALIDATED);
        assertThat(app.backups().findById(newest.id()).orElseThrow().status())
                .isEqualTo(BackupStatus.VALIDATED);
        assertThat(Path.of(newest.storagePath())).exists();
    }

    @Test
    void aPrunedRecordKeepsItsHistoryEvenThoughTheFileIsGone() {
        BackupRecord old = backup(BackupKind.MANUAL).orElseThrow();
        backup(BackupKind.MANUAL).orElseThrow();

        app.manageBackupPolicy()
                .configure(
                        new ManageBackupPolicy.ConfigureCommand(
                                admin, true, BackupSchedule.Frequency.DAILY, 20, 1, true, "test"));
        app.manageBackupPolicy().sweep(app.manageBackupPolicy().schedule());

        BackupRecord pruned = app.backups().findById(old.id()).orElseThrow();
        assertThat(pruned.status()).isEqualTo(BackupStatus.PRUNED);
        assertThat(pruned.sha256()).isEqualTo(old.sha256());
        assertThat(pruned.rowCounts()).isEqualTo(old.rowCounts());
        assertThat(Path.of(pruned.storagePath())).doesNotExist();
    }

    @Test
    void theScheduleIsDueOnAFreshInstallAndNotAgainTheSameDay() {
        BackupSchedule schedule = app.manageBackupPolicy().schedule();
        ZoneId zone = ZoneId.systemDefault();

        assertThat(schedule.isDue(Instant.now(), zone)).isTrue();

        app.manageBackupPolicy().runIfDue(admin, Optional.of(PASSPHRASE));

        assertThat(app.manageBackupPolicy().schedule().isDue(Instant.now(), zone)).isFalse();
    }

    @Test
    void backupsAreAuditedAndARestoredSystemCanExplainItsOwnProvenance() {
        BackupRecord target = backup(BackupKind.MANUAL).orElseThrow();

        assertThat(auditActions()).contains("backup.created");

        RestoreBackup.Outcome outcome =
                app.restoreBackup()
                        .execute(
                                new RestoreBackup.Command(
                                        admin,
                                        target.id(),
                                        true,
                                        Optional.of(PASSPHRASE),
                                        Optional.empty(),
                                        "test"))
                        .orElseThrow();

        // The restored database predates its own backup history, so the restore re-inscribes it.
        // Without this, the emergency copy holding the discarded state would be unfindable.
        assertThat(auditActions()).contains("backup.restored");
        assertThat(app.backups().findById(target.id())).isPresent();
        assertThat(app.backups().findById(outcome.emergencyBackup().orElseThrow().id()))
                .isPresent();

        var restores = app.backups().restoreHistory(10);
        assertThat(restores).hasSize(1);
        assertThat(restores.get(0).status()).isEqualTo("COMPLETED");
        assertThat(restores.get(0).emergencyBackupId())
                .contains(outcome.emergencyBackup().orElseThrow().id());
    }

    private List<String> auditActions() {
        return app.auditTrail()
                .search(
                        Optional.of("Backup"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        50,
                        0)
                .stream()
                .map(org.sirmax.domain.audit.AuditEvent::action)
                .toList();
    }

    private long personCount(Path file) {
        try (var connection = java.sql.DriverManager.getConnection("jdbc:sqlite:" + file);
                var statement = connection.createStatement();
                var rs = statement.executeQuery("SELECT count(*) FROM person")) {
            return rs.next() ? rs.getLong(1) : -1;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
