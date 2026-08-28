// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.identity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.domain.common.ArchiveStatus;
import org.sirmax.domain.common.PartyRef;

/**
 * A natural person, registered once and referenced from many contexts — applicant, property owner,
 * merchant, payer, deceased, representative, party/witness (master prompt §23).
 *
 * <p>Identifications, addresses and contact points are attached separately via {@link #ref()}.
 */
public final class Person {

    private final String id;
    private PersonName name;
    private LocalDate birthDate; // nullable
    private Sex sex; // nullable
    private String notes; // nullable
    private ArchiveStatus archiveStatus;
    private final Instant createdAt;
    private Instant updatedAt;

    public Person(
            String id,
            PersonName name,
            LocalDate birthDate,
            Sex sex,
            String notes,
            ArchiveStatus archiveStatus,
            Instant createdAt,
            Instant updatedAt) {
        this.id = requireText(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.birthDate = birthDate;
        this.sex = sex;
        this.notes = blankToNull(notes);
        this.archiveStatus = Objects.requireNonNull(archiveStatus, "archiveStatus");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Person create(String id, PersonName name, Instant now) {
        return new Person(id, name, null, null, null, ArchiveStatus.ACTIVE, now, now);
    }

    public String id() {
        return id;
    }

    public PartyRef ref() {
        return PartyRef.person(id);
    }

    public PersonName name() {
        return name;
    }

    /** Denormalized name for the {@code full_name} search column. */
    public String fullName() {
        return name.full();
    }

    public Optional<LocalDate> birthDate() {
        return Optional.ofNullable(birthDate);
    }

    public Optional<Sex> sex() {
        return Optional.ofNullable(sex);
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

    public void update(PersonName name, LocalDate birthDate, Sex sex, String notes, Instant now) {
        this.name = Objects.requireNonNull(name, "name");
        this.birthDate = birthDate;
        this.sex = sex;
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
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Person p && id.equals(p.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
