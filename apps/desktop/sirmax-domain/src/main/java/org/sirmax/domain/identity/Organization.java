// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.identity;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.domain.common.ArchiveStatus;
import org.sirmax.domain.common.PartyRef;

/**
 * A non-person party: a business, a neighbourhood association (junta de vecinos), an institution
 * (master prompt §23). Named to avoid confusion with {@code org.OrganizationUnit}, which is the
 * ayuntamiento running SIRMAX.
 */
public final class Organization {

    private final String id;
    private String legalName;
    private String tradeName; // nullable
    private OrganizationKind kind;
    private String notes; // nullable
    private ArchiveStatus archiveStatus;
    private final Instant createdAt;
    private Instant updatedAt;

    public Organization(
            String id,
            String legalName,
            String tradeName,
            OrganizationKind kind,
            String notes,
            ArchiveStatus archiveStatus,
            Instant createdAt,
            Instant updatedAt) {
        this.id = requireText(id, "id");
        this.legalName = requireText(legalName, "legalName");
        this.tradeName = blankToNull(tradeName);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.notes = blankToNull(notes);
        this.archiveStatus = Objects.requireNonNull(archiveStatus, "archiveStatus");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Organization create(
            String id, String legalName, OrganizationKind kind, Instant now) {
        return new Organization(
                id, legalName, null, kind, null, ArchiveStatus.ACTIVE, now, now);
    }

    public String id() {
        return id;
    }

    public PartyRef ref() {
        return PartyRef.organization(id);
    }

    public String legalName() {
        return legalName;
    }

    public Optional<String> tradeName() {
        return Optional.ofNullable(tradeName);
    }

    public OrganizationKind kind() {
        return kind;
    }

    public Optional<String> notes() {
        return Optional.ofNullable(notes);
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
            String legalName,
            String tradeName,
            OrganizationKind kind,
            String notes,
            Instant now) {
        this.legalName = requireText(legalName, "legalName");
        this.tradeName = blankToNull(tradeName);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.notes = blankToNull(notes);
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public void archive(Instant now) {
        this.archiveStatus = ArchiveStatus.ARCHIVED;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Organization x && id.equals(x.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
