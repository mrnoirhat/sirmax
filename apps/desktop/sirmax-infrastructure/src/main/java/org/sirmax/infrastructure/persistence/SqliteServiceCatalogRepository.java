// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.sirmax.infrastructure.persistence.JdbcHelper.bool;
import static org.sirmax.infrastructure.persistence.JdbcHelper.instant;
import static org.sirmax.infrastructure.persistence.JdbcHelper.str;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.sirmax.application.port.ServiceCatalogRepository;
import org.sirmax.domain.common.ArchiveStatus;
import org.sirmax.domain.service.ServiceCategory;
import org.sirmax.domain.service.ServiceDefinition;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.domain.service.ServiceStatus;
import org.sirmax.domain.service.ServiceType;
import org.sirmax.shared.JsonDoc;

public final class SqliteServiceCatalogRepository implements ServiceCatalogRepository {

    private final SqliteDatabase db;

    public SqliteServiceCatalogRepository(SqliteDatabase db) {
        this.db = db;
    }

    // ── categories ──

    @Override
    public void saveCategory(ServiceCategory c) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO service_category (id, code, name, sort_order, archive_status, created_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT(id) DO UPDATE SET code=excluded.code, name=excluded.name,"
                    + " sort_order=excluded.sort_order, archive_status=excluded.archive_status",
                c.id(),
                c.code(),
                c.name(),
                c.sortOrder(),
                c.archiveStatus().name(),
                c.createdAt());
    }

    @Override
    public Optional<ServiceCategory> findCategoryById(String id) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM service_category WHERE id = ?",
                SqliteServiceCatalogRepository::mapCategory,
                id);
    }

    @Override
    public Optional<ServiceCategory> findCategoryByCode(String code) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM service_category WHERE lower(code) = lower(?)",
                SqliteServiceCatalogRepository::mapCategory,
                code);
    }

    @Override
    public List<ServiceCategory> listActiveCategories() {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM service_category WHERE archive_status = 'ACTIVE'"
                        + " ORDER BY sort_order, name",
                SqliteServiceCatalogRepository::mapCategory);
    }

    // ── definitions ──

    @Override
    public void saveDefinition(ServiceDefinition d) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO service_definition"
                    + " (id, code, category_id, name, description, service_type, department_id,"
                    + "  country_scope, municipal_override_allowed, current_version_id,"
                    + "  archive_status, created_at, updated_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT(id) DO UPDATE SET category_id=excluded.category_id,"
                    + " name=excluded.name, description=excluded.description,"
                    + " service_type=excluded.service_type, department_id=excluded.department_id,"
                    + " municipal_override_allowed=excluded.municipal_override_allowed,"
                    + " current_version_id=excluded.current_version_id,"
                    + " archive_status=excluded.archive_status, updated_at=excluded.updated_at",
                d.id(),
                d.code(),
                d.categoryId(),
                d.name(),
                d.description().orElse(null),
                d.serviceType().name(),
                d.departmentId().orElse(null),
                d.countryScope(),
                d.municipalOverrideAllowed() ? 1 : 0,
                d.currentVersionId().orElse(null),
                d.archiveStatus().name(),
                d.createdAt(),
                d.updatedAt());
    }

    @Override
    public Optional<ServiceDefinition> findDefinitionById(String id) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM service_definition WHERE id = ?",
                SqliteServiceCatalogRepository::mapDefinition,
                id);
    }

    @Override
    public Optional<ServiceDefinition> findDefinitionByCode(String code) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM service_definition WHERE lower(code) = lower(?)",
                SqliteServiceCatalogRepository::mapDefinition,
                code);
    }

    @Override
    public List<ServiceDefinition> listDefinitions(boolean includeArchived) {
        String sql =
                includeArchived
                        ? "SELECT * FROM service_definition ORDER BY name"
                        : "SELECT * FROM service_definition WHERE archive_status = 'ACTIVE'"
                                + " ORDER BY name";
        return JdbcHelper.queryList(
                db.connection(), sql, SqliteServiceCatalogRepository::mapDefinition);
    }

    // ── versions ──

    @Override
    public void saveVersion(ServiceDefinitionVersion v) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO service_definition_version"
                    + " (id, service_definition_id, version_number, status, requires_payment,"
                    + "  numbering_sequence_code, notes, requirements_json, form_schema_json,"
                    + "  workflow_json, fee_rules_json, output_documents_json, authorization_json,"
                    + "  sla_json, validity_json, created_at, published_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT(id) DO UPDATE SET status=excluded.status,"
                    + " requires_payment=excluded.requires_payment,"
                    + " numbering_sequence_code=excluded.numbering_sequence_code,"
                    + " notes=excluded.notes, requirements_json=excluded.requirements_json,"
                    + " form_schema_json=excluded.form_schema_json,"
                    + " workflow_json=excluded.workflow_json, fee_rules_json=excluded.fee_rules_json,"
                    + " output_documents_json=excluded.output_documents_json,"
                    + " authorization_json=excluded.authorization_json, sla_json=excluded.sla_json,"
                    + " validity_json=excluded.validity_json, published_at=excluded.published_at",
                v.id(),
                v.serviceDefinitionId(),
                v.versionNumber(),
                v.status().name(),
                v.requiresPayment() ? 1 : 0,
                v.numberingSequenceCode().orElse(null),
                v.notes().orElse(null),
                ServiceJson.requirementsToJson(v.requirements()),
                v.formSchema().value(),
                v.workflow().value(),
                v.feeRules().value(),
                v.outputDocuments().value(),
                v.authorization().value(),
                ServiceJson.slaToJson(v.sla()),
                ServiceJson.validityToJson(v.validity()),
                v.createdAt(),
                v.publishedAt().map(Instant::toString).orElse(null));
    }

    @Override
    public Optional<ServiceDefinitionVersion> findVersionById(String id) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM service_definition_version WHERE id = ?",
                SqliteServiceCatalogRepository::mapVersion,
                id);
    }

    @Override
    public List<ServiceDefinitionVersion> listVersions(String serviceDefinitionId) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM service_definition_version WHERE service_definition_id = ?"
                        + " ORDER BY version_number",
                SqliteServiceCatalogRepository::mapVersion,
                serviceDefinitionId);
    }

    @Override
    public Optional<ServiceDefinitionVersion> findActiveVersion(String serviceDefinitionId) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM service_definition_version"
                        + " WHERE service_definition_id = ? AND status = 'ACTIVE'"
                        + " ORDER BY version_number DESC LIMIT 1",
                SqliteServiceCatalogRepository::mapVersion,
                serviceDefinitionId);
    }

    @Override
    public int nextVersionNumber(String serviceDefinitionId) {
        long max =
                JdbcHelper.queryLong(
                        db.connection(),
                        "SELECT coalesce(max(version_number), 0) FROM service_definition_version"
                                + " WHERE service_definition_id = ?",
                        serviceDefinitionId);
        return (int) max + 1;
    }

    // ── row mappers ──

    private static ServiceCategory mapCategory(ResultSet rs) throws SQLException {
        return new ServiceCategory(
                rs.getString("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getInt("sort_order"),
                ArchiveStatus.valueOf(rs.getString("archive_status")),
                instant(rs, "created_at"));
    }

    private static ServiceDefinition mapDefinition(ResultSet rs) throws SQLException {
        return new ServiceDefinition(
                rs.getString("id"),
                rs.getString("code"),
                rs.getString("category_id"),
                rs.getString("name"),
                str(rs, "description"),
                ServiceType.valueOf(rs.getString("service_type")),
                str(rs, "department_id"),
                rs.getString("country_scope"),
                bool(rs, "municipal_override_allowed"),
                str(rs, "current_version_id"),
                ArchiveStatus.valueOf(rs.getString("archive_status")),
                instant(rs, "created_at"),
                instant(rs, "updated_at"));
    }

    private static ServiceDefinitionVersion mapVersion(ResultSet rs) throws SQLException {
        String publishedAt = str(rs, "published_at");
        return new ServiceDefinitionVersion(
                rs.getString("id"),
                rs.getString("service_definition_id"),
                rs.getInt("version_number"),
                ServiceStatus.valueOf(rs.getString("status")),
                bool(rs, "requires_payment"),
                str(rs, "numbering_sequence_code"),
                str(rs, "notes"),
                ServiceJson.requirementsFromJson(rs.getString("requirements_json")),
                ServiceJson.slaFromJson(rs.getString("sla_json")),
                ServiceJson.validityFromJson(rs.getString("validity_json")),
                JsonDoc.of(rs.getString("form_schema_json")),
                JsonDoc.of(rs.getString("workflow_json")),
                JsonDoc.of(rs.getString("fee_rules_json")),
                JsonDoc.of(rs.getString("output_documents_json")),
                JsonDoc.of(rs.getString("authorization_json")),
                instant(rs, "created_at"),
                publishedAt == null ? null : Instant.parse(publishedAt));
    }
}
