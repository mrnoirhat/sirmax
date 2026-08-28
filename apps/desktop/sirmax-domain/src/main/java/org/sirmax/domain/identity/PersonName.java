// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.identity;

import java.util.Objects;

/**
 * A person's name split into given and family parts, with a derived full name used for search and
 * display.
 *
 * @param givenNames one or more given names
 * @param familyNames one or more family names
 */
public record PersonName(String givenNames, String familyNames) {

    public PersonName {
        givenNames = require(givenNames, "givenNames");
        familyNames = require(familyNames, "familyNames");
    }

    /** "{givenNames} {familyNames}", collapsed whitespace — for the {@code full_name} column. */
    public String full() {
        return (givenNames + " " + familyNames).replaceAll("\\s+", " ").strip();
    }

    private static String require(String value, String field) {
        Objects.requireNonNull(value, field);
        String v = value.replaceAll("\\s+", " ").strip();
        if (v.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return v;
    }
}
