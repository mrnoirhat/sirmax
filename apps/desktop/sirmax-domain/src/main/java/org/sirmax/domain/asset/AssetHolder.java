// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.asset;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import org.sirmax.domain.common.PartyRef;

/**
 * A party's relationship to an asset over a period (master prompt §25).
 *
 * <p>Recorded as a history rather than a current-owner column: a parcel's file has to answer "who
 * held this in 2019", which a mutable owner field cannot. A row with an open {@code toDate} is
 * current; closing it is how a transfer is recorded, never an edit.
 *
 * @param sharePercent for co-ownership; empty when the role is exclusive or the share is unstated
 */
public record AssetHolder(
        String id,
        String assetId,
        PartyRef party,
        HolderRole role,
        OptionalInt sharePercent,
        LocalDate fromDate,
        Optional<LocalDate> toDate,
        Optional<String> legalReference,
        Instant createdAt) {

    public AssetHolder {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(assetId, "assetId");
        Objects.requireNonNull(party, "party");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(fromDate, "fromDate");
        Objects.requireNonNull(createdAt, "createdAt");
        sharePercent = sharePercent == null ? OptionalInt.empty() : sharePercent;
        toDate = toDate == null ? Optional.empty() : toDate;
        legalReference = legalReference == null ? Optional.empty() : legalReference;
        if (sharePercent.isPresent()
                && (sharePercent.getAsInt() < 1 || sharePercent.getAsInt() > 100)) {
            throw new IllegalArgumentException("sharePercent must be between 1 and 100");
        }
        if (toDate.isPresent() && toDate.get().isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate must not precede fromDate");
        }
    }

    public static AssetHolder current(
            String id, String assetId, PartyRef party, HolderRole role, LocalDate from, Instant now) {
        return new AssetHolder(
                id,
                assetId,
                party,
                role,
                OptionalInt.empty(),
                from,
                Optional.empty(),
                Optional.empty(),
                now);
    }

    /** {@code true} while this relationship is still in force. */
    public boolean isCurrent() {
        return toDate.isEmpty();
    }

    public boolean wasHeldOn(LocalDate date) {
        return !date.isBefore(fromDate) && toDate.map(end -> !date.isAfter(end)).orElse(true);
    }

    /** Close the period — how a transfer, an inheritance or a termination is recorded. */
    public AssetHolder endedOn(LocalDate end) {
        return new AssetHolder(
                id, assetId, party, role, sharePercent, fromDate, Optional.of(end), legalReference,
                createdAt);
    }
}
