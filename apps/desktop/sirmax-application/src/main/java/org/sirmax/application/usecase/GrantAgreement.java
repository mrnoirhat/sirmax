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
import org.sirmax.domain.asset.AgreementKind;
import org.sirmax.domain.asset.AssetHolder;
import org.sirmax.domain.asset.Availability;
import org.sirmax.domain.asset.HolderRole;
import org.sirmax.domain.asset.MunicipalAsset;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.security.Permission;
import org.sirmax.shared.Money;
import org.sirmax.shared.Result;

/**
 * Grants an asset to a citizen under a contract (master prompt §26).
 *
 * <p>The same use case issues a cemetery concession, a market stall assignment, a municipal land
 * lease and a public-space permit — that is the point of one {@link Agreement} model.
 *
 * <p>Granting does three things atomically: it writes the contract, marks the asset occupied, and
 * opens an {@link AssetHolder} period. Doing any one without the others would leave a stall that is
 * rented but shows as free, or a contract over a plot nobody is recorded as holding.
 */
public final class GrantAgreement implements UseCase<GrantAgreement.Command, Agreement> {

    private static final String SEQUENCE = "CONT";
    private static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();

    public record Command(
            Session session,
            String assetId,
            PartyRef holder,
            AgreementKind kind,
            LocalDate startDate,
            Optional<LocalDate> endDate,
            Money amount,
            Agreement.BillingFrequency billingFrequency,
            Optional<String> procedureId,
            String source) {}

    private final AssetRepository assets;
    private final NumberingRepository numbering;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public GrantAgreement(
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
        if (!c.session().can(Permission.DEPARTMENT_MANAGE)
                && !c.session().can(Permission.PROCEDURE_DECIDE)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }

        Optional<MunicipalAsset> found = assets.findById(c.assetId());
        if (found.isEmpty()) {
            return Result.err("ASSET_NOT_FOUND", "asset.not_found");
        }
        MunicipalAsset asset = found.get();
        if (!asset.kind().isGrantable()) {
            return Result.err("NOT_GRANTABLE", "asset.not_grantable");
        }
        if (!asset.canBeGranted()) {
            return Result.err("NOT_AVAILABLE", "asset.not_available");
        }
        if (c.endDate().isPresent() && c.endDate().get().isBefore(c.startDate())) {
            return Result.err("INVALID_TERM", "agreement.invalid_term");
        }
        if (c.amount().isNegative()) {
            return Result.err("INVALID_AMOUNT", "agreement.invalid_amount");
        }

        return Result.ok(unitOfWork.execute(() -> doGrant(c, asset)));
    }

    private Agreement doGrant(Command c, MunicipalAsset asset) {
        Instant now = clock.now();
        String code =
                numbering.allocate(
                        SEQUENCE, SEQUENCE, LocalDate.ofInstant(now, LOCAL_ZONE).getYear());

        Agreement agreement =
                new Agreement(
                        ids.newId(),
                        code,
                        c.kind(),
                        asset.id(),
                        c.procedureId().orElse(null),
                        c.holder(),
                        Agreement.Status.ACTIVE,
                        c.startDate(),
                        c.endDate().orElse(null),
                        true,
                        c.amount(),
                        c.billingFrequency(),
                        null,
                        null,
                        null,
                        null,
                        now,
                        now);
        assets.save(agreement);

        asset.setAvailability(Availability.OCCUPIED, now);
        assets.save(asset);

        // The holder period stays open. A contract's end date is when it is *due* to end, not
        // when the holding actually stopped: fixed-term leases run on until someone terminates,
        // transfers or renews them, and closing the period now would make every current holder
        // invisible the moment the contract was signed.
        assets.save(
                new AssetHolder(
                        ids.newId(),
                        asset.id(),
                        c.holder(),
                        roleFor(c.kind()),
                        java.util.OptionalInt.empty(),
                        c.startDate(),
                        Optional.empty(),
                        Optional.of(code),
                        now));

        audit.record(
                c.session().audit(c.source()),
                "agreement.granted",
                "Agreement",
                agreement.id(),
                asset.code(),
                code + " " + c.kind(),
                null);
        return agreement;
    }

    /** A lease makes a lessee, a concession makes a concessionaire; a permit is just occupancy. */
    private static HolderRole roleFor(AgreementKind kind) {
        return switch (kind) {
            case LEASE, STALL_ASSIGNMENT -> HolderRole.LESSEE;
            case CONCESSION -> HolderRole.CONCESSIONAIRE;
            case PUBLIC_SPACE_PERMIT, OTHER -> HolderRole.OCCUPANT;
        };
    }
}
