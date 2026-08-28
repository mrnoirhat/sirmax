// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.sirmax.application.port.PasswordHasher;
import org.sirmax.domain.security.PasswordHash;
import org.sirmax.shared.SirmaxException;

/**
 * PBKDF2-HMAC-SHA256 password hashing using only the JDK — no extra dependency to licence-review
 * (see {@code docs/adr/0014-password-hashing.md}). Argon2id is the intended upgrade in Phase 10;
 * the stored {@link PasswordHash} records the algorithm so existing accounts keep working.
 *
 * <p>Encoded form: {@code pbkdf2-sha256$<iterations>$<base64 salt>$<base64 dk>}.
 */
public final class Pbkdf2PasswordHasher implements PasswordHasher {

    public static final String ALGORITHM_LABEL = "PBKDF2-HMAC-SHA256";

    private static final String PREFIX = "pbkdf2-sha256";
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final int DEFAULT_ITERATIONS = 210_000;

    private final SecureRandom random = new SecureRandom();
    private final int iterations;

    public Pbkdf2PasswordHasher() {
        this(DEFAULT_ITERATIONS);
    }

    Pbkdf2PasswordHasher(int iterations) {
        this.iterations = iterations;
    }

    @Override
    public PasswordHash hash(char[] plaintext) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] dk = pbkdf2(plaintext, salt, iterations);
        Base64.Encoder b64 = Base64.getEncoder().withoutPadding();
        String encoded =
                PREFIX
                        + "$"
                        + iterations
                        + "$"
                        + b64.encodeToString(salt)
                        + "$"
                        + b64.encodeToString(dk);
        return new PasswordHash(ALGORITHM_LABEL, encoded);
    }

    @Override
    public boolean verify(char[] plaintext, PasswordHash stored) {
        String[] parts = stored.value().split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        int iters;
        byte[] salt;
        byte[] expected;
        try {
            iters = Integer.parseInt(parts[1]);
            Base64.Decoder b64 = Base64.getDecoder();
            salt = b64.decode(parts[2]);
            expected = b64.decode(parts[3]);
        } catch (RuntimeException e) {
            return false;
        }
        byte[] actual = pbkdf2(plaintext, salt, iters);
        return MessageDigest.isEqual(actual, expected);
    }

    private static byte[] pbkdf2(char[] plaintext, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(plaintext, salt, iterations, KEY_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (java.security.NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SirmaxException("PBKDF2 hashing unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }
}
