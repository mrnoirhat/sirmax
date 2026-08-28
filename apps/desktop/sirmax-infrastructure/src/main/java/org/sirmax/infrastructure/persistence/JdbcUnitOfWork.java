// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.shared.SirmaxException;

/**
 * Runs a block of work inside one SQLite transaction on the shared {@link SqliteDatabase}
 * connection.
 *
 * <p>Re-entrant: a use case that calls another already-transactional use case just joins the
 * outer transaction (no nested savepoints in Phase 3).
 */
public final class JdbcUnitOfWork implements UnitOfWork {

    private final SqliteDatabase database;
    private int depth = 0;

    public JdbcUnitOfWork(SqliteDatabase database) {
        this.database = database;
    }

    @Override
    public <T> T execute(Supplier<T> work) {
        if (depth > 0) {
            return work.get();
        }
        Connection c = database.connection();
        try {
            c.setAutoCommit(false);
            depth++;
            T result = work.get();
            c.commit();
            return result;
        } catch (RuntimeException e) {
            rollbackQuietly(c);
            throw e;
        } catch (SQLException e) {
            rollbackQuietly(c);
            throw new SirmaxException("Transaction failed", e);
        } finally {
            depth--;
            if (depth == 0) {
                try {
                    c.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // best effort
                }
            }
        }
    }

    private static void rollbackQuietly(Connection c) {
        try {
            c.rollback();
        } catch (SQLException ignored) {
            // best effort
        }
    }
}
