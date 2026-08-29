// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.util.List;
import java.util.Optional;
import org.sirmax.domain.numbering.NumberingSequence;

/**
 * Persistence for document numbering counters (master prompt §27).
 *
 * <p>{@link #allocate} must read-modify-write the counter atomically. Callers run it inside the same
 * {@link UnitOfWork} as the row being numbered, so a rolled-back insert also rolls back the number.
 */
public interface NumberingRepository {

    /**
     * Hand out the next code for {@code sequenceCode} in {@code year}, creating the sequence with
     * {@code defaultPrefix} if it does not exist yet.
     */
    String allocate(String sequenceCode, String defaultPrefix, int year);

    Optional<NumberingSequence> findByCode(String sequenceCode);

    void save(NumberingSequence sequence);

    List<NumberingSequence> listAll();
}
