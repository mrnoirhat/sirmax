// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuditChainTest {

    private static AuditEvent event(String id, String action, String entityId) {
        return new AuditEvent(
                id,
                Instant.parse("2026-07-01T10:00:00Z"),
                Optional.of("u-1"),
                action,
                "Invoice",
                entityId,
                Optional.empty(),
                Optional.of("FACT-2026-000001"),
                Optional.empty(),
                "s-1",
                "test");
    }

    /** Build a well-formed chain, as the sink does. */
    private static List<AuditChain.Entry> chainOf(AuditEvent... events) {
        List<AuditChain.Entry> entries = new ArrayList<>();
        String previous = AuditChain.GENESIS;
        for (AuditEvent e : events) {
            String hash = AuditChain.hash(e, previous);
            entries.add(new AuditChain.Entry(e, Optional.of(previous), Optional.of(hash)));
            previous = hash;
        }
        return entries;
    }

    @Test
    void anUntouchedChainVerifies() {
        var chain =
                chainOf(
                        event("a", "invoice.issued", "inv-1"),
                        event("b", "payment.registered", "inv-1"),
                        event("c", "invoice.voided", "inv-1"));

        AuditChain.Verification result = AuditChain.verify(chain);

        assertThat(result.isIntact()).isTrue();
        assertThat(result.verifiedEntries()).isEqualTo(3);
        assertThat(result.unchainedEntries()).isZero();
    }

    @Test
    void editingAnEntryBreaksItsOwnHash() {
        var chain =
                new ArrayList<>(
                        chainOf(
                                event("a", "invoice.issued", "inv-1"),
                                event("b", "payment.registered", "inv-1")));

        // Someone rewrites what the second entry says, keeping its stored hashes.
        var tampered = chain.get(1);
        chain.set(
                1,
                new AuditChain.Entry(
                        event("b", "payment.refunded", "inv-1"),
                        tampered.prevHash(),
                        tampered.entryHash()));

        AuditChain.Verification result = AuditChain.verify(chain);

        assertThat(result.isIntact()).isFalse();
        assertThat(result.brokenAtEventId()).contains("b");
        assertThat(result.breakKind()).contains(AuditChain.Break.CONTENT);
    }

    @Test
    void removingAnEntryBreaksTheLinkOfTheOneAfterIt() {
        var chain =
                new ArrayList<>(
                        chainOf(
                                event("a", "invoice.issued", "inv-1"),
                                event("b", "fee.override", "inv-1"),
                                event("c", "payment.registered", "inv-1")));

        // The inconvenient middle entry is deleted.
        chain.remove(1);

        AuditChain.Verification result = AuditChain.verify(chain);

        assertThat(result.isIntact()).isFalse();
        assertThat(result.brokenAtEventId()).contains("c");
        assertThat(result.breakKind()).contains(AuditChain.Break.LINK);
    }

    @Test
    void reorderingIsDetectedToo() {
        var chain =
                new ArrayList<>(
                        chainOf(
                                event("a", "invoice.issued", "inv-1"),
                                event("b", "payment.registered", "inv-1"),
                                event("c", "invoice.voided", "inv-1")));

        var second = chain.get(1);
        chain.set(1, chain.get(2));
        chain.set(2, second);

        assertThat(AuditChain.verify(chain).isIntact()).isFalse();
    }

    @Test
    void entriesPredatingTheChainAreReportedNotVouchedFor() {
        var legacy =
                new AuditChain.Entry(
                        event("old", "auth.signin", "u-1"), Optional.empty(), Optional.empty());
        var chain = new ArrayList<AuditChain.Entry>();
        chain.add(legacy);
        chain.addAll(chainOf(event("a", "invoice.issued", "inv-1")));

        AuditChain.Verification result = AuditChain.verify(chain);

        assertThat(result.isIntact()).isTrue();
        assertThat(result.verifiedEntries()).isEqualTo(1);
        assertThat(result.unchainedEntries()).isEqualTo(1);
    }

    @Test
    void theHashCoversEveryFieldThatCarriesMeaning() {
        AuditEvent base = event("a", "invoice.issued", "inv-1");
        String baseline = AuditChain.hash(base, AuditChain.GENESIS);

        assertThat(AuditChain.hash(event("a", "invoice.voided", "inv-1"), AuditChain.GENESIS))
                .isNotEqualTo(baseline);
        assertThat(AuditChain.hash(event("a", "invoice.issued", "inv-2"), AuditChain.GENESIS))
                .isNotEqualTo(baseline);
        assertThat(AuditChain.hash(base, "some-other-previous-hash")).isNotEqualTo(baseline);
    }

    @Test
    void fieldBoundariesCannotBeShiftedWithoutChangingTheHash() {
        // Without a separator, "ab" + "c" and "a" + "bc" would hash identically.
        AuditEvent left = withActionAndEntity("ab", "c");
        AuditEvent right = withActionAndEntity("a", "bc");

        assertThat(AuditChain.hash(left, AuditChain.GENESIS))
                .isNotEqualTo(AuditChain.hash(right, AuditChain.GENESIS));
    }

    private static AuditEvent withActionAndEntity(String action, String entityId) {
        return new AuditEvent(
                "x",
                Instant.parse("2026-07-01T10:00:00Z"),
                Optional.empty(),
                action,
                "T",
                entityId,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                "s",
                "t");
    }
}
