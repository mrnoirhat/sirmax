// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.backup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.sirmax.application.port.BackupEngine;
import org.sirmax.infrastructure.persistence.SqliteDatabase;
import org.sirmax.shared.SirmaxException;

/**
 * The §41 backup pipeline against a live SQLite database.
 *
 * <pre>{@code
 * VACUUM INTO  →  row-count fingerprint  →  gzip  →  AES-GCM  →  SHA-256  →  disk
 * }</pre>
 *
 * <p>The snapshot uses SQLite's own {@code VACUUM INTO}, not a file copy. Copying the database file
 * while the application holds it open would capture a half-written page or miss the WAL entirely;
 * {@code VACUUM INTO} produces a consistent, compacted database from inside the engine, which is the
 * only correct way to snapshot a live SQLite file.
 *
 * <p>Encryption is AES-256-GCM with a key derived per archive by PBKDF2 from the municipality's
 * passphrase, and a random salt and nonce stored in the file header. GCM authenticates as well as
 * encrypts, so a tampered archive fails to decrypt rather than restoring quietly-corrupted data.
 *
 * <p><b>The passphrase is never stored.</b> Losing it loses the encrypted backups, and the UI has to
 * say so before anyone turns encryption on.
 */
public final class SqliteBackupEngine implements BackupEngine {

    /** File magic, so a wrong file is rejected before anything is decrypted or restored. */
    private static final byte[] MAGIC = {'S', 'M', 'X', 'B'};

    private static final int FORMAT_VERSION = 1;

    /** Tables whose row counts make a useful at-a-glance fingerprint of an archive. */
    private static final List<String> FINGERPRINT_TABLES =
            List.of(
                    "person",
                    "organization_party",
                    "service_definition",
                    "procedure",
                    "invoice",
                    "payment",
                    "issued_document",
                    "audit_event");

    private final SqliteDatabase database;
    private final Path backupsDirectory;
    private final Path databaseFile;
    private final ArchiveCipher cipher = new ArchiveCipher();

    public SqliteBackupEngine(SqliteDatabase database, Path backupsDirectory, Path databaseFile) {
        this.database = database;
        this.backupsDirectory = backupsDirectory;
        this.databaseFile = databaseFile;
    }

    @Override
    public Archive create(String fileName, Optional<char[]> passphrase) {
        boolean encrypt = passphrase.isPresent();
        Path target = backupsDirectory.resolve(fileName);
        Path snapshot = null;
        try {
            Files.createDirectories(backupsDirectory);
            snapshot = Files.createTempFile(backupsDirectory, "snapshot-", ".sqlite");
            // VACUUM INTO refuses to overwrite, so the temp file has to be gone first.
            Files.deleteIfExists(snapshot);

            snapshotInto(snapshot);
            Map<String, Long> rowCounts = fingerprint();
            int schemaVersion = schemaVersion();

            writeArchive(snapshot, target, passphrase);

            return new Archive(
                    target.toString(),
                    Files.size(target),
                    sha256Of(target),
                    true,
                    encrypt,
                    schemaVersion,
                    rowCounts);
        } catch (IOException e) {
            throw new SirmaxException("Could not write the backup archive", e);
        } finally {
            deleteQuietly(snapshot);
        }
    }

    @Override
    public boolean validate(String storagePath, String expectedSha256) {
        Path path = Path.of(storagePath);
        if (!Files.isRegularFile(path)) {
            return false;
        }
        return sha256Of(path).equalsIgnoreCase(expectedSha256);
    }

    @Override
    public void restore(String storagePath, Optional<char[]> passphrase) {
        Path archive = Path.of(storagePath);
        if (!Files.isRegularFile(archive)) {
            throw new SirmaxException("The backup archive is missing: " + storagePath);
        }

        Path staged = null;
        try {
            // Unpack and check the archive *before* touching the live database. A restore that
            // fails halfway through is worse than one that never starts (§42).
            staged = Files.createTempFile(backupsDirectory, "restore-", ".sqlite");
            readArchive(archive, staged, passphrase);
            verifyRestorable(staged);

            database.close();
            Files.move(
                    staged,
                    databaseFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            staged = null;

            // The WAL and shared-memory files belong to the database we just replaced; leaving
            // them would let SQLite reapply pages from the old database over the restored one.
            deleteQuietly(Path.of(databaseFile + "-wal"));
            deleteQuietly(Path.of(databaseFile + "-shm"));

            // Re-open against the restored file. The caller still has work to do — §42 step 7
            // records the restore — and that record belongs in the database it produced.
            database.reopen();
        } catch (IOException e) {
            throw new SirmaxException("Could not restore the backup", e);
        } finally {
            deleteQuietly(staged);
        }
    }

    @Override
    public void deleteArchive(String storagePath) {
        deleteQuietly(Path.of(storagePath));
    }

    // ── pipeline steps ──

    /** SQLite's own consistent snapshot; a file copy of a live database is not one. */
    private void snapshotInto(Path snapshot) {
        try (Statement statement = database.connection().createStatement()) {
            statement.execute("VACUUM INTO '" + snapshot.toString().replace("'", "''") + "'");
        } catch (SQLException e) {
            throw new SirmaxException("Could not snapshot the database", e);
        }
    }

    private Map<String, Long> fingerprint() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String table : FINGERPRINT_TABLES) {
            try (Statement statement = database.connection().createStatement();
                    ResultSet rs = statement.executeQuery("SELECT count(*) FROM " + table)) {
                counts.put(table, rs.next() ? rs.getLong(1) : 0L);
            } catch (SQLException e) {
                // A table a future migration renames must not break backups; the fingerprint is
                // informational, and an absent entry is more honest than a failed backup.
                counts.put(table, -1L);
            }
        }
        return counts;
    }

    private int schemaVersion() {
        try (Statement statement = database.connection().createStatement();
                ResultSet rs =
                        statement.executeQuery(
                                "SELECT coalesce(max(version), 0) FROM schema_migrations")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    /** header · gzip · optional AES-GCM. The header stays readable so the format is self-describing. */
    private void writeArchive(Path snapshot, Path target, Optional<char[]> passphrase)
            throws IOException {
        try (OutputStream file = Files.newOutputStream(target)) {
            file.write(MAGIC);
            file.write(FORMAT_VERSION);
            file.write(passphrase.isPresent() ? 1 : 0);

            if (passphrase.isEmpty()) {
                try (OutputStream gzip = new GZIPOutputStream(file);
                        InputStream in = Files.newInputStream(snapshot)) {
                    in.transferTo(gzip);
                }
                return;
            }
            // Compress before encrypting: ciphertext does not compress.
            Path compressed = Files.createTempFile(backupsDirectory, "gz-", ".tmp");
            try {
                try (OutputStream gzip = new GZIPOutputStream(Files.newOutputStream(compressed));
                        InputStream in = Files.newInputStream(snapshot)) {
                    in.transferTo(gzip);
                }
                cipher.encrypt(compressed, file, passphrase.get());
            } finally {
                deleteQuietly(compressed);
            }
        }
    }

    private void readArchive(Path archive, Path target, Optional<char[]> passphrase)
            throws IOException {
        try (InputStream file = Files.newInputStream(archive)) {
            byte[] magic = file.readNBytes(MAGIC.length);
            if (!java.util.Arrays.equals(magic, MAGIC)) {
                throw new SirmaxException("That file is not a SIRMAX backup");
            }
            int version = file.read();
            if (version != FORMAT_VERSION) {
                throw new SirmaxException(
                        "This backup was written by a newer version of SIRMAX (format " + version + ")");
            }
            boolean encrypted = file.read() == 1;
            if (encrypted && passphrase.isEmpty()) {
                throw new SirmaxException("This backup is encrypted and needs its passphrase");
            }

            if (!encrypted) {
                try (InputStream gzip = new GZIPInputStream(file);
                        OutputStream out = Files.newOutputStream(target)) {
                    gzip.transferTo(out);
                }
                return;
            }
            Path decrypted = Files.createTempFile(backupsDirectory, "dec-", ".tmp");
            try {
                cipher.decrypt(file, decrypted, passphrase.get());
                try (InputStream gzip = new GZIPInputStream(Files.newInputStream(decrypted));
                        OutputStream out = Files.newOutputStream(target)) {
                    gzip.transferTo(out);
                }
            } finally {
                deleteQuietly(decrypted);
            }
        }
    }

    /** SQLite's own integrity check on the unpacked file, before it replaces anything (§42 step 5). */
    private void verifyRestorable(Path candidate) {
        try (var connection =
                        java.sql.DriverManager.getConnection("jdbc:sqlite:" + candidate);
                Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery("PRAGMA integrity_check")) {
                String result = rs.next() ? rs.getString(1) : "unknown";
                if (!"ok".equalsIgnoreCase(result)) {
                    throw new SirmaxException("The backup failed SQLite's integrity check: " + result);
                }
            }
            try (ResultSet rs =
                    statement.executeQuery(
                            "SELECT count(*) FROM sqlite_master WHERE type = 'table'"
                                    + " AND name = 'schema_migrations'")) {
                if (!rs.next() || rs.getInt(1) == 0) {
                    throw new SirmaxException("That archive is not a SIRMAX database");
                }
            }
        } catch (SQLException e) {
            throw new SirmaxException("The backup could not be opened as a database", e);
        }
    }

    static String sha256Of(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new SirmaxException("Could not hash " + path, e);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // A leftover temp file is untidy, not a failure worth aborting a backup over.
        }
    }
}
