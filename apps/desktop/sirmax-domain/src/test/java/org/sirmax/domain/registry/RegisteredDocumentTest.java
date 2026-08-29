// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sirmax.domain.common.PartyRef;

class RegisteredDocumentTest {

    private static final Instant NOW = Instant.parse("2026-04-02T11:00:00Z");

    private static RegisteredDocument presented() {
        RegisteredDocument document =
                RegisteredDocument.presented(
                        "doc-1", "REG-2026-000001", "Acto de venta", "Venta de solar", "proc-1", NOW);
        document.addParty(
                new RegisteredDocument.Party(
                        "p-1", "doc-1", PartyRef.person("per-1"), "vendedor"));
        document.addParty(
                new RegisteredDocument.Party(
                        "p-2", "doc-1", PartyRef.person("per-2"), "comprador"));
        return document;
    }

    @Test
    void aPresentedDocumentIsNotYetInTheRegister() {
        RegisteredDocument document = presented();

        assertThat(document.status()).isEqualTo(RegisteredDocument.Status.PRESENTED);
        assertThat(document.canIssueCertifiedCopy()).isFalse();
        assertThat(document.registeredAt()).isEmpty();
        assertThat(document.parties()).hasSize(2);
    }

    @Test
    void registeringNeedsABookAndAFolio() {
        RegisteredDocument document = presented();

        assertThatThrownBy(() -> document.register(null, null, "12", NOW))
                .isInstanceOf(IllegalArgumentException.class);

        document.register("7", "II", "134", NOW);

        assertThat(document.status()).isEqualTo(RegisteredDocument.Status.REGISTERED);
        assertThat(document.book()).contains("7");
        assertThat(document.folio()).contains("134");
        assertThat(document.registeredAt()).contains(NOW);
        assertThat(document.canIssueCertifiedCopy()).isTrue();
    }

    @Test
    void aRegisteredEntryIsFrozenAndCorrectedOnlyByAnnotation() {
        RegisteredDocument document = presented();
        document.register("7", null, "134", NOW);

        assertThatThrownBy(
                        () ->
                                document.updateDetails(
                                        "Otro tipo", "Otro título", LocalDate.of(2026, 1, 1), NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("annotation");
        assertThatThrownBy(
                        () ->
                                document.addParty(
                                        new RegisteredDocument.Party(
                                                "p-3",
                                                "doc-1",
                                                PartyRef.person("per-3"),
                                                "testigo")))
                .isInstanceOf(IllegalStateException.class);

        document.annotate(
                new RegisteredDocument.Annotation(
                        "a-1",
                        "doc-1",
                        "Se corrige el nombre del comprador según acta rectificativa",
                        Optional.of("u-1"),
                        NOW));
        assertThat(document.annotations()).hasSize(1);
    }

    @Test
    void annulmentKeepsTheEntryAndItsFolio() {
        RegisteredDocument document = presented();
        document.register("7", null, "134", NOW);

        document.annul("Sentencia de nulidad", NOW);

        assertThat(document.status()).isEqualTo(RegisteredDocument.Status.ANNULLED);
        assertThat(document.folio()).contains("134");
        assertThat(document.canIssueCertifiedCopy()).isFalse();
        assertThat(document.notes()).contains("Sentencia de nulidad");
    }

    @Test
    void theSamePartyCannotAppearTwiceInTheSameRole() {
        RegisteredDocument document = presented();

        assertThatThrownBy(
                        () ->
                                document.addParty(
                                        new RegisteredDocument.Party(
                                                "p-3",
                                                "doc-1",
                                                PartyRef.person("per-1"),
                                                "vendedor")))
                .isInstanceOf(IllegalArgumentException.class);

        // the same person in a different role is legitimate
        document.addParty(
                new RegisteredDocument.Party(
                        "p-4", "doc-1", PartyRef.person("per-1"), "representante"));
        assertThat(document.parties()).hasSize(3);
    }

    @Test
    void rejectionNeedsAReasonAndBlocksRegistration() {
        RegisteredDocument document = presented();
        assertThatThrownBy(() -> document.reject(" ", NOW))
                .isInstanceOf(IllegalArgumentException.class);

        document.reject("Falta la firma del vendedor", NOW);
        assertThat(document.status()).isEqualTo(RegisteredDocument.Status.REJECTED);
    }
}
