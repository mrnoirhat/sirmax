// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.util.List;
import java.util.Optional;
import org.sirmax.domain.org.Department;
import org.sirmax.domain.org.InstitutionProfile;
import org.sirmax.domain.org.OrganizationUnit;

/**
 * Persistence for the operating institution: the {@link OrganizationUnit}, its {@link
 * InstitutionProfile}, and its {@link Department}s.
 */
public interface OrganizationRepository {

    // ── organization unit ──
    void save(OrganizationUnit unit);

    Optional<OrganizationUnit> findById(String id);

    /** The single non-archived organization unit, if the install has been set up. */
    Optional<OrganizationUnit> findActive();

    // ── institution profile (1:1) ──
    void saveProfile(InstitutionProfile profile);

    Optional<InstitutionProfile> findProfile(String organizationUnitId);

    // ── departments ──
    void save(Department department);

    Optional<Department> findDepartmentById(String id);

    Optional<Department> findDepartmentByCode(String organizationUnitId, String code);

    List<Department> listActiveDepartments(String organizationUnitId);
}
