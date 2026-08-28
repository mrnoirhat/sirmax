// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.service;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.domain.common.ArchiveStatus;

/**
 * A configurable municipal service (master prompt §15, §22, §55).
 *
 * <p>Stable metadata lives here; the requirements / form / workflow / fee rules / output documents
 * live on the {@link ServiceDefinitionVersion}s. {@code currentVersionId} points at the one ACTIVE
 * version operators use to open new procedures; older versions stay interpretable for procedures
 * opened against them.
 */
public final class ServiceDefinition {

    private final String id;
    private final String code; // immutable once created
    private String categoryId;
    private String name;
    private String description; // nullable
    private ServiceType serviceType;
    private String departmentId; // nullable — a service need not belong to one department
    private String countryScope;
    private boolean municipalOverrideAllowed;
    private String currentVersionId; // nullable until first publish
    private ArchiveStatus archiveStatus;
    private final Instant createdAt;
    private Instant updatedAt;

    public ServiceDefinition(
            String id,
            String code,
            String categoryId,
            String name,
            String description,
            ServiceType serviceType,
            String departmentId,
            String countryScope,
            boolean municipalOverrideAllowed,
            String currentVersionId,
            ArchiveStatus archiveStatus,
            Instant createdAt,
            Instant updatedAt) {
        this.id = requireText(id, "id");
        this.code = requireCode(code);
        this.categoryId = requireText(categoryId, "categoryId");
        this.name = requireText(name, "name");
        this.description = blankToNull(description);
        this.serviceType = Objects.requireNonNull(serviceType, "serviceType");
        this.departmentId = blankToNull(departmentId);
        this.countryScope = requireCountry(countryScope);
        this.municipalOverrideAllowed = municipalOverrideAllowed;
        this.currentVersionId = blankToNull(currentVersionId);
        this.archiveStatus = Objects.requireNonNull(archiveStatus, "archiveStatus");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static ServiceDefinition create(
            String id,
            String code,
            String categoryId,
            String name,
            ServiceType serviceType,
            String countryScope,
            Instant now) {
        return new ServiceDefinition(
                id,
                code,
                categoryId,
                name,
                null,
                serviceType,
                null,
                countryScope,
                true,
                null,
                ArchiveStatus.ACTIVE,
                now,
                now);
    }

    public String id() {
        return id;
    }

    public String code() {
        return code;
    }

    public String categoryId() {
        return categoryId;
    }

    public String name() {
        return name;
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    public ServiceType serviceType() {
        return serviceType;
    }

    public Optional<String> departmentId() {
        return Optional.ofNullable(departmentId);
    }

    public String countryScope() {
        return countryScope;
    }

    public boolean municipalOverrideAllowed() {
        return municipalOverrideAllowed;
    }

    public Optional<String> currentVersionId() {
        return Optional.ofNullable(currentVersionId);
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

    /** Available to open new procedures: active and with a published version. */
    public boolean isAvailable() {
        return archiveStatus == ArchiveStatus.ACTIVE && currentVersionId != null;
    }

    public void updateMetadata(
            String categoryId,
            String name,
            String description,
            ServiceType serviceType,
            String departmentId,
            boolean municipalOverrideAllowed,
            Instant now) {
        this.categoryId = requireText(categoryId, "categoryId");
        this.name = requireText(name, "name");
        this.description = blankToNull(description);
        this.serviceType = Objects.requireNonNull(serviceType, "serviceType");
        this.departmentId = blankToNull(departmentId);
        this.municipalOverrideAllowed = municipalOverrideAllowed;
        touch(now);
    }

    public void setCurrentVersion(String versionId, Instant now) {
        this.currentVersionId = requireText(versionId, "versionId");
        touch(now);
    }

    /** Deactivate the whole service (master prompt §22 — archive/deactivate, never delete). */
    public void deactivate(Instant now) {
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

    private static String requireCode(String code) {
        String c = requireText(code, "code").toUpperCase(java.util.Locale.ROOT);
        if (!c.matches("[A-Z0-9_-]{2,40}")) {
            throw new IllegalArgumentException("code must be 2–40 chars of A–Z, 0–9, '-' or '_'");
        }
        return c;
    }

    private static String requireCountry(String value) {
        String v = requireText(value, "countryScope").toUpperCase(java.util.Locale.ROOT);
        if (v.length() != 2) {
            throw new IllegalArgumentException("countryScope must be an ISO 3166-1 alpha-2 code");
        }
        return v;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ServiceDefinition s && id.equals(s.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
