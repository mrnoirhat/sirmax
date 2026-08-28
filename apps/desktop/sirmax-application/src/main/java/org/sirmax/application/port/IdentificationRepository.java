// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.util.List;
import java.util.Optional;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.identity.Identification;
import org.sirmax.domain.identity.IdentificationType;

/** Persistence for {@link Identification}s attached to a party. */
public interface IdentificationRepository {

    void save(Identification identification);

    List<Identification> forOwner(PartyRef owner);

    /** Look up by document type and number (used for duplicate detection). */
    Optional<Identification> findByNumber(IdentificationType type, String number);
}
