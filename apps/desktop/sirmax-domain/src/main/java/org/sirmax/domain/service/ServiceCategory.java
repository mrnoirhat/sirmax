// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.service;

import java.time.Instant;
import java.util.Objects;
import org.sirmax.domain.common.ArchiveStatus;

/** A grouping in the service catalog (Planeamiento, Registro Civil, Cementerios…). */
public final class ServiceCategory {

    private final String id;
    private String code;
    private String name;
    private int sortOrder;
    private ArchiveStatus archiveStatus;
    private final Instant createdAt;

    public ServiceCategory(
            String id,
            String code,
            String name,
            int sortOrder,
            ArchiveStatus archiveStatus,
            Instant createdAt) {
        this.id = requireText(id, "id");
        this.code = requireCode(code);
        this.name = requireText(name, "name");
        this.sortOrder = sortOrder;
        this.archiveStatus = Objects.requireNonNull(archiveStatus, "archiveStatus");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static ServiceCategory create(
            String id, String code, String name, int sortOrder, Instant now) {
        return new ServiceCategory(id, code, name, sortOrder, ArchiveStatus.ACTIVE, now);
    }

    public String id() {
        return id;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public int sortOrder() {
        return sortOrder;
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

    public void reorder(int newSortOrder) {
        this.sortOrder = newSortOrder;
    }

    public void archive() {
        this.archiveStatus = ArchiveStatus.ARCHIVED;
    }

    public void restore() {
        this.archiveStatus = ArchiveStatus.ACTIVE;
    }

    private static String requireCode(String code) {
        String c = requireText(code, "code").toUpperCase(java.util.Locale.ROOT);
        if (!c.matches("[A-Z0-9_-]{1,32}")) {
            throw new IllegalArgumentException("code must be 1–32 chars of A–Z, 0–9, '-' or '_'");
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
        return o instanceof ServiceCategory c && id.equals(c.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
