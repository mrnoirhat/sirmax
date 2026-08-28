// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.common.PartyType;

class PartyValueObjectsTest {

    private static final Instant NOW = Instant.parse("2026-01-02T03:04:05Z");

    @Test
    void personNameCollapsesWhitespaceInFullName() {
        PersonName n = new PersonName("  José   Luis ", "  De la  Cruz ");
        assertThat(n.givenNames()).isEqualTo("José Luis");
        assertThat(n.full()).isEqualTo("José Luis De la Cruz");
    }

    @Test
    void personNameRejectsBlankParts() {
        assertThatThrownBy(() -> new PersonName("  ", "Cruz"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void personRefIsTypedToPerson() {
        Person p = Person.create("p1", new PersonName("Ana", "Reyes"), NOW);
        assertThat(p.ref()).isEqualTo(new PartyRef(PartyType.PERSON, "p1"));
        assertThat(p.fullName()).isEqualTo("Ana Reyes");
        assertThat(p.birthDate()).isEmpty();
    }

    @Test
    void identificationTrimsNumberAndDefaultsNotPrimary() {
        Identification id =
                Identification.of(
                        "i1", PartyRef.person("p1"), IdentificationType.CEDULA, "  001-0000000-1  ", NOW);
        assertThat(id.number()).isEqualTo("001-0000000-1");
        assertThat(id.primary()).isFalse();
        assertThat(id.asPrimary().primary()).isTrue();
    }

    @Test
    void addressRequiresAtLeastOnePart() {
        assertThatThrownBy(
                        () ->
                                new Address(
                                        "a1",
                                        PartyRef.person("p1"),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        false,
                                        NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one part");
    }

    @Test
    void addressRejectsLoneCoordinate() {
        assertThatThrownBy(
                        () ->
                                new Address(
                                        "a1",
                                        PartyRef.person("p1"),
                                        Optional.of("Santo Domingo"),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.of(18.5),
                                        Optional.empty(),
                                        false,
                                        NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("together");
    }

    @Test
    void addressOneLineJoinsPresentParts() {
        Address a =
                new Address(
                        "a1",
                        PartyRef.person("p1"),
                        Optional.of("Santiago"),
                        Optional.of("Centro"),
                        Optional.of("La Trinitaria"),
                        Optional.of("Calle 5"),
                        Optional.of("12"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        true,
                        NOW);
        assertThat(a.oneLine()).isEqualTo("Calle 5 12, La Trinitaria, Centro, Santiago");
    }
}
