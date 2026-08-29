// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import org.sirmax.application.UseCase;
import org.sirmax.application.port.AuditRepository;
import org.sirmax.application.security.Session;
import org.sirmax.domain.audit.AuditChain;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Result;

/**
 * Checks that the audit trail has not been altered (master prompt §40 — audit integrity).
 *
 * <p>Walks the hash chain and reports the first entry that does not hold. An intact result means
 * every chained entry is exactly as it was written and none has been removed; a broken one names the
 * entry where the chain fails, which points at what happened — a changed entry breaks its own hash,
 * a deleted one breaks the link of the entry after it.
 *
 * <p>Deliberately does <b>not</b> write to the audit trail itself. Verification is a read, and a
 * check that appends to the thing it checks would keep changing the answer.
 *
 * <p>Gated by {@code audit.read}, and the result is reported plainly rather than as a reassuring
 * green tick: an integrity check that only ever says "fine" is worse than none, because people stop
 * looking.
 */
public final class VerifyAuditIntegrity
        implements UseCase<VerifyAuditIntegrity.Command, AuditChain.Verification> {

    /** How many entries one pass reads. A municipal trail is small; this is a memory guard. */
    private static final int PAGE = 5_000;

    public record Command(Session session) {}

    private final AuditRepository auditTrail;

    public VerifyAuditIntegrity(AuditRepository auditTrail) {
        this.auditTrail = auditTrail;
    }

    @Override
    public Result<AuditChain.Verification> execute(Command c) {
        if (!c.session().can(Permission.AUDIT_READ)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }

        long verified = 0;
        long unchained = 0;
        int offset = 0;
        String carriedHash = null;

        while (true) {
            var page = auditTrail.chainEntries(PAGE, offset);
            if (page.isEmpty()) {
                break;
            }
            // Each page is verified on its own, then stitched to the previous one by comparing the
            // first entry's recorded predecessor against the last hash carried over. Verifying the
            // whole trail in one list would mean holding every event in memory to prove a property
            // that only ever looks one step back.
            AuditChain.Verification result = AuditChain.verify(page);
            if (!result.isIntact()) {
                return Result.ok(result);
            }
            // Stitch this page to the previous one: the first entry must follow the last hash
            // carried over, or an entry went missing at the page boundary.
            var first = page.get(0);
            if (carriedHash != null
                    && first.entryHash().isPresent()
                    && !carriedHash.equals(first.prevHash().orElse(AuditChain.GENESIS))) {
                return Result.ok(
                        AuditChain.Verification.brokenAt(
                                first.event().id(), AuditChain.Break.LINK));
            }
            verified += result.verifiedEntries();
            unchained += result.unchainedEntries();
            carriedHash =
                    page.stream()
                            .map(e -> e.entryHash().orElse(null))
                            .filter(java.util.Objects::nonNull)
                            .reduce((a, b) -> b)
                            .orElse(carriedHash);

            if (page.size() < PAGE) {
                break;
            }
            offset += PAGE;
        }

        return Result.ok(AuditChain.Verification.intact(verified, unchained));
    }
}
