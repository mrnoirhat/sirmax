// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.sirmax.infrastructure.persistence.JdbcHelper.bool;
import static org.sirmax.infrastructure.persistence.JdbcHelper.instant;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.sirmax.application.port.IdentificationRepository;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.common.PartyType;
import org.sirmax.domain.identity.Identification;
import org.sirmax.domain.identity.IdentificationType;

public final class SqliteIdentificationRepository implements IdentificationRepository {

    private final SqliteDatabase db;

    public SqliteIdentificationRepository(SqliteDatabase db) {
        this.db = db;
    }

    @Override
    public void save(Identification i) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO identification"
                    + " (id, party_type, party_id, id_type, id_number, is_primary, created_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT(id) DO UPDATE SET id_type=excluded.id_type,"
                    + " id_number=excluded.id_number, is_primary=excluded.is_primary",
                i.id(),
                i.owner().type().name(),
                i.owner().id(),
                i.type().name(),
                i.number(),
                i.primary() ? 1 : 0,
                i.createdAt());
    }

    @Override
    public List<Identification> forOwner(PartyRef owner) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM identification WHERE party_type = ? AND party_id = ?"
                        + " ORDER BY is_primary DESC, created_at",
                SqliteIdentificationRepository::map,
                owner.type().name(),
                owner.id());
    }

    @Override
    public Optional<Identification> findByNumber(IdentificationType type, String number) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM identification WHERE id_type = ? AND id_number = ? LIMIT 1",
                SqliteIdentificationRepository::map,
                type.name(),
                number);
    }

    private static Identification map(ResultSet rs) throws SQLException {
        PartyRef owner =
                new PartyRef(
                        PartyType.valueOf(rs.getString("party_type")), rs.getString("party_id"));
        return new Identification(
                rs.getString("id"),
                owner,
                IdentificationType.valueOf(rs.getString("id_type")),
                rs.getString("id_number"),
                bool(rs, "is_primary"),
                instant(rs, "created_at"));
    }
}
