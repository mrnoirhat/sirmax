// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sirmax.shared.SirmaxException;

/** Small JDBC helpers so the SQLite repositories stay readable. */
final class JdbcHelper {

    /** Maps one {@link ResultSet} row to a value. */
    @FunctionalInterface
    interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private JdbcHelper() {}

    static int update(Connection c, String sql, Object... params) {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new SirmaxException("update failed: " + sql, e);
        }
    }

    static <T> Optional<T> queryOne(Connection c, String sql, RowMapper<T> mapper, Object... params) {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(mapper.map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new SirmaxException("query failed: " + sql, e);
        }
    }

    static <T> List<T> queryList(Connection c, String sql, RowMapper<T> mapper, Object... params) {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapper.map(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new SirmaxException("query failed: " + sql, e);
        }
    }

    static long queryLong(Connection c, String sql, Object... params) {
        return queryOne(c, sql, rs -> rs.getLong(1), params).orElse(0L);
    }

    /** Nullable string column. */
    static String str(ResultSet rs, String col) throws SQLException {
        String v = rs.getString(col);
        return rs.wasNull() ? null : v;
    }

    static Instant instant(ResultSet rs, String col) throws SQLException {
        String v = rs.getString(col);
        return (v == null) ? null : Instant.parse(v);
    }

    static boolean bool(ResultSet rs, String col) throws SQLException {
        return rs.getInt(col) != 0;
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            int idx = i + 1;
            Object p = params[i];
            switch (p) {
                case null -> ps.setNull(idx, Types.VARCHAR);
                case String s -> ps.setString(idx, s);
                case Integer n -> ps.setInt(idx, n);
                case Long n -> ps.setLong(idx, n);
                case Double d -> ps.setDouble(idx, d);
                case Boolean b -> ps.setInt(idx, b ? 1 : 0);
                case Instant t -> ps.setString(idx, t.toString());
                case Enum<?> e -> ps.setString(idx, e.name());
                default -> ps.setString(idx, p.toString());
            }
        }
    }
}
