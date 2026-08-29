// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.shared.SirmaxException;

/**
 * Makes the audit trail tamper-evident (master prompt §40).
 *
 * <p>Each entry's hash covers its own content <em>and</em> the previous entry's hash, so altering
 * one entry — or removing one — breaks every hash after it. The database triggers from V0001 already
 * refuse UPDATE and DELETE, but a trigger can be dropped by anyone holding the file. This cannot:
 * it does not prevent tampering, it makes tampering impossible to hide.
 *
 * <p>Hashing lives in the domain, not in the SQLite adapter, because the chain is a rule about what
 * the audit trail <em>is</em>. Recomputing it must not depend on how the rows happen to be stored.
 *
 * <p>Deliberately not a MAC. A keyed hash would need a key on the same machine as the data, which
 * buys nothing against an attacker who has the file: they would have the key too. What this gives is
 * detection — an auditor with an earlier export can prove the middle was rewritten — and that is an
 * honest claim rather than an overstated one.
 */
public final class AuditChain {

    /**
     * Field separator inside the hashed material. A unit separator appears in neither a UUID, an
     * ISO-8601 timestamp nor the JSON SIRMAX writes — without one, moving a character between two
     * adjacent fields would leave the hash unchanged.
     */
    private static final char SEPARATOR = 0x1F;

    /** The hash a chain's first entry links to, so genesis is explicit rather than null-shaped. */
    public static final String GENESIS = "0".repeat(64);

    private AuditChain() {}

    /**
     * The hash of {@code event} following {@code previousHash}.
     *
     * <p>Every field that carries meaning is covered, separated by a character that cannot appear in
     * a UUID or an ISO timestamp — without a separator, moving a character between two adjacent
     * fields would leave the hash unchanged.
     */
    public static String hash(AuditEvent event, String previousHash) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(previousHash, "previousHash");

        StringBuilder material = new StringBuilder(256);
        append(material, previousHash);
        append(material, event.id());
        append(material, event.whenAt().toString());
        append(material, event.actorUserId().orElse(""));
        append(material, event.action());
        append(material, event.entityType());
        append(material, event.entityId());
        append(material, event.beforeJson().orElse(""));
        append(material, event.afterJson().orElse(""));
        append(material, event.reason().orElse(""));
        append(material, event.sessionId());
        append(material, event.source());

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(material.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new SirmaxException("SHA-256 is unavailable on this JVM", e);
        }
    }

    /**
     * Walk a chain and report the first entry that does not hold.
     *
     * @param entries in the order they were written, oldest first
     * @return the verdict; {@link Verification#isIntact()} when every link checks out
     */
    public static Verification verify(List<Entry> entries) {
        Objects.requireNonNull(entries, "entries");
        String expectedPrevious = null;

        for (Entry entry : entries) {
            // Entries written before the chain existed carry no hash. They are counted and
            // reported, never vouched for: claiming to verify them would be the one lie an
            // integrity check cannot afford.
            if (entry.entryHash().isEmpty()) {
                continue;
            }
            String previous = entry.prevHash().orElse(GENESIS);
            if (expectedPrevious != null && !expectedPrevious.equals(previous)) {
                return Verification.brokenAt(entry.event().id(), Break.LINK);
            }
            String recomputed = hash(entry.event(), previous);
            if (!recomputed.equals(entry.entryHash().get())) {
                return Verification.brokenAt(entry.event().id(), Break.CONTENT);
            }
            expectedPrevious = entry.entryHash().get();
        }
        long unchained = entries.stream().filter(e -> e.entryHash().isEmpty()).count();
        return Verification.intact(entries.size() - unchained, unchained);
    }

    /** An audit event together with the chain fields stored beside it. */
    public record Entry(AuditEvent event, Optional<String> prevHash, Optional<String> entryHash) {

        public Entry {
            Objects.requireNonNull(event, "event");
            prevHash = prevHash == null ? Optional.empty() : prevHash;
            entryHash = entryHash == null ? Optional.empty() : entryHash;
        }
    }

    /** How a chain was broken, which points at what happened. */
    public enum Break {
        /** An entry's own hash does not match its content: that entry was edited. */
        CONTENT,
        /** An entry does not follow the one before it: an entry was removed or reordered. */
        LINK
    }

    /**
     * @param verifiedEntries entries whose hashes were checked and held
     * @param unchainedEntries entries predating the chain, reported but not vouched for
     */
    public record Verification(
            boolean isIntact,
            long verifiedEntries,
            long unchainedEntries,
            Optional<String> brokenAtEventId,
            Optional<Break> breakKind) {

        public static Verification intact(long verified, long unchained) {
            return new Verification(true, verified, unchained, Optional.empty(), Optional.empty());
        }

        /** A break found at {@code eventId}. Public so a paged caller can report one it detects. */
        public static Verification brokenAt(String eventId, Break kind) {
            return new Verification(false, 0, 0, Optional.of(eventId), Optional.of(kind));
        }
    }

    private static void append(StringBuilder material, String value) {
        material.append(value).append(SEPARATOR);
    }
}
