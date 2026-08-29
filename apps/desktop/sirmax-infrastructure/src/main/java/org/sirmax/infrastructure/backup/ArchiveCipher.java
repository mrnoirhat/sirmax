// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.backup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.sirmax.shared.SirmaxException;

/**
 * AES-256-GCM for backup archives (master prompt §41, §43 — backup encryption).
 *
 * <p>GCM rather than CBC because it authenticates as well as encrypts: a tampered or truncated
 * archive fails to decrypt instead of restoring quietly-corrupted data over a municipality's live
 * database. That difference matters more here than anywhere else in SIRMAX.
 *
 * <p>Each archive gets a fresh random salt and nonce, written in the clear ahead of the ciphertext.
 * Reusing a nonce under one key is the one thing that breaks GCM outright, and per-archive
 * randomness removes the possibility by construction.
 *
 * <p>The key is derived with PBKDF2-HMAC-SHA256. The passphrase itself is never written anywhere —
 * which is exactly why the settings screen has to say, before encryption is switched on, that losing
 * it loses the backups.
 */
final class ArchiveCipher {

    private static final String KDF = "PBKDF2WithHmacSHA256";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    /** OWASP's floor for PBKDF2-HMAC-SHA256; a backup is decrypted rarely, so cost is affordable. */
    private static final int ITERATIONS = 600_000;

    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();

    /** Writes {@code salt · nonce · ciphertext} to {@code out}, which is left open for the caller. */
    void encrypt(Path plaintext, OutputStream out, char[] passphrase) throws IOException {
        byte[] salt = new byte[SALT_BYTES];
        byte[] nonce = new byte[NONCE_BYTES];
        random.nextBytes(salt);
        random.nextBytes(nonce);

        out.write(salt);
        out.write(nonce);

        Cipher cipher = init(Cipher.ENCRYPT_MODE, passphrase, salt, nonce);
        // CipherOutputStream must be closed to emit the GCM tag, but closing it would close the
        // caller's stream too; the shield keeps both invariants.
        try (OutputStream shielded = new NonClosingOutputStream(out);
                CipherOutputStream cipherOut = new CipherOutputStream(shielded, cipher);
                InputStream in = Files.newInputStream(plaintext)) {
            in.transferTo(cipherOut);
        }
    }

    /** Reads {@code salt · nonce · ciphertext} from {@code in} and writes the plaintext to a file. */
    void decrypt(InputStream in, Path plaintext, char[] passphrase) throws IOException {
        byte[] salt = in.readNBytes(SALT_BYTES);
        byte[] nonce = in.readNBytes(NONCE_BYTES);
        if (salt.length != SALT_BYTES || nonce.length != NONCE_BYTES) {
            throw new SirmaxException("The backup archive is truncated");
        }

        Cipher cipher = init(Cipher.DECRYPT_MODE, passphrase, salt, nonce);
        try (OutputStream out = Files.newOutputStream(plaintext)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                byte[] chunk = cipher.update(buffer, 0, read);
                if (chunk != null) {
                    out.write(chunk);
                }
            }
            // doFinal verifies the GCM tag: a wrong passphrase or a modified archive fails here,
            // which is the whole point of using an authenticated mode.
            out.write(cipher.doFinal());
        } catch (javax.crypto.AEADBadTagException e) {
            throw new SirmaxException(
                    "The backup could not be decrypted: wrong passphrase, or the archive has been"
                            + " altered",
                    e);
        } catch (GeneralSecurityException e) {
            throw new SirmaxException("The backup could not be decrypted", e);
        }
    }

    private Cipher init(int mode, char[] passphrase, byte[] salt, byte[] nonce) {
        try {
            SecretKey key =
                    new SecretKeySpec(
                            SecretKeyFactory.getInstance(KDF)
                                    .generateSecret(
                                            new PBEKeySpec(passphrase, salt, ITERATIONS, KEY_BITS))
                                    .getEncoded(),
                            "AES");
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, nonce));
            return cipher;
        } catch (GeneralSecurityException e) {
            throw new SirmaxException("Could not set up backup encryption", e);
        }
    }

    /** Lets a filter stream be closed without closing the stream underneath it. */
    private static final class NonClosingOutputStream extends java.io.FilterOutputStream {

        NonClosingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len); // FilterOutputStream would write byte by byte
        }

        @Override
        public void close() throws IOException {
            flush();
        }
    }
}
