// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.org;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.sirmax.domain.common.ArchiveStatus;

class OrganizationDomainTest {

    private static final Instant NOW = Instant.parse("2026-01-02T03:04:05Z");

    @Test
    void organizationUnitNormalizesCountryToUppercaseAlpha2() {
        OrganizationUnit ou =
                OrganizationUnit.create("o1", "Ayuntamiento de Ejemplo", "Ejemplo", "do", NOW);
        assertThat(ou.country()).isEqualTo("DO");
        assertThat(ou.archiveStatus()).isEqualTo(ArchiveStatus.ACTIVE);
    }

    @Test
    void organizationUnitRejectsBadCountryCode() {
        assertThatThrownBy(
                        () ->
                                OrganizationUnit.create(
                                        "o1", "Ayuntamiento", "Ejemplo", "Dominican Republic", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alpha-2");
    }

    @Test
    void departmentCodeIsUppercasedAndValidated() {
        Department d = Department.create("d1", "o1", "Registro Civil", "reg-civ", NOW);
        assertThat(d.code()).isEqualTo("REG-CIV");
        assertThat(d.isActive()).isTrue();

        assertThatThrownBy(() -> Department.create("d2", "o1", "X", "has spaces", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void departmentArchiveAndRestore() {
        Department d = Department.create("d1", "o1", "Caja", "CAJA", NOW);
        d.archive();
        assertThat(d.isActive()).isFalse();
        assertThat(d.archiveStatus()).isEqualTo(ArchiveStatus.ARCHIVED);
        d.restore();
        assertThat(d.isActive()).isTrue();
    }

    @Test
    void institutionProfileWithLeavesUnspecifiedFieldsAndClearsOnBlank() {
        InstitutionProfile base = InstitutionProfile.empty("o1");

        InstitutionProfile.Overrides set = new InstitutionProfile.Overrides();
        set.phone = "809-000-0000";
        set.email = "  info@ayto.gob.do  ";
        InstitutionProfile p1 = base.with(set);
        assertThat(p1.phone()).contains("809-000-0000");
        assertThat(p1.email()).contains("info@ayto.gob.do");
        assertThat(p1.website()).isEmpty();

        InstitutionProfile.Overrides clearPhone = new InstitutionProfile.Overrides();
        clearPhone.phone = "";
        InstitutionProfile p2 = p1.with(clearPhone);
        assertThat(p2.phone()).isEmpty();
        assertThat(p2.email()).contains("info@ayto.gob.do"); // untouched
    }
}
