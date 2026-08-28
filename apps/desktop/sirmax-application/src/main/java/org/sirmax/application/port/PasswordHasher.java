// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import org.sirmax.domain.security.PasswordHash;

/**
 * Hashes and verifies operator passwords.
 *
 * <p>Plaintext is passed as {@code char[]} so the caller can wipe it after use. The concrete
 * algorithm (PBKDF2 today, Argon2 later — see {@code docs/adr/0014-password-hashing.md}) is entirely
 * the adapter's concern; the stored {@link PasswordHash} records which one produced it.
 */
public interface PasswordHasher {

    PasswordHash hash(char[] plaintext);

    boolean verify(char[] plaintext, PasswordHash stored);
}
