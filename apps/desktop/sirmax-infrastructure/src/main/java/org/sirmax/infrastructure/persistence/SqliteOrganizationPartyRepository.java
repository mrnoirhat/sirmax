// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.sirmax.infrastructure.persistence.JdbcHelper.instant;
import static org.sirmax.infrastructure.persistence.JdbcHelper.str;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.sirmax.application.port.OrganizationPartyRepository;
import org.sirmax.domain.common.ArchiveStatus;
import org.sirmax.domain.identity.Organization;
import org.sirmax.domain.identity.OrganizationKind;

public final class SqliteOrganizationPartyRepository implements OrganizationPartyRepository {

    private final SqliteDatabase db;

    public SqliteOrganizationPartyRepository(SqliteDatabase db) {
        this.db = db;
    }

    @Override
    public void save(Organization o) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO organization_party"
                    + " (id, legal_name, trade_name, kind, notes, archive_status, created_at,"
                    + "  updated_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT(id) DO UPDATE SET legal_name=excluded.legal_name,"
                    + " trade_name=excluded.trade_name, kind=excluded.kind, notes=excluded.notes,"
                    + " archive_status=excluded.archive_status, updated_at=excluded.updated_at",
                o.id(),
                o.legalName(),
                o.tradeName().orElse(null),
                o.kind().name(),
                o.notes().orElse(null),
                o.archiveStatus().name(),
                o.createdAt(),
                o.updatedAt());
    }

    @Override
    public Optional<Organization> findById(String id) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM organization_party WHERE id = ?",
                SqliteOrganizationPartyRepository::map,
                id);
    }

    @Override
    public List<Organization> search(String query, int limit, int offset) {
        String q = query == null ? "" : query.strip();
        if (q.isEmpty()) {
            return JdbcHelper.queryList(
                    db.connection(),
                    "SELECT * FROM organization_party ORDER BY created_at DESC LIMIT ? OFFSET ?",
                    SqliteOrganizationPartyRepository::map,
                    limit,
                    offset);
        }
        String like = "%" + q + "%";
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM organization_party WHERE legal_name LIKE ? OR trade_name LIKE ?"
                        + " ORDER BY legal_name LIMIT ? OFFSET ?",
                SqliteOrganizationPartyRepository::map,
                like,
                like,
                limit,
                offset);
    }

    @Override
    public long countSearch(String query) {
        String q = query == null ? "" : query.strip();
        if (q.isEmpty()) {
            return JdbcHelper.queryLong(db.connection(), "SELECT count(*) FROM organization_party");
        }
        String like = "%" + q + "%";
        return JdbcHelper.queryLong(
                db.connection(),
                "SELECT count(*) FROM organization_party WHERE legal_name LIKE ? OR trade_name LIKE ?",
                like,
                like);
    }

    private static Organization map(ResultSet rs) throws SQLException {
        return new Organization(
                rs.getString("id"),
                rs.getString("legal_name"),
                str(rs, "trade_name"),
                OrganizationKind.valueOf(rs.getString("kind")),
                str(rs, "notes"),
                ArchiveStatus.valueOf(rs.getString("archive_status")),
                instant(rs, "created_at"),
                instant(rs, "updated_at"));
    }
}
