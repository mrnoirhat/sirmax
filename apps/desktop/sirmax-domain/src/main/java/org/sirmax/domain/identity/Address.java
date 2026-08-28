// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.identity;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.domain.common.PartyRef;

/**
 * A normalized location attached to a party (master prompt §24). Every part is optional so partial
 * addresses (very common at a counter) are allowed; at least one part must be present.
 *
 * <p>Latitude/longitude are optional and only for future maps; they are never put in URLs.
 */
public record Address(
        String id,
        PartyRef owner,
        Optional<String> municipality,
        Optional<String> districtSector,
        Optional<String> neighborhood,
        Optional<String> street,
        Optional<String> streetNumber,
        Optional<String> reference,
        Optional<String> postalCode,
        Optional<Double> latitude,
        Optional<Double> longitude,
        boolean primary,
        Instant createdAt) {

    public Address {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(createdAt, "createdAt");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        municipality = norm(municipality);
        districtSector = norm(districtSector);
        neighborhood = norm(neighborhood);
        street = norm(street);
        streetNumber = norm(streetNumber);
        reference = norm(reference);
        postalCode = norm(postalCode);
        latitude = latitude == null ? Optional.empty() : latitude;
        longitude = longitude == null ? Optional.empty() : longitude;
        boolean anyPart =
                municipality.isPresent()
                        || districtSector.isPresent()
                        || neighborhood.isPresent()
                        || street.isPresent()
                        || streetNumber.isPresent()
                        || reference.isPresent();
        if (!anyPart) {
            throw new IllegalArgumentException("Address must have at least one part");
        }
        if (latitude.isPresent() != longitude.isPresent()) {
            throw new IllegalArgumentException("latitude and longitude must be set together");
        }
    }

    /** Human-readable one-line rendering, joining the present parts. */
    public String oneLine() {
        StringBuilder sb = new StringBuilder();
        street.ifPresent(s -> sb.append(s));
        streetNumber.ifPresent(s -> sb.append(sb.isEmpty() ? "" : " ").append(s));
        neighborhood.ifPresent(s -> sb.append(sb.isEmpty() ? "" : ", ").append(s));
        districtSector.ifPresent(s -> sb.append(sb.isEmpty() ? "" : ", ").append(s));
        municipality.ifPresent(s -> sb.append(sb.isEmpty() ? "" : ", ").append(s));
        return sb.toString();
    }

    private static Optional<String> norm(Optional<String> v) {
        if (v == null || v.isEmpty()) {
            return Optional.empty();
        }
        String s = v.get().strip();
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }
}
