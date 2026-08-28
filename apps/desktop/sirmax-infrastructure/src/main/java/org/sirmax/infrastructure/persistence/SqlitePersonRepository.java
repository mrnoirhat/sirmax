// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.sirmax.infrastructure.persistence.JdbcHelper.instant;
import static org.sirmax.infrastructure.persistence.JdbcHelper.str;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.sirmax.application.port.PersonRepository;
import org.sirmax.domain.common.ArchiveStatus;
import org.sirmax.domain.identity.Person;
import org.sirmax.domain.identity.PersonName;
import org.sirmax.domain.identity.Sex;

public final class SqlitePersonRepository implements PersonRepository {

    private final SqliteDatabase db;

    public SqlitePersonRepository(SqliteDatabase db) {
        this.db = db;
    }

    @Override
    public void save(Person p) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO person"
                    + " (id, given_names, family_names, full_name, birth_date, sex, notes,"
                    + "  archive_status, created_at, updated_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT(id) DO UPDATE SET given_names=excluded.given_names,"
                    + " family_names=excluded.family_names, full_name=excluded.full_name,"
                    + " birth_date=excluded.birth_date, sex=excluded.sex, notes=excluded.notes,"
                    + " archive_status=excluded.archive_status, updated_at=excluded.updated_at",
                p.id(),
                p.name().givenNames(),
                p.name().familyNames(),
                p.fullName(),
                p.birthDate().map(LocalDate::toString).orElse(null),
                p.sex().map(Enum::name).orElse(null),
                p.notes().orElse(null),
                p.archiveStatus().name(),
                p.createdAt(),
                p.updatedAt());
    }

    @Override
    public Optional<Person> findById(String id) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM person WHERE id = ?",
                SqlitePersonRepository::mapPerson,
                id);
    }

    @Override
    public List<Person> search(String query, int limit, int offset) {
        String q = query == null ? "" : query.strip();
        if (q.isEmpty()) {
            return JdbcHelper.queryList(
                    db.connection(),
                    "SELECT * FROM person ORDER BY created_at DESC LIMIT ? OFFSET ?",
                    SqlitePersonRepository::mapPerson,
                    limit,
                    offset);
        }
        String like = "%" + q + "%";
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM person WHERE full_name LIKE ? OR family_names LIKE ?"
                        + " ORDER BY full_name LIMIT ? OFFSET ?",
                SqlitePersonRepository::mapPerson,
                like,
                like,
                limit,
                offset);
    }

    @Override
    public long countSearch(String query) {
        String q = query == null ? "" : query.strip();
        if (q.isEmpty()) {
            return JdbcHelper.queryLong(db.connection(), "SELECT count(*) FROM person");
        }
        String like = "%" + q + "%";
        return JdbcHelper.queryLong(
                db.connection(),
                "SELECT count(*) FROM person WHERE full_name LIKE ? OR family_names LIKE ?",
                like,
                like);
    }

    private static Person mapPerson(ResultSet rs) throws SQLException {
        String birth = str(rs, "birth_date");
        String sex = str(rs, "sex");
        return new Person(
                rs.getString("id"),
                new PersonName(rs.getString("given_names"), rs.getString("family_names")),
                birth == null ? null : LocalDate.parse(birth),
                sex == null ? null : Sex.valueOf(sex),
                str(rs, "notes"),
                ArchiveStatus.valueOf(rs.getString("archive_status")),
                instant(rs, "created_at"),
                instant(rs, "updated_at"));
    }
}
