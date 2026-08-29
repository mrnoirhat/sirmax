// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * The installation's security settings (master prompt §43).
 *
 * <p>The defaults are deliberately mild. A counter PC that locks every five minutes gets its policy
 * switched off entirely by the office, which leaves the municipality worse off than a longer timeout
 * somebody actually keeps. Security that gets disabled is not security.
 *
 * @param minPasswordLength shortest password accepted when one is set or changed
 * @param maxFailedAttempts consecutive failures before the account locks
 * @param lockoutMinutes how long a lock lasts; it expires rather than needing an administrator,
 *     because a municipality with one administrator who is on holiday still has to open the office
 * @param idleLockMinutes unattended time before the screen locks
 * @param sessionMaxHours absolute session lifetime, regardless of activity
 * @param maxAttachmentMb largest file that may be attached to a case
 */
public record SecurityPolicy(
        int minPasswordLength,
        int maxFailedAttempts,
        int lockoutMinutes,
        int idleLockMinutes,
        int sessionMaxHours,
        int maxAttachmentMb,
        Instant updatedAt) {

    public SecurityPolicy {
        Objects.requireNonNull(updatedAt, "updatedAt");
        requireAtLeast(minPasswordLength, 8, "minPasswordLength");
        requireAtLeast(maxFailedAttempts, 3, "maxFailedAttempts");
        requireAtLeast(lockoutMinutes, 1, "lockoutMinutes");
        requireAtLeast(idleLockMinutes, 1, "idleLockMinutes");
        requireAtLeast(sessionMaxHours, 1, "sessionMaxHours");
        requireAtLeast(maxAttachmentMb, 1, "maxAttachmentMb");
    }

    public static SecurityPolicy defaults(Instant now) {
        return new SecurityPolicy(12, 5, 15, 20, 12, 25, now);
    }

    public Duration lockout() {
        return Duration.ofMinutes(lockoutMinutes);
    }

    public Duration idleLock() {
        return Duration.ofMinutes(idleLockMinutes);
    }

    public Duration sessionLifetime() {
        return Duration.ofHours(sessionMaxHours);
    }

    public long maxAttachmentBytes() {
        return (long) maxAttachmentMb * 1024L * 1024L;
    }

    /**
     * Why a password is unacceptable, or empty when it is fine.
     *
     * <p>Length plus a check against the passwords people actually pick. Composition rules —
     * "one uppercase, one digit, one symbol" — reliably produce {@code Password1!} and are not
     * applied here for that reason.
     */
    public java.util.Optional<String> rejectPassword(char[] password) {
        if (password == null || password.length < minPasswordLength) {
            return java.util.Optional.of("security.password_too_short");
        }
        String value = new String(password).toLowerCase(java.util.Locale.ROOT);
        if (COMMON_PASSWORDS.contains(value)) {
            return java.util.Optional.of("security.password_too_common");
        }
        if (value.chars().distinct().count() < 5) {
            return java.util.Optional.of("security.password_too_repetitive");
        }
        return java.util.Optional.empty();
    }

    /**
     * The handful of passwords that would otherwise be chosen for a municipal admin account. Not a
     * breach corpus — shipping one would bloat the installer for little gain — but the Spanish and
     * keyboard-walk patterns a real office reaches for first.
     */
    private static final java.util.Set<String> COMMON_PASSWORDS =
            java.util.Set.of(
                    "contrasena123",
                    "contraseña123",
                    "administrador",
                    "ayuntamiento",
                    "password1234",
                    "123456789012",
                    "qwertyuiopas",
                    "sirmax123456",
                    "municipalidad");

    private static void requireAtLeast(int value, int minimum, String field) {
        if (value < minimum) {
            throw new IllegalArgumentException(field + " must be at least " + minimum);
        }
    }
}
