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
import org.sirmax.shared.text.Normalization;

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
                    + " (id, given_names, family_names, full_name, search_name, birth_date, sex,"
                    + "  notes, archive_status, created_at, updated_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT(id) DO UPDATE SET given_names=excluded.given_names,"
                    + " family_names=excluded.family_names, full_name=excluded.full_name,"
                    + " search_name=excluded.search_name,"
                    + " birth_date=excluded.birth_date, sex=excluded.sex, notes=excluded.notes,"
                    + " archive_status=excluded.archive_status, updated_at=excluded.updated_at",
                p.id(),
                p.name().givenNames(),
                p.name().familyNames(),
                p.fullName(),
                Normalization.fold(p.fullName()),
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
                    // Alphabetical, like the paper register it replaces. Ordering by creation
                    // date answers "who did we add last", which is not the question anyone has
                    // while looking at a list of citizens.
                    "SELECT * FROM person ORDER BY full_name LIMIT ? OFFSET ?",
                    SqlitePersonRepository::mapPerson,
                    limit,
                    offset);
        }
        // Match on the folded key so "Pena" finds "Peña" and "jose" finds "José".
        String like = "%" + Normalization.fold(q) + "%";
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM person WHERE search_name LIKE ?"
                        + " ORDER BY full_name LIMIT ? OFFSET ?",
                SqlitePersonRepository::mapPerson,
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
        String like = "%" + Normalization.fold(q) + "%";
        return JdbcHelper.queryLong(
                db.connection(),
                "SELECT count(*) FROM person WHERE search_name LIKE ?",
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
