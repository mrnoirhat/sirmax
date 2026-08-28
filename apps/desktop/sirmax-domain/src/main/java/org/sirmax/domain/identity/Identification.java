// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.identity;

import java.time.Instant;
import java.util.Objects;
import org.sirmax.domain.common.PartyRef;

/**
 * An identifying document held by a party.
 *
 * <p>The {@code number} is stored as given (formatting differences are the caller's concern); the
 * {@code (type, number)} pair is indexed for lookup. Country-specific validation of the number
 * format belongs to the country adapter, not here (master prompt §1.1, §37).
 *
 * @param id identifier
 * @param owner the party this document belongs to
 * @param type kind of document
 * @param number the document number, non-blank
 * @param primary whether this is the party's primary identification
 * @param createdAt when it was recorded
 */
public record Identification(
        String id,
        PartyRef owner,
        IdentificationType type,
        String number,
        boolean primary,
        Instant createdAt) {

    public Identification {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(number, "number");
        Objects.requireNonNull(createdAt, "createdAt");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        number = number.strip();
        if (number.isEmpty()) {
            throw new IllegalArgumentException("number must not be blank");
        }
    }

    public static Identification of(
            String id, PartyRef owner, IdentificationType type, String number, Instant now) {
        return new Identification(id, owner, type, number, false, now);
    }

    public Identification asPrimary() {
        return primary ? this : new Identification(id, owner, type, number, true, createdAt);
    }
}
