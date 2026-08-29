// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.AssetRepository;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.NumberingRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.asset.Agreement;
import org.sirmax.domain.asset.AssetHolder;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Result;

/**
 * Moves a contract to a new holder (master prompt §25, §26 — "traspaso de contrato de
 * arrendamiento", "traspaso por herencia").
 *
 * <p>The old agreement is closed as TRANSFERRED and a new one is created pointing back at it. The
 * holder is never swapped in place: a dispute over a cemetery plot or a market stall turns on
 * exactly this chain, and a mutable holder column would erase it.
 *
 * <p>The outgoing holder's {@link AssetHolder} period is closed the day before the transfer takes
 * effect, so the asset's history has no overlap and no gap.
 */
public final class TransferAgreement implements UseCase<TransferAgreement.Command, Agreement> {

    private static final String SEQUENCE = "CONT";
    private static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();

    public record Command(
            Session session,
            String agreementId,
            PartyRef newHolder,
            LocalDate effectiveDate,
            String reason,
            Optional<String> procedureId,
            String source) {}

    private final AssetRepository assets;
    private final NumberingRepository numbering;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public TransferAgreement(
            AssetRepository assets,
            NumberingRepository numbering,
            IdGenerator ids,
            Clock clock,
            UnitOfWork unitOfWork,
            Audit audit) {
        this.assets = assets;
        this.numbering = numbering;
        this.ids = ids;
        this.clock = clock;
        this.unitOfWork = unitOfWork;
        this.audit = audit;
    }

    @Override
    public Result<Agreement> execute(Command c) {
        if (!c.session().can(Permission.PROCEDURE_DECIDE)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        if (c.reason() == null || c.reason().isBlank()) {
            return Result.err("REASON_REQUIRED", "agreement.transfer_needs_reason");
        }

        Optional<Agreement> found = assets.findAgreementById(c.agreementId());
        if (found.isEmpty()) {
            return Result.err("AGREEMENT_NOT_FOUND", "agreement.not_found");
        }
        Agreement agreement = found.get();
        if (agreement.status() != Agreement.Status.ACTIVE) {
            return Result.err("NOT_ACTIVE", "agreement.not_active");
        }
        if (agreement.holder().equals(c.newHolder())) {
            return Result.err("SAME_HOLDER", "agreement.same_holder");
        }
        if (c.effectiveDate().isBefore(agreement.startDate())) {
            return Result.err("INVALID_DATE", "agreement.transfer_before_start");
        }

        return Result.ok(unitOfWork.execute(() -> doTransfer(c, agreement)));
    }

    private Agreement doTransfer(Command c, Agreement agreement) {
        Instant now = clock.now();
        String code =
                numbering.allocate(
                        SEQUENCE, SEQUENCE, LocalDate.ofInstant(now, LOCAL_ZONE).getYear());

        Agreement successor =
                agreement.transferTo(ids.newId(), code, c.newHolder(), c.effectiveDate(), now);
        successor.setNotes(c.reason(), now);

        assets.save(agreement); // now TRANSFERRED
        assets.save(successor);

        agreement.assetId()
                .ifPresent(assetId -> moveHolder(agreement, successor, assetId, c, code, now));

        audit.record(
                c.session().audit(c.source()),
                "agreement.transferred",
                "Agreement",
                successor.id(),
                agreement.code() + " · " + agreement.holder().id(),
                code + " · " + c.newHolder().id(),
                c.reason());
        return successor;
    }

    /** Close the outgoing period the day before, open the incoming one on the effective date. */
    private void moveHolder(
            Agreement outgoing,
            Agreement successor,
            String assetId,
            Command c,
            String code,
            Instant now) {
        for (AssetHolder holder : assets.currentHoldersOf(assetId)) {
            if (holder.party().equals(outgoing.holder()) && holder.role().derivesFromAgreement()) {
                LocalDate lastDay = c.effectiveDate().minusDays(1);
                // A same-day transfer would otherwise produce a period ending before it began.
                assets.save(
                        holder.endedOn(
                                lastDay.isBefore(holder.fromDate()) ? holder.fromDate() : lastDay));
            }
        }
        assets.save(
                new AssetHolder(
                        ids.newId(),
                        assetId,
                        c.newHolder(),
                        successorRole(outgoing),
                        java.util.OptionalInt.empty(),
                        c.effectiveDate(),
                        Optional.empty(),
                        Optional.of(code),
                        now));
    }

    private static org.sirmax.domain.asset.HolderRole successorRole(Agreement agreement) {
        return switch (agreement.kind()) {
            case LEASE, STALL_ASSIGNMENT -> org.sirmax.domain.asset.HolderRole.LESSEE;
            case CONCESSION -> org.sirmax.domain.asset.HolderRole.CONCESSIONAIRE;
            case PUBLIC_SPACE_PERMIT, OTHER -> org.sirmax.domain.asset.HolderRole.OCCUPANT;
        };
    }
}
