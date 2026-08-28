// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.sirmax.infrastructure.persistence.JdbcHelper.instant;
import static org.sirmax.infrastructure.persistence.JdbcHelper.str;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.sirmax.application.port.OrganizationRepository;
import org.sirmax.domain.common.ArchiveStatus;
import org.sirmax.domain.org.Department;
import org.sirmax.domain.org.InstitutionProfile;
import org.sirmax.domain.org.OrganizationUnit;

public final class SqliteOrganizationRepository implements OrganizationRepository {

    private final SqliteDatabase db;

    public SqliteOrganizationRepository(SqliteDatabase db) {
        this.db = db;
    }

    // ── organization unit ──

    @Override
    public void save(OrganizationUnit u) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO organization_unit"
                    + " (id, name, short_name, municipality, province, country, archive_status,"
                    + "  created_at, updated_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT(id) DO UPDATE SET name=excluded.name,"
                    + " short_name=excluded.short_name, municipality=excluded.municipality,"
                    + " province=excluded.province, country=excluded.country,"
                    + " archive_status=excluded.archive_status, updated_at=excluded.updated_at",
                u.id(),
                u.name(),
                u.shortName().orElse(null),
                u.municipality(),
                u.province().orElse(null),
                u.country(),
                u.archiveStatus().name(),
                u.createdAt(),
                u.updatedAt());
    }

    @Override
    public Optional<OrganizationUnit> findById(String id) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM organization_unit WHERE id = ?",
                SqliteOrganizationRepository::mapUnit,
                id);
    }

    @Override
    public Optional<OrganizationUnit> findActive() {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM organization_unit WHERE archive_status = 'ACTIVE'"
                        + " ORDER BY created_at LIMIT 1",
                SqliteOrganizationRepository::mapUnit);
    }

    // ── institution profile ──

    @Override
    public void saveProfile(InstitutionProfile p) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO institution_profile"
                    + " (organization_unit_id, legal_identifier, address, phone, email, website,"
                    + "  logo_path, secondary_logo_path, color_primary, color_secondary,"
                    + "  color_accent, color_text, color_background, invoice_footer, document_header,"
                    + "  verification_json, updated_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '{}', ?)"
                    + " ON CONFLICT(organization_unit_id) DO UPDATE SET"
                    + " legal_identifier=excluded.legal_identifier, address=excluded.address,"
                    + " phone=excluded.phone, email=excluded.email, website=excluded.website,"
                    + " logo_path=excluded.logo_path,"
                    + " secondary_logo_path=excluded.secondary_logo_path,"
                    + " color_primary=excluded.color_primary,"
                    + " color_secondary=excluded.color_secondary,"
                    + " color_accent=excluded.color_accent, color_text=excluded.color_text,"
                    + " color_background=excluded.color_background,"
                    + " invoice_footer=excluded.invoice_footer,"
                    + " document_header=excluded.document_header, updated_at=excluded.updated_at",
                p.organizationUnitId(),
                p.legalIdentifier().orElse(null),
                p.address().orElse(null),
                p.phone().orElse(null),
                p.email().orElse(null),
                p.website().orElse(null),
                p.logoPath().orElse(null),
                p.secondaryLogoPath().orElse(null),
                p.colorPrimary().orElse(null),
                p.colorSecondary().orElse(null),
                p.colorAccent().orElse(null),
                p.colorText().orElse(null),
                p.colorBackground().orElse(null),
                p.invoiceFooter().orElse(null),
                p.documentHeader().orElse(null),
                java.time.Instant.now());
    }

    @Override
    public Optional<InstitutionProfile> findProfile(String organizationUnitId) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM institution_profile WHERE organization_unit_id = ?",
                SqliteOrganizationRepository::mapProfile,
                organizationUnitId);
    }

    // ── departments ──

    @Override
    public void save(Department d) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO department"
                    + " (id, organization_unit_id, name, code, archive_status, created_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT(id) DO UPDATE SET name=excluded.name, code=excluded.code,"
                    + " archive_status=excluded.archive_status",
                d.id(),
                d.organizationUnitId(),
                d.name(),
                d.code(),
                d.archiveStatus().name(),
                d.createdAt());
    }

    @Override
    public Optional<Department> findDepartmentById(String id) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM department WHERE id = ?",
                SqliteOrganizationRepository::mapDepartment,
                id);
    }

    @Override
    public Optional<Department> findDepartmentByCode(String organizationUnitId, String code) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM department WHERE organization_unit_id = ? AND code = ?",
                SqliteOrganizationRepository::mapDepartment,
                organizationUnitId,
                code.toUpperCase(java.util.Locale.ROOT));
    }

    @Override
    public List<Department> listActiveDepartments(String organizationUnitId) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM department WHERE organization_unit_id = ?"
                        + " AND archive_status = 'ACTIVE' ORDER BY name",
                SqliteOrganizationRepository::mapDepartment,
                organizationUnitId);
    }

    // ── row mappers ──

    private static OrganizationUnit mapUnit(ResultSet rs) throws SQLException {
        return new OrganizationUnit(
                rs.getString("id"),
                rs.getString("name"),
                str(rs, "short_name"),
                rs.getString("municipality"),
                str(rs, "province"),
                rs.getString("country"),
                ArchiveStatus.valueOf(rs.getString("archive_status")),
                instant(rs, "created_at"),
                instant(rs, "updated_at"));
    }

    private static Department mapDepartment(ResultSet rs) throws SQLException {
        return new Department(
                rs.getString("id"),
                rs.getString("organization_unit_id"),
                rs.getString("name"),
                rs.getString("code"),
                ArchiveStatus.valueOf(rs.getString("archive_status")),
                instant(rs, "created_at"));
    }

    private static InstitutionProfile mapProfile(ResultSet rs) throws SQLException {
        String orgId = rs.getString("organization_unit_id");
        InstitutionProfile.Overrides o = new InstitutionProfile.Overrides();
        o.legalIdentifier = str(rs, "legal_identifier");
        o.address = str(rs, "address");
        o.phone = str(rs, "phone");
        o.email = str(rs, "email");
        o.website = str(rs, "website");
        o.logoPath = str(rs, "logo_path");
        o.secondaryLogoPath = str(rs, "secondary_logo_path");
        o.colorPrimary = str(rs, "color_primary");
        o.colorSecondary = str(rs, "color_secondary");
        o.colorAccent = str(rs, "color_accent");
        o.colorText = str(rs, "color_text");
        o.colorBackground = str(rs, "color_background");
        o.invoiceFooter = str(rs, "invoice_footer");
        o.documentHeader = str(rs, "document_header");
        return InstitutionProfile.empty(orgId).with(o);
    }
}
