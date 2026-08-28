// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import org.sirmax.shared.SirmaxException;

/**
 * Opens JDBC connections to the local SQLite database file with SIRMAX's required pragmas.
 *
 * <p>Every connection is configured with:
 *
 * <ul>
 *   <li>{@code foreign_keys = ON} — referential integrity is mandatory ({@code DATABASE.md} §1);
 *   <li>{@code journal_mode = WAL} — concurrent readers alongside the single writer;
 *   <li>{@code synchronous = NORMAL} — the documented normal-operation durability trade-off;
 *   <li>{@code busy_timeout = 5000} — wait rather than fail immediately on a locked database.
 * </ul>
 *
 * <p>The connection pool / unit-of-work wiring is added in Phase 3.
 */
public final class SqliteConnectionFactory {

    private final String jdbcUrl;

    private SqliteConnectionFactory(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public SqliteConnectionFactory(Path databaseFile) {
        this("jdbc:sqlite:" + Objects.requireNonNull(databaseFile, "databaseFile").toAbsolutePath());
    }

    /** An in-memory database, for tests and throwaway bootstrapping. */
    public static SqliteConnectionFactory inMemory() {
        return new SqliteConnectionFactory("jdbc:sqlite::memory:");
    }

    public Connection open() {
        try {
            Connection connection = DriverManager.getConnection(jdbcUrl);
            applyPragmas(connection);
            return connection;
        } catch (SQLException e) {
            throw new SirmaxException("Could not open SQLite database at " + jdbcUrl, e);
        }
    }

    private static void applyPragmas(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA journal_mode = WAL");
            st.execute("PRAGMA synchronous = NORMAL");
            st.execute("PRAGMA busy_timeout = 5000");
        }
    }

    String jdbcUrl() {
        return jdbcUrl;
    }
}
