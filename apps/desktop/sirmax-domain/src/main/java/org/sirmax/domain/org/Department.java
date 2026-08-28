// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.org;

import java.time.Instant;
import java.util.Objects;
import org.sirmax.domain.common.ArchiveStatus;

/**
 * An internal organizational unit (Planeamiento, Registro Civil, Caja…).
 *
 * <p>A service is not required to belong to a single department (master prompt §0.3); departments
 * are used for assignment, dashboards and reporting.
 */
public final class Department {

    private final String id;
    private final String organizationUnitId;
    private String name;
    private String code;
    private ArchiveStatus archiveStatus;
    private final Instant createdAt;

    public Department(
            String id,
            String organizationUnitId,
            String name,
            String code,
            ArchiveStatus archiveStatus,
            Instant createdAt) {
        this.id = requireText(id, "id");
        this.organizationUnitId = requireText(organizationUnitId, "organizationUnitId");
        this.name = requireText(name, "name");
        this.code = requireCode(code);
        this.archiveStatus = Objects.requireNonNull(archiveStatus, "archiveStatus");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static Department create(
            String id, String organizationUnitId, String name, String code, Instant now) {
        return new Department(
                id, organizationUnitId, name, code, ArchiveStatus.ACTIVE, now);
    }

    public String id() {
        return id;
    }

    public String organizationUnitId() {
        return organizationUnitId;
    }

    public String name() {
        return name;
    }

    public String code() {
        return code;
    }

    public ArchiveStatus archiveStatus() {
        return archiveStatus;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public boolean isActive() {
        return archiveStatus == ArchiveStatus.ACTIVE;
    }

    public void rename(String newName) {
        this.name = requireText(newName, "name");
    }

    public void recode(String newCode) {
        this.code = requireCode(newCode);
    }

    /** Deactivate without deleting — historical assignments remain valid (master prompt §31). */
    public void archive() {
        this.archiveStatus = ArchiveStatus.ARCHIVED;
    }

    public void restore() {
        this.archiveStatus = ArchiveStatus.ACTIVE;
    }

    private static String requireCode(String code) {
        String c = requireText(code, "code").toUpperCase(java.util.Locale.ROOT);
        if (!c.matches("[A-Z0-9_-]{1,16}")) {
            throw new IllegalArgumentException(
                    "code must be 1–16 chars of A–Z, 0–9, '-' or '_'");
        }
        return c;
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
        return o instanceof Department d && id.equals(d.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
