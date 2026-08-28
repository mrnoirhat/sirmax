// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.security;

import java.util.Objects;

/**
 * A stored password hash together with the algorithm that produced it.
 *
 * <p>The domain never sees plaintext passwords; hashing/verification is a port implemented by the
 * infrastructure layer. Keeping the algorithm label alongside the hash lets the hash format evolve
 * (e.g. PBKDF2 → Argon2) without breaking existing accounts.
 *
 * @param algorithm e.g. {@code "PBKDF2-HMAC-SHA256"}
 * @param value the encoded hash string (salt + parameters + digest, format is the hasher's concern)
 */
public record PasswordHash(String algorithm, String value) {

    public PasswordHash {
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(value, "value");
        if (algorithm.isBlank() || value.isBlank()) {
            throw new IllegalArgumentException("PasswordHash fields must not be blank");
        }
    }
}
