// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.sirmax.infrastructure.persistence.JdbcHelper.bool;
import static org.sirmax.infrastructure.persistence.JdbcHelper.instant;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.NumberingRepository;
import org.sirmax.domain.numbering.NumberingSequence;

/**
 * SQLite counters for document numbering (master prompt §27).
 *
 * <p>{@link #allocate} does read → mutate → conditional write in one statement pair guarded by the
 * counter's own value: the {@code UPDATE … WHERE next_value = ?} only lands if nobody else moved the
 * counter, and a losing writer retries. SQLite's single-writer model makes this cheap, and the
 * caller's transaction makes the number and the row it numbers commit or roll back together.
 */
public final class SqliteNumberingRepository implements NumberingRepository {

    /** Enough for any contention SQLite's single-writer lock can actually produce. */
    private static final int MAX_ATTEMPTS = 5;

    private final SqliteDatabase db;
    private final Clock clock;

    public SqliteNumberingRepository(SqliteDatabase db, Clock clock) {
        this.db = db;
        this.clock = clock;
    }

    @Override
    public String allocate(String sequenceCode, String defaultPrefix, int year) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            NumberingSequence sequence =
                    findByCode(sequenceCode)
                            .orElseGet(
                                    () -> {
                                        NumberingSequence created =
                                                NumberingSequence.create(
                                                        sequenceCode, defaultPrefix, clock.now());
                                        insert(created);
                                        return created;
                                    });

            long expected = sequence.nextValue();
            int expectedYear = sequence.periodYear();
            String code = sequence.allocate(year, clock.now());

            int updated =
                    JdbcHelper.update(
                            db.connection(),
                            "UPDATE numbering_sequence SET period_year = ?, next_value = ?,"
                                    + " updated_at = ?"
                                    + " WHERE code = ? AND next_value = ? AND period_year = ?",
                            sequence.periodYear(),
                            sequence.nextValue(),
                            sequence.updatedAt(),
                            sequenceCode,
                            expected,
                            expectedYear);
            if (updated == 1) {
                return code;
            }
        }
        throw new org.sirmax.shared.SirmaxException(
                "Could not allocate a number from sequence " + sequenceCode + " after "
                        + MAX_ATTEMPTS + " attempts");
    }

    @Override
    public Optional<NumberingSequence> findByCode(String sequenceCode) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM numbering_sequence WHERE code = ?",
                SqliteNumberingRepository::map,
                sequenceCode);
    }

    @Override
    public void save(NumberingSequence sequence) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO numbering_sequence"
                        + " (code, prefix, padding, yearly_reset, period_year, next_value, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?)"
                        + " ON CONFLICT(code) DO UPDATE SET prefix=excluded.prefix,"
                        + " padding=excluded.padding, yearly_reset=excluded.yearly_reset,"
                        + " period_year=excluded.period_year, next_value=excluded.next_value,"
                        + " updated_at=excluded.updated_at",
                sequence.code(),
                sequence.prefix(),
                sequence.padding(),
                sequence.yearlyReset(),
                sequence.periodYear(),
                sequence.nextValue(),
                sequence.updatedAt());
    }

    @Override
    public List<NumberingSequence> listAll() {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM numbering_sequence ORDER BY code",
                SqliteNumberingRepository::map);
    }

    /** Insert-if-absent, so a concurrent creator does not blow up the allocation. */
    private void insert(NumberingSequence sequence) {
        JdbcHelper.update(
                db.connection(),
                "INSERT OR IGNORE INTO numbering_sequence"
                        + " (code, prefix, padding, yearly_reset, period_year, next_value, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                sequence.code(),
                sequence.prefix(),
                sequence.padding(),
                sequence.yearlyReset(),
                sequence.periodYear(),
                sequence.nextValue(),
                sequence.updatedAt());
    }

    private static NumberingSequence map(ResultSet rs) throws SQLException {
        Instant updatedAt = instant(rs, "updated_at");
        return new NumberingSequence(
                rs.getString("code"),
                rs.getString("prefix"),
                rs.getInt("padding"),
                bool(rs, "yearly_reset"),
                rs.getInt("period_year"),
                rs.getLong("next_value"),
                updatedAt == null ? Instant.EPOCH : updatedAt);
    }
}
