// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.util.Map;
import java.util.Optional;

/**
 * Produces and restores database archives (master prompt §41).
 *
 * <p>The pipeline §41 prescribes — snapshot, validate, compress, encrypt, hash — is the engine's
 * responsibility because every step of it is infrastructure: SQLite's snapshot mechanism, gzip, the
 * cipher, the digest. The use case above orchestrates and audits; it never touches bytes.
 */
public interface BackupEngine {

    /**
     * What a completed archive turned out to be.
     *
     * @param sha256 of the file as written, after compression and encryption
     * @param rowCounts a coarse fingerprint of the snapshot's contents, kept as readable metadata
     */
    record Archive(
            String storagePath,
            long sizeBytes,
            String sha256,
            boolean compressed,
            boolean encrypted,
            int schemaVersion,
            Map<String, Long> rowCounts) {}

    /**
     * Take a consistent snapshot of the live database and write it as an archive.
     *
     * @param passphrase encrypts the archive; empty writes it unencrypted
     */
    Archive create(String fileName, Optional<char[]> passphrase);

    /**
     * Re-read an archive and check it against {@code expectedSha256}.
     *
     * @return {@code true} when the bytes on disk are still the ones SIRMAX wrote
     */
    boolean validate(String storagePath, String expectedSha256);

    /**
     * Replace the live database with the contents of an archive (§42 step 4).
     *
     * <p>The caller has already taken an emergency backup and confirmed with the operator. This does
     * the destructive part and runs SQLite's own integrity check afterwards (§42 steps 5–6).
     *
     * @throws org.sirmax.shared.SirmaxException if the archive cannot be read or decrypted, before
     *     anything is overwritten
     */
    void restore(String storagePath, Optional<char[]> passphrase);

    /** Delete an archive file from local storage; the {@code BackupRecord} row is kept. */
    void deleteArchive(String storagePath);
}
