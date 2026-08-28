// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import org.sirmax.shared.SirmaxException;

/**
 * Owns the single JDBC {@link Connection} to the local SQLite database for the app's lifetime.
 *
 * <p>SQLite is single-writer and the desktop client is effectively single-user, so one long-lived
 * connection is the right model (and lets the in-memory database survive across calls in tests).
 * Repositories and the {@link JdbcUnitOfWork} share this connection.
 */
public final class SqliteDatabase implements AutoCloseable {

    private final Connection connection;

    private SqliteDatabase(Connection connection) {
        this.connection = connection;
    }

    public static SqliteDatabase openInMemory() {
        return new SqliteDatabase(SqliteConnectionFactory.inMemory().open());
    }

    public static SqliteDatabase openAt(Path databaseFile) {
        return new SqliteDatabase(new SqliteConnectionFactory(databaseFile).open());
    }

    public Connection connection() {
        return connection;
    }

    /** Apply all pending schema migrations; safe to call on every start-up. */
    public void migrate() {
        new MigrationRunner().migrate(connection);
    }

    /** Verify applied migrations still match their files without changing anything. */
    public void validateSchema() {
        new MigrationRunner().validate(connection);
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new SirmaxException("Could not close the database connection", e);
        }
    }
}
