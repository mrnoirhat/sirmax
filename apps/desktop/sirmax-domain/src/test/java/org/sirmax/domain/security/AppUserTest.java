// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AppUserTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-01-02T00:00:00Z");
    private static final PasswordHash HASH = new PasswordHash("PBKDF2-HMAC-SHA256", "x$y$z");

    @Test
    void createStartsActiveWithNoLoginAndMatchingTimestamps() {
        AppUser u = AppUser.create("u1", "cajera1", "Cajera Uno", HASH, null, T0);
        assertThat(u.status()).isEqualTo(AppUserStatus.ACTIVE);
        assertThat(u.canSignIn()).isTrue();
        assertThat(u.lastLoginAt()).isEmpty();
        assertThat(u.createdAt()).isEqualTo(u.updatedAt());
        assertThat(u.departmentId()).isEmpty();
    }

    @Test
    void disabledAndLockedCannotSignIn() {
        AppUser u = AppUser.create("u1", "op1", "Operador", HASH, null, T0);
        u.changeStatus(AppUserStatus.DISABLED, T1);
        assertThat(u.canSignIn()).isFalse();
        u.changeStatus(AppUserStatus.LOCKED, T1);
        assertThat(u.canSignIn()).isFalse();
        u.changeStatus(AppUserStatus.ACTIVE, T1);
        assertThat(u.canSignIn()).isTrue();
    }

    @Test
    void mutationsBumpUpdatedAtAndRecordSignIn() {
        AppUser u = AppUser.create("u1", "op1", "Operador", HASH, null, T0);
        u.rename("Operador Editado", T1);
        assertThat(u.displayName()).isEqualTo("Operador Editado");
        assertThat(u.updatedAt()).isEqualTo(T1);

        u.recordSignIn(T1);
        assertThat(u.lastLoginAt()).contains(T1);
    }
}
