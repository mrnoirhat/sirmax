// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.shared.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NormalizationTest {

    @Test
    void foldsCaseAccentsAndRepeatedWhitespace() {
        assertThat(Normalization.fold("  José   Luis  Peña Gómez ")).isEqualTo("jose luis pena gomez");
        assertThat(Normalization.fold("NÚÑEZ")).isEqualTo("nunez");
    }

    @Test
    void foldsNullAndBlankToTheEmptyString() {
        assertThat(Normalization.fold(null)).isEmpty();
        assertThat(Normalization.fold("   ")).isEmpty();
    }

    @Test
    void tokensDropSingleCharacterParticles() {
        assertThat(Normalization.tokens("Ana de la Cruz"))
                .containsExactly("ana", "de", "la", "cruz");
        assertThat(Normalization.tokens("J Pérez")).containsExactly("perez");
    }

    @Test
    void similarityIgnoresWordOrderAndAccents() {
        assertThat(Normalization.similarity("José Pérez", "Perez Jose")).isEqualTo(1.0);
    }

    @Test
    void aMissingMiddleNameStillScoresHighly() {
        assertThat(Normalization.similarity("José Luis Pérez Gómez", "Jose Perez Gomez"))
                .isGreaterThan(0.7);
    }

    @Test
    void unrelatedNamesScoreZero() {
        assertThat(Normalization.similarity("Ana Rodríguez", "Pedro Martínez")).isZero();
        assertThat(Normalization.similarity("", "Pedro")).isZero();
    }
}
