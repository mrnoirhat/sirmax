// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.common;

import java.util.Objects;

/**
 * A reference to a party (person or organization) by type and id.
 *
 * <p>Identifications, addresses and contact points attach to a {@code PartyRef} rather than to a
 * fixed table, so the same value objects serve people and organizations.
 */
public record PartyRef(PartyType type, String id) {

    public PartyRef {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("PartyRef id must not be blank");
        }
    }

    public static PartyRef person(String id) {
        return new PartyRef(PartyType.PERSON, id);
    }

    public static PartyRef organization(String id) {
        return new PartyRef(PartyType.ORGANIZATION, id);
    }
}
