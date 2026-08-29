// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sirmax.application.fakes.Fakes;
import org.sirmax.application.security.Session;
import org.sirmax.domain.identity.Identification;
import org.sirmax.domain.identity.IdentificationType;
import org.sirmax.domain.identity.Person;
import org.sirmax.domain.identity.PersonName;
import org.sirmax.domain.security.AccessPolicy;
import org.sirmax.domain.security.AppUser;
import org.sirmax.domain.security.PasswordHash;
import org.sirmax.domain.security.Permission;

class FindDuplicatePeopleTest {

    private static final Instant NOW = Instant.parse("2026-03-05T13:00:00Z");

    private final Fakes.InMemoryPeople people = new Fakes.InMemoryPeople();
    private final Fakes.InMemoryIdentifications identifications = new Fakes.InMemoryIdentifications();
    private final FindDuplicatePeople find = new FindDuplicatePeople(people, identifications);

    private Session clerk;

    @BeforeEach
    void setUp() {
        AppUser u = AppUser.create("u1", "op", "Op", new PasswordHash("FAKE", "h:x"), null, NOW);
        clerk = new Session("s1", u, AccessPolicy.of(EnumSet.of(Permission.PERSON_READ)), NOW);
    }

    private Person save(String id, String given, String family, LocalDate birth) {
        Person p =
                new Person(
                        id,
                        new PersonName(given, family),
                        birth,
                        null,
                        null,
                        org.sirmax.domain.common.ArchiveStatus.ACTIVE,
                        NOW,
                        NOW);
        people.save(p);
        return p;
    }

    private FindDuplicatePeople.Command probe(
            String given, String family, LocalDate birth, String idNumber) {
        return new FindDuplicatePeople.Command(
                clerk,
                given,
                family,
                Optional.ofNullable(birth),
                idNumber == null ? Optional.empty() : Optional.of(IdentificationType.CEDULA),
                Optional.ofNullable(idNumber));
    }

    @Test
    void anExactIdentificationMatchIsConclusive() {
        Person existing = save("p-1", "José Luis", "Pérez Gómez", LocalDate.of(1980, 4, 2));
        identifications.save(
                Identification.of(
                        "i-1", existing.ref(), IdentificationType.CEDULA, "001-1234567-8", NOW));

        var found =
                find.execute(probe("Otro", "Nombre Distinto", null, "001-1234567-8")).orElseThrow();

        assertThat(found).hasSize(1);
        assertThat(found.get(0).isConclusive()).isTrue();
        assertThat(found.get(0).reason()).isEqualTo("person.duplicate_id");
    }

    @Test
    void accentsAndAMissingMiddleNameStillMatch() {
        save("p-1", "José Luis", "Peña Gómez", LocalDate.of(1980, 4, 2));

        var found = find.execute(probe("Jose", "Pena Gomez", null, null)).orElseThrow();

        assertThat(found).hasSize(1);
        assertThat(found.get(0).person().id()).isEqualTo("p-1");
        assertThat(found.get(0).reason()).isEqualTo("person.similar_name");
    }

    @Test
    void aMatchingBirthDateStrengthensTheSuggestion() {
        save("p-1", "Ana", "Rodríguez Cruz", LocalDate.of(1991, 7, 15));

        var withBirthDate =
                find.execute(probe("Ana", "Rodriguez", LocalDate.of(1991, 7, 15), null))
                        .orElseThrow();
        var withoutBirthDate = find.execute(probe("Ana", "Rodriguez", null, null)).orElseThrow();

        assertThat(withBirthDate.get(0).score())
                .isGreaterThan(withoutBirthDate.get(0).score());
        assertThat(withBirthDate.get(0).reason()).isEqualTo("person.similar_name_and_birthdate");
    }

    @Test
    void anUnrelatedNameIsNotOffered() {
        save("p-1", "Ana", "Rodríguez Cruz", LocalDate.of(1991, 7, 15));

        assertThat(find.execute(probe("Pedro", "Martínez", null, null)).orElseThrow()).isEmpty();
    }

    @Test
    void conclusiveMatchesRankAboveNameSuggestions() {
        Person byId = save("p-1", "Carmen", "Santos", LocalDate.of(1975, 1, 1));
        identifications.save(
                Identification.of("i-1", byId.ref(), IdentificationType.CEDULA, "001-9999999-9", NOW));
        save("p-2", "Carmen", "Santos", LocalDate.of(1988, 2, 2));

        var found =
                find.execute(probe("Carmen", "Santos", null, "001-9999999-9")).orElseThrow();

        assertThat(found).hasSize(2);
        assertThat(found.get(0).person().id()).isEqualTo("p-1");
        assertThat(found.get(0).isConclusive()).isTrue();
    }
}
