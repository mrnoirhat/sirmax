// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.util.List;
import java.util.Optional;
import org.sirmax.domain.asset.Agreement;
import org.sirmax.domain.asset.AssetHolder;
import org.sirmax.domain.asset.AssetKind;
import org.sirmax.domain.asset.Availability;
import org.sirmax.domain.asset.MunicipalAsset;
import org.sirmax.domain.common.PartyRef;

/**
 * Persistence for municipal assets, who holds them, and the agreements over them (master prompt
 * §25, §26).
 *
 * <p>One port for all of it, because the three are read together everywhere they are read at all:
 * an asset's file is the asset plus its holders plus its contracts.
 */
public interface AssetRepository {

    // ── assets ──

    void save(MunicipalAsset asset);

    Optional<MunicipalAsset> findById(String id);

    Optional<MunicipalAsset> findByCode(AssetKind kind, String code);

    /** Assets of a kind, optionally restricted to an availability and/or a container. */
    List<MunicipalAsset> search(
            Optional<AssetKind> kind,
            Optional<Availability> availability,
            Optional<String> parentId,
            Optional<String> text,
            int limit,
            int offset);

    /** The assets directly contained by {@code parentId} — a cemetery's sections, a market's stalls. */
    List<MunicipalAsset> childrenOf(String parentId);

    // ── holders ──

    void save(AssetHolder holder);

    /** Every recorded relationship for an asset, oldest first — the asset's ownership history. */
    List<AssetHolder> holdersOf(String assetId);

    /** Relationships still in force. */
    List<AssetHolder> currentHoldersOf(String assetId);

    /** Everything a party holds, in any role — the citizen file's "sus propiedades" panel. */
    List<AssetHolder> heldBy(PartyRef party);

    // ── agreements ──

    void save(Agreement agreement);

    Optional<Agreement> findAgreementById(String id);

    Optional<Agreement> findAgreementByCode(String code);

    List<Agreement> agreementsFor(String assetId);

    List<Agreement> agreementsHeldBy(PartyRef holder, int limit);

    /** Agreements in the given statuses, newest first; empty {@code statuses} means "any". */
    List<Agreement> listAgreements(List<Agreement.Status> statuses, int limit, int offset);

    /**
     * Active agreements whose end date has already passed — what the expiry sweep and the arrears
     * report both need (§26, §30).
     */
    List<Agreement> lapsedAgreements(java.time.LocalDate asOf, int limit);
}
