// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import java.nio.file.Path;
import java.util.Optional;
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

    private final SqliteConnectionFactory factory;
    private final Path databaseFile; // null for an in-memory database
    private Connection connection;

    private SqliteDatabase(SqliteConnectionFactory factory, Path databaseFile) {
        this.factory = factory;
        this.databaseFile = databaseFile;
        this.connection = factory.open();
    }

    public static SqliteDatabase openInMemory() {
        return new SqliteDatabase(SqliteConnectionFactory.inMemory(), null);
    }

    public static SqliteDatabase openAt(Path databaseFile) {
        return new SqliteDatabase(new SqliteConnectionFactory(databaseFile), databaseFile);
    }

    public Connection connection() {
        return connection;
    }

    /** The file this database lives in, or empty for an in-memory one. */
    public Optional<Path> databaseFile() {
        return Optional.ofNullable(databaseFile);
    }

    /**
     * Close and re-open the connection against the same file.
     *
     * <p>Only a restore needs this: the file underneath has been replaced, so the old connection
     * refers to a database that no longer exists. Re-opening lets the restore write its own record
     * into the database it just produced — which is where that record belongs, because from now on
     * this database's history includes having been restored.
     *
     * @throws SirmaxException for an in-memory database, which cannot be reopened at all
     */
    public void reopen() {
        if (databaseFile == null) {
            throw new SirmaxException("An in-memory database cannot be reopened");
        }
        close();
        this.connection = factory.open();
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
            if (connection.isClosed()) {
                return;
            }
            connection.close();
        } catch (SQLException e) {
            throw new SirmaxException("Could not close the database connection", e);
        }
    }
}
