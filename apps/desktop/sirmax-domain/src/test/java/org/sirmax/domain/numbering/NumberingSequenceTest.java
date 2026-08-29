// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.numbering;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class NumberingSequenceTest {

    private static final Instant NOW = Instant.parse("2026-01-05T09:00:00Z");

    @Test
    void allocatesZeroPaddedCodesInSequence() {
        NumberingSequence seq = NumberingSequence.create("FACT", "FACT", NOW);

        assertThat(seq.allocate(2026, NOW)).isEqualTo("FACT-2026-000001");
        assertThat(seq.allocate(2026, NOW)).isEqualTo("FACT-2026-000002");
        assertThat(seq.allocate(2026, NOW)).isEqualTo("FACT-2026-000003");
    }

    @Test
    void yearlyResetRestartsTheCounterInANewYear() {
        NumberingSequence seq = NumberingSequence.create("TRM", "TRM", NOW);
        seq.allocate(2026, NOW);
        seq.allocate(2026, NOW);

        assertThat(seq.allocate(2027, NOW)).isEqualTo("TRM-2027-000001");
    }

    @Test
    void withoutYearlyResetTheCounterRunsContinuously() {
        NumberingSequence seq =
                new NumberingSequence("REG", "REG", 6, false, 2026, 41L, NOW);

        assertThat(seq.allocate(2026, NOW)).isEqualTo("REG-2026-000041");
        assertThat(seq.allocate(2027, NOW)).isEqualTo("REG-2027-000042");
    }

    @Test
    void peekShowsTheNextCodeWithoutConsumingIt() {
        NumberingSequence seq = NumberingSequence.create("CERT", "CERT", NOW);

        assertThat(seq.peek(2026)).isEqualTo("CERT-2026-000001");
        assertThat(seq.allocate(2026, NOW)).isEqualTo("CERT-2026-000001");
        assertThat(seq.peek(2026)).isEqualTo("CERT-2026-000002");
    }

    @Test
    void reconfiguringChangesPresentationButNeverRewindsTheCounter() {
        NumberingSequence seq = NumberingSequence.create("TRM", "TRM", NOW);
        seq.allocate(2026, NOW);
        seq.allocate(2026, NOW);

        seq.reconfigure("EXP", 4, true, NOW);

        assertThat(seq.allocate(2026, NOW)).isEqualTo("EXP-2026-0003");
        assertThat(seq.nextValue()).isEqualTo(4L);
    }
}
