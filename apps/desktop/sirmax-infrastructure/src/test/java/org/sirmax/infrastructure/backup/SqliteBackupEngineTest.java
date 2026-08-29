// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sirmax.application.port.BackupEngine;
import org.sirmax.domain.common.ArchiveStatus;
import org.sirmax.domain.identity.Person;
import org.sirmax.domain.identity.PersonName;
import org.sirmax.infrastructure.persistence.SqliteDatabase;
import org.sirmax.infrastructure.persistence.SqlitePersonRepository;
import org.sirmax.shared.SirmaxException;

/**
 * The §41 pipeline against a real on-disk SQLite database.
 *
 * <p>An in-memory database would make most of this vacuous: the point is that a snapshot of a live
 * file is consistent, that the archive round-trips through gzip and AES-GCM, and that a tampered one
 * is refused rather than restored.
 */
class SqliteBackupEngineTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final char[] PASSPHRASE = "una-frase-de-respaldo-larga".toCharArray();

    @TempDir Path root;

    private Path databaseFile;
    private Path backupsDir;
    private SqliteDatabase database;
    private SqliteBackupEngine engine;

    @BeforeEach
    void setUp() throws IOException {
        Path dataDir = Files.createDirectories(root.resolve("data"));
        backupsDir = Files.createDirectories(root.resolve("backups"));
        databaseFile = dataDir.resolve("sirmax.sqlite");

        database = SqliteDatabase.openAt(databaseFile);
        database.migrate();
        engine = new SqliteBackupEngine(database, backupsDir, databaseFile);

        savePerson("p-1", "Ana", "Rodríguez Cruz");
    }

    @AfterEach
    void tearDown() {
        try {
            database.close();
        } catch (RuntimeException alreadyClosedByRestore) {
            // restore() closes the connection on purpose; closing twice is not a failure
        }
    }

    private void savePerson(String id, String given, String family) {
        new SqlitePersonRepository(database)
                .save(
                        new Person(
                                id,
                                new PersonName(given, family),
                                null,
                                null,
                                null,
                                ArchiveStatus.ACTIVE,
                                NOW,
                                NOW));
    }

    private long personCount(Path file) {
        try (var connection = java.sql.DriverManager.getConnection("jdbc:sqlite:" + file);
                Statement statement = connection.createStatement();
                var rs = statement.executeQuery("SELECT count(*) FROM person")) {
            return rs.next() ? rs.getLong(1) : -1;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void anUnencryptedArchiveRoundTripsAndCarriesItsFingerprint() {
        BackupEngine.Archive archive = engine.create("BKP-1.sirmax", Optional.empty());

        assertThat(Path.of(archive.storagePath())).exists();
        assertThat(archive.compressed()).isTrue();
        assertThat(archive.encrypted()).isFalse();
        assertThat(archive.sha256()).hasSize(64);
        assertThat(archive.schemaVersion()).isGreaterThan(0);
        assertThat(archive.rowCounts()).containsEntry("person", 1L);
        assertThat(engine.validate(archive.storagePath(), archive.sha256())).isTrue();
    }

    @Test
    void anEncryptedArchiveIsUnreadableWithoutThePassphrase() throws IOException {
        BackupEngine.Archive archive = engine.create("BKP-2.sirmax", Optional.of(PASSPHRASE));

        assertThat(archive.encrypted()).isTrue();
        // The database's own header would be plainly visible in an unencrypted archive.
        String raw = Files.readString(Path.of(archive.storagePath()), StandardCharsets.ISO_8859_1);
        assertThat(raw).doesNotContain("SQLite format 3");
        assertThat(raw).doesNotContain("Rodríguez");

        assertThatThrownBy(() -> engine.restore(archive.storagePath(), Optional.empty()))
                .isInstanceOf(SirmaxException.class)
                .hasMessageContaining("passphrase");
    }

    @Test
    void aWrongPassphraseFailsLoudlyRatherThanRestoringRubbish() {
        BackupEngine.Archive archive = engine.create("BKP-3.sirmax", Optional.of(PASSPHRASE));

        assertThatThrownBy(
                        () ->
                                engine.restore(
                                        archive.storagePath(),
                                        Optional.of("otra-frase-distinta".toCharArray())))
                .isInstanceOf(SirmaxException.class)
                .hasMessageContaining("wrong passphrase");

        // The live database is untouched: nothing was overwritten before the failure.
        assertThat(personCount(databaseFile)).isEqualTo(1);
    }

    @Test
    void aTamperedArchiveFailsValidationAndIsRefused() throws IOException {
        BackupEngine.Archive archive = engine.create("BKP-4.sirmax", Optional.of(PASSPHRASE));
        Path file = Path.of(archive.storagePath());

        byte[] bytes = Files.readAllBytes(file);
        bytes[bytes.length - 20] ^= 0x5A; // flip bits inside the ciphertext
        Files.write(file, bytes);

        assertThat(engine.validate(archive.storagePath(), archive.sha256())).isFalse();
        assertThatThrownBy(() -> engine.restore(archive.storagePath(), Optional.of(PASSPHRASE)))
                .isInstanceOf(SirmaxException.class)
                .hasMessageContaining("altered");
    }

    @Test
    void restoringBringsBackTheStateTheSnapshotCaptured() {
        BackupEngine.Archive archive = engine.create("BKP-5.sirmax", Optional.of(PASSPHRASE));

        // Work happens after the backup and is expected to be lost by the restore.
        savePerson("p-2", "Pedro", "Martínez");
        savePerson("p-3", "Luisa", "Fernández");
        assertThat(personCount(databaseFile)).isEqualTo(3);

        engine.restore(archive.storagePath(), Optional.of(PASSPHRASE));

        assertThat(personCount(databaseFile)).isEqualTo(1);
    }

    @Test
    void aFileThatIsNotASirmaxBackupIsRejectedBeforeAnythingIsOverwritten() throws IOException {
        Path impostor = backupsDir.resolve("vacaciones.jpg");
        Files.writeString(impostor, "definitely not a database");

        assertThatThrownBy(() -> engine.restore(impostor.toString(), Optional.empty()))
                .isInstanceOf(SirmaxException.class)
                .hasMessageContaining("not a SIRMAX backup");
        assertThat(personCount(databaseFile)).isEqualTo(1);
    }

    @Test
    void validationNoticesAMissingArchive() {
        BackupEngine.Archive archive = engine.create("BKP-6.sirmax", Optional.empty());
        engine.deleteArchive(archive.storagePath());

        assertThat(engine.validate(archive.storagePath(), archive.sha256())).isFalse();
    }

    @Test
    void compressionActuallyShrinksTheArchive() throws IOException {
        for (int i = 0; i < 200; i++) {
            savePerson("p-bulk-" + i, "Nombre" + i, "Apellido" + i);
        }
        BackupEngine.Archive archive = engine.create("BKP-7.sirmax", Optional.empty());

        assertThat(archive.sizeBytes()).isLessThan(Files.size(databaseFile));
    }
}
