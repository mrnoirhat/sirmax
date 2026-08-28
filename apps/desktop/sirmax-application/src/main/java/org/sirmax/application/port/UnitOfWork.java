// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.util.function.Supplier;

/**
 * Runs a block of work inside a single database transaction.
 *
 * <p>Use cases that touch more than one repository wrap their mutation in {@link #execute} so it
 * commits atomically or rolls back as a whole. The SQLite adapter maps this to a JDBC transaction
 * (foreign keys on, one writer).
 */
public interface UnitOfWork {

    <T> T execute(Supplier<T> work);

    default void execute(Runnable work) {
        execute(
                () -> {
                    work.run();
                    return null;
                });
    }
}
