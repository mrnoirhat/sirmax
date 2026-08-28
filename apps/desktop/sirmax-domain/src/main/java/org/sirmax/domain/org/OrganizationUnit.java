// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.org;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.domain.common.ArchiveStatus;

/**
 * The local-government organization operating SIRMAX (a Dominican <em>ayuntamiento</em> in the
 * initial market, but the concept is neutral — master prompt §38).
 *
 * <p>One installation has one active {@code OrganizationUnit}. Country is an ISO 3166-1 alpha-2 code
 * so country-specific rules can be selected later without more schema.
 */
public final class OrganizationUnit {

    private final String id;
    private String name;
    private String shortName; // nullable
    private String municipality;
    private String province; // nullable
    private String country;
    private ArchiveStatus archiveStatus;
    private final Instant createdAt;
    private Instant updatedAt;

    public OrganizationUnit(
            String id,
            String name,
            String shortName,
            String municipality,
            String province,
            String country,
            ArchiveStatus archiveStatus,
            Instant createdAt,
            Instant updatedAt) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.shortName = blankToNull(shortName);
        this.municipality = requireText(municipality, "municipality");
        this.province = blankToNull(province);
        this.country = requireCountry(country);
        this.archiveStatus = Objects.requireNonNull(archiveStatus, "archiveStatus");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static OrganizationUnit create(
            String id, String name, String municipality, String country, Instant now) {
        return new OrganizationUnit(
                id, name, null, municipality, null, country, ArchiveStatus.ACTIVE, now, now);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Optional<String> shortName() {
        return Optional.ofNullable(shortName);
    }

    public String municipality() {
        return municipality;
    }

    public Optional<String> province() {
        return Optional.ofNullable(province);
    }

    public String country() {
        return country;
    }

    public ArchiveStatus archiveStatus() {
        return archiveStatus;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void update(
            String name,
            String shortName,
            String municipality,
            String province,
            String country,
            Instant now) {
        this.name = requireText(name, "name");
        this.shortName = blankToNull(shortName);
        this.municipality = requireText(municipality, "municipality");
        this.province = blankToNull(province);
        this.country = requireCountry(country);
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }

    private static String requireCountry(String value) {
        String v = requireText(value, "country").toUpperCase(java.util.Locale.ROOT);
        if (v.length() != 2) {
            throw new IllegalArgumentException("country must be an ISO 3166-1 alpha-2 code");
        }
        return v;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof OrganizationUnit u && id.equals(u.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
