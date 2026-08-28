// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.util.List;
import java.util.Optional;
import org.sirmax.domain.identity.Person;

/** Persistence and search for {@link Person} master records. */
public interface PersonRepository {

    void save(Person person);

    Optional<Person> findById(String id);

    /**
     * Full-name / family-name search, paginated. {@code query} is matched loosely (case-insensitive,
     * contains); an empty query returns the most recent people.
     */
    List<Person> search(String query, int limit, int offset);

    long countSearch(String query);
}
