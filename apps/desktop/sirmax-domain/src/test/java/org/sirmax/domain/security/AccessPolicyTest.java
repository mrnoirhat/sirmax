// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AccessPolicyTest {

    @Test
    void unionsThePermissionsOfEveryRole() {
        Role cashier =
                new Role(
                        "r1",
                        "CAJERA",
                        "",
                        true,
                        Set.of(Permission.INVOICE_ISSUE, Permission.PAYMENT_REGISTER));
        Role reader = new Role("r2", "LECTOR", "", true, Set.of(Permission.PERSON_READ));

        AccessPolicy policy = AccessPolicy.fromRoles(List.of(cashier, reader));

        assertThat(policy.allows(Permission.INVOICE_ISSUE)).isTrue();
        assertThat(policy.allows(Permission.PERSON_READ)).isTrue();
        assertThat(policy.allows(Permission.INVOICE_VOID)).isFalse();
    }

    @Test
    void requireThrowsAccessDeniedForAMissingPermission() {
        AccessPolicy policy = AccessPolicy.of(EnumSet.of(Permission.PERSON_READ));

        policy.require(Permission.PERSON_READ); // no throw

        assertThatThrownBy(() -> policy.require(Permission.INVOICE_VOID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("invoice.void");
    }

    @Test
    void noneAllowsNothing() {
        assertThat(AccessPolicy.none().allows(Permission.REPORT_VIEW)).isFalse();
        assertThat(AccessPolicy.none().permissions()).isEmpty();
    }

    @Test
    void everyPermissionKeyIsUniqueAndResolvable() {
        for (Permission p : Permission.values()) {
            assertThat(Permission.fromKey(p.key())).contains(p);
        }
        assertThat(Permission.fromKey("nope.nope")).isEmpty();
    }
}
