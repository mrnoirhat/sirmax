// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.util.List;
import java.util.Optional;
import org.sirmax.domain.identity.Organization;

/**
 * Persistence and search for {@link Organization} parties (businesses, community associations,
 * institutions). Distinct from {@link OrganizationRepository}, which is the ayuntamiento itself.
 */
public interface OrganizationPartyRepository {

    void save(Organization organization);

    Optional<Organization> findById(String id);

    List<Organization> search(String query, int limit, int offset);

    long countSearch(String query);
}
