// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.asset;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import org.sirmax.domain.common.ArchiveStatus;
import org.sirmax.shared.JsonDoc;

/**
 * A place the municipality keeps a record of: a parcel, a cemetery niche, a market stall, a kiosk
 * (master prompt §25, §6, §7).
 *
 * <p>This is the answer to "do not hard-code every service as its own unrelated architecture". A
 * cemetery is not a different <em>kind of thing</em> from a market — both are containers of
 * grantable spaces with a code, a location and a holder. The differences that are real live in
 * {@link #kind()} and in {@link #attributes()}, the module-specific JSON.
 *
 * <p>Containment is a self-reference: cementerio → sección → nicho, mercado → casilla. One parent
 * link replaces four parallel hierarchies, and a new asset kind is configuration rather than a
 * migration.
 */
public final class MunicipalAsset {

    private final String id;
    private final String code;
    private final AssetKind kind;
    private String name;
    private String parentId; // nullable — a top-level asset has none

    private String addressLine; // nullable
    private String sector; // nullable
    private String municipality; // nullable
    private String province; // nullable
    private Double latitude; // nullable
    private Double longitude; // nullable

    private Long areaSqM; // nullable — whole square metres; area-based fees bill on this
    private boolean municipallyOwned;
    private JsonDoc attributes;

    private Availability availability;
    private ArchiveStatus archiveStatus;
    private String notes; // nullable
    private final Instant createdAt;
    private Instant updatedAt;

    public MunicipalAsset(
            String id,
            String code,
            AssetKind kind,
            String name,
            String parentId,
            String addressLine,
            String sector,
            String municipality,
            String province,
            Double latitude,
            Double longitude,
            Long areaSqM,
            boolean municipallyOwned,
            JsonDoc attributes,
            Availability availability,
            ArchiveStatus archiveStatus,
            String notes,
            Instant createdAt,
            Instant updatedAt) {
        this.id = requireText(id, "id");
        this.code = requireText(code, "code");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.name = requireText(name, "name");
        this.parentId = blankToNull(parentId);
        this.addressLine = blankToNull(addressLine);
        this.sector = blankToNull(sector);
        this.municipality = blankToNull(municipality);
        this.province = blankToNull(province);
        this.latitude = latitude;
        this.longitude = longitude;
        this.areaSqM = requireNonNegative(areaSqM);
        this.municipallyOwned = municipallyOwned;
        this.attributes = attributes == null ? JsonDoc.EMPTY_OBJECT : attributes;
        this.availability = Objects.requireNonNull(availability, "availability");
        this.archiveStatus = Objects.requireNonNull(archiveStatus, "archiveStatus");
        this.notes = blankToNull(notes);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static MunicipalAsset create(
            String id, String code, AssetKind kind, String name, String parentId, Instant now) {
        return new MunicipalAsset(
                id,
                code,
                kind,
                name,
                parentId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                JsonDoc.EMPTY_OBJECT,
                Availability.AVAILABLE,
                ArchiveStatus.ACTIVE,
                null,
                now,
                now);
    }

    public String id() {
        return id;
    }

    public String code() {
        return code;
    }

    public AssetKind kind() {
        return kind;
    }

    public String name() {
        return name;
    }

    public Optional<String> parentId() {
        return Optional.ofNullable(parentId);
    }

    public Optional<String> addressLine() {
        return Optional.ofNullable(addressLine);
    }

    public Optional<String> sector() {
        return Optional.ofNullable(sector);
    }

    public Optional<String> municipality() {
        return Optional.ofNullable(municipality);
    }

    public Optional<String> province() {
        return Optional.ofNullable(province);
    }

    public Optional<Double> latitude() {
        return Optional.ofNullable(latitude);
    }

    public Optional<Double> longitude() {
        return Optional.ofNullable(longitude);
    }

    public OptionalLong areaSqM() {
        return areaSqM == null ? OptionalLong.empty() : OptionalLong.of(areaSqM);
    }

    public boolean municipallyOwned() {
        return municipallyOwned;
    }

    /** Module-specific fields: cadastral references, niche capacity, the trade a stall sells. */
    public JsonDoc attributes() {
        return attributes;
    }

    public Availability availability() {
        return availability;
    }

    public ArchiveStatus archiveStatus() {
        return archiveStatus;
    }

    public Optional<String> notes() {
        return Optional.ofNullable(notes);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return archiveStatus == ArchiveStatus.ACTIVE;
    }

    /** {@code true} when this asset can be granted to a citizen right now (§26). */
    public boolean canBeGranted() {
        return isActive() && kind.isGrantable() && availability.canBeGranted();
    }

    public void updateLocation(
            String newAddressLine,
            String newSector,
            String newMunicipality,
            String newProvince,
            Instant now) {
        this.addressLine = blankToNull(newAddressLine);
        this.sector = blankToNull(newSector);
        this.municipality = blankToNull(newMunicipality);
        this.province = blankToNull(newProvince);
        touch(now);
    }

    public void setCoordinates(Double newLatitude, Double newLongitude, Instant now) {
        if (newLatitude != null && (newLatitude < -90 || newLatitude > 90)) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        if (newLongitude != null && (newLongitude < -180 || newLongitude > 180)) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
        this.latitude = newLatitude;
        this.longitude = newLongitude;
        touch(now);
    }

    public void rename(String newName, Instant now) {
        this.name = requireText(newName, "name");
        touch(now);
    }

    public void setArea(Long squareMetres, Instant now) {
        this.areaSqM = requireNonNegative(squareMetres);
        touch(now);
    }

    public void setMunicipallyOwned(boolean value, Instant now) {
        this.municipallyOwned = value;
        touch(now);
    }

    public void setAttributes(JsonDoc value, Instant now) {
        this.attributes = value == null ? JsonDoc.EMPTY_OBJECT : value;
        touch(now);
    }

    public void setNotes(String value, Instant now) {
        this.notes = blankToNull(value);
        touch(now);
    }

    public void setParent(String newParentId, Instant now) {
        String parent = blankToNull(newParentId);
        if (id.equals(parent)) {
            throw new IllegalArgumentException("An asset cannot contain itself");
        }
        this.parentId = parent;
        touch(now);
    }

    /**
     * Change availability. A container is never marked occupied — occupancy is a fact about the
     * spaces inside it, and letting a market read "OCCUPIED" would hide its free stalls.
     */
    public void setAvailability(Availability value, Instant now) {
        Objects.requireNonNull(value, "availability");
        if (kind.isContainer() && value == Availability.OCCUPIED) {
            throw new IllegalArgumentException(
                    kind + " is a container; occupancy belongs to the spaces it holds");
        }
        this.availability = value;
        touch(now);
    }

    public void archive(Instant now) {
        this.archiveStatus = ArchiveStatus.ARCHIVED;
        touch(now);
    }

    public void reactivate(Instant now) {
        this.archiveStatus = ArchiveStatus.ACTIVE;
        touch(now);
    }

    private void touch(Instant now) {
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    private static Long requireNonNegative(Long value) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException("areaSqM must be >= 0");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String v = value.strip();
        if (v.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return v;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String v = value.strip();
        return v.isEmpty() ? null : v;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MunicipalAsset other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
