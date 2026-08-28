// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.identity;

import java.time.Instant;
import java.util.Objects;
import org.sirmax.domain.common.PartyRef;

/** A way to reach a party. */
public record ContactPoint(
        String id,
        PartyRef owner,
        Kind kind,
        String value,
        boolean primary,
        Instant createdAt) {

    public enum Kind {
        PHONE,
        MOBILE,
        EMAIL,
        WHATSAPP,
        OTHER
    }

    public ContactPoint {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(createdAt, "createdAt");
        value = value.strip();
        if (id.isBlank() || value.isEmpty()) {
            throw new IllegalArgumentException("id and value must not be blank");
        }
    }

    public static ContactPoint of(
            String id, PartyRef owner, Kind kind, String value, Instant now) {
        return new ContactPoint(id, owner, kind, value, false, now);
    }
}
