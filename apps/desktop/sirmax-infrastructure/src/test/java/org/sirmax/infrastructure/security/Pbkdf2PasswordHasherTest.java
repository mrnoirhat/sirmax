// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sirmax.domain.security.PasswordHash;

class Pbkdf2PasswordHasherTest {

    // low iteration count keeps the test fast; production uses the default
    private final Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher(1_000);

    @Test
    void hashThenVerifyRoundTrips() {
        PasswordHash h = hasher.hash("correct horse battery".toCharArray());
        assertThat(h.algorithm()).isEqualTo("PBKDF2-HMAC-SHA256");
        assertThat(h.value()).startsWith("pbkdf2-sha256$1000$");
        assertThat(hasher.verify("correct horse battery".toCharArray(), h)).isTrue();
    }

    @Test
    void verifyRejectsTheWrongPassword() {
        PasswordHash h = hasher.hash("s3cret".toCharArray());
        assertThat(hasher.verify("S3cret".toCharArray(), h)).isFalse();
        assertThat(hasher.verify("".toCharArray(), h)).isFalse();
    }

    @Test
    void eachHashHasAFreshSalt() {
        PasswordHash a = hasher.hash("same".toCharArray());
        PasswordHash b = hasher.hash("same".toCharArray());
        assertThat(a.value()).isNotEqualTo(b.value());
        assertThat(hasher.verify("same".toCharArray(), a)).isTrue();
        assertThat(hasher.verify("same".toCharArray(), b)).isTrue();
    }

    @Test
    void malformedStoredHashVerifiesFalseRatherThanThrowing() {
        assertThat(hasher.verify("x".toCharArray(), new PasswordHash("PBKDF2-HMAC-SHA256", "junk")))
                .isFalse();
    }
}
