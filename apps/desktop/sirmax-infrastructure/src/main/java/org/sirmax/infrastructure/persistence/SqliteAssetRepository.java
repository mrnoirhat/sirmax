// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.sirmax.infrastructure.persistence.JdbcHelper.bool;
import static org.sirmax.infrastructure.persistence.JdbcHelper.instant;
import static org.sirmax.infrastructure.persistence.JdbcHelper.str;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.sirmax.application.port.AssetRepository;
import org.sirmax.domain.asset.Agreement;
import org.sirmax.domain.asset.AgreementKind;
import org.sirmax.domain.asset.AssetHolder;
import org.sirmax.domain.asset.AssetKind;
import org.sirmax.domain.asset.Availability;
import org.sirmax.domain.asset.HolderRole;
import org.sirmax.domain.asset.MunicipalAsset;
import org.sirmax.domain.common.ArchiveStatus;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.common.PartyType;
import org.sirmax.shared.JsonDoc;
import org.sirmax.shared.Money;
import org.sirmax.shared.text.Normalization;

/** SQLite persistence for municipal assets, their holders and the agreements over them. */
public final class SqliteAssetRepository implements AssetRepository {

    private final SqliteDatabase db;

    public SqliteAssetRepository(SqliteDatabase db) {
        this.db = db;
    }

    // ── assets ──

    @Override
    public void save(MunicipalAsset a) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO municipal_asset"
                    + " (id, code, kind, name, parent_id, address_line, sector, municipality,"
                    + "  province, latitude, longitude, area_sq_m, municipally_owned,"
                    + "  attributes_json, availability, archive_status, notes, created_at, updated_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT(id) DO UPDATE SET name=excluded.name,"
                    + " parent_id=excluded.parent_id, address_line=excluded.address_line,"
                    + " sector=excluded.sector, municipality=excluded.municipality,"
                    + " province=excluded.province, latitude=excluded.latitude,"
                    + " longitude=excluded.longitude, area_sq_m=excluded.area_sq_m,"
                    + " municipally_owned=excluded.municipally_owned,"
                    + " attributes_json=excluded.attributes_json,"
                    + " availability=excluded.availability, archive_status=excluded.archive_status,"
                    + " notes=excluded.notes, updated_at=excluded.updated_at",
                a.id(),
                a.code(),
                a.kind().name(),
                a.name(),
                a.parentId().orElse(null),
                a.addressLine().orElse(null),
                a.sector().orElse(null),
                a.municipality().orElse(null),
                a.province().orElse(null),
                a.latitude().orElse(null),
                a.longitude().orElse(null),
                a.areaSqM().isPresent() ? a.areaSqM().getAsLong() : null,
                a.municipallyOwned(),
                a.attributes().value(),
                a.availability().name(),
                a.archiveStatus().name(),
                a.notes().orElse(null),
                a.createdAt(),
                a.updatedAt());
    }

    @Override
    public Optional<MunicipalAsset> findById(String id) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM municipal_asset WHERE id = ?",
                SqliteAssetRepository::mapAsset,
                id);
    }

    @Override
    public Optional<MunicipalAsset> findByCode(AssetKind kind, String code) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM municipal_asset WHERE kind = ? AND lower(code) = lower(?)",
                SqliteAssetRepository::mapAsset,
                kind.name(),
                code);
    }

    @Override
    public List<MunicipalAsset> search(
            Optional<AssetKind> kind,
            Optional<Availability> availability,
            Optional<String> parentId,
            Optional<String> text,
            int limit,
            int offset) {
        List<String> clauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        clauses.add("archive_status = 'ACTIVE'");

        kind.ifPresent(
                k -> {
                    clauses.add("kind = ?");
                    params.add(k.name());
                });
        availability.ifPresent(
                v -> {
                    clauses.add("availability = ?");
                    params.add(v.name());
                });
        parentId.ifPresent(
                p -> {
                    clauses.add("parent_id = ?");
                    params.add(p);
                });
        text.ifPresent(
                t -> {
                    // Names carry accents ("Sección Ángel"); fold both sides so the operator can
                    // type without them, exactly as citizen search does.
                    clauses.add("(lower(code) LIKE ? OR lower(name) LIKE ?)");
                    String like = "%" + Normalization.fold(t) + "%";
                    params.add(like);
                    params.add(like);
                });
        params.add(limit);
        params.add(offset);

        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM municipal_asset WHERE " + String.join(" AND ", clauses)
                        + " ORDER BY kind, code LIMIT ? OFFSET ?",
                SqliteAssetRepository::mapAsset,
                params.toArray());
    }

    @Override
    public List<MunicipalAsset> childrenOf(String parentId) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM municipal_asset WHERE parent_id = ? AND archive_status = 'ACTIVE'"
                        + " ORDER BY code",
                SqliteAssetRepository::mapAsset,
                parentId);
    }

    // ── holders ──

    @Override
    public void save(AssetHolder h) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO asset_holder"
                        + " (id, asset_id, party_type, party_id, role, share_percent, from_date,"
                        + "  to_date, legal_reference, created_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        + " ON CONFLICT(id) DO UPDATE SET to_date=excluded.to_date,"
                        + " legal_reference=excluded.legal_reference",
                h.id(),
                h.assetId(),
                h.party().type().name(),
                h.party().id(),
                h.role().name(),
                h.sharePercent().isPresent() ? h.sharePercent().getAsInt() : null,
                h.fromDate().toString(),
                h.toDate().map(LocalDate::toString).orElse(null),
                h.legalReference().orElse(null),
                h.createdAt());
    }

    @Override
    public List<AssetHolder> holdersOf(String assetId) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM asset_holder WHERE asset_id = ? ORDER BY from_date, rowid",
                SqliteAssetRepository::mapHolder,
                assetId);
    }

    @Override
    public List<AssetHolder> currentHoldersOf(String assetId) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM asset_holder WHERE asset_id = ? AND to_date IS NULL"
                        + " ORDER BY from_date",
                SqliteAssetRepository::mapHolder,
                assetId);
    }

    @Override
    public List<AssetHolder> heldBy(PartyRef party) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM asset_holder WHERE party_type = ? AND party_id = ?"
                        + " ORDER BY from_date DESC",
                SqliteAssetRepository::mapHolder,
                party.type().name(),
                party.id());
    }

    // ── agreements ──

    @Override
    public void save(Agreement a) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO agreement"
                    + " (id, code, kind, asset_id, procedure_id, holder_type, holder_id, status,"
                    + "  start_date, end_date, renewable, currency, amount_minor, billing_frequency,"
                    + "  transferred_from_id, terminated_at, termination_reason, notes, created_at,"
                    + "  updated_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT(id) DO UPDATE SET holder_type=excluded.holder_type,"
                    + " holder_id=excluded.holder_id, status=excluded.status,"
                    + " start_date=excluded.start_date, end_date=excluded.end_date,"
                    + " renewable=excluded.renewable, amount_minor=excluded.amount_minor,"
                    + " billing_frequency=excluded.billing_frequency,"
                    + " terminated_at=excluded.terminated_at,"
                    + " termination_reason=excluded.termination_reason, notes=excluded.notes,"
                    + " updated_at=excluded.updated_at",
                a.id(),
                a.code(),
                a.kind().name(),
                a.assetId().orElse(null),
                a.procedureId().orElse(null),
                a.holder().type().name(),
                a.holder().id(),
                a.status().name(),
                a.startDate().toString(),
                a.endDate().map(LocalDate::toString).orElse(null),
                a.renewable(),
                a.amount().currency().getCurrencyCode(),
                a.amount().minorUnits(),
                a.billingFrequency().name(),
                a.transferredFromId().orElse(null),
                a.terminatedAt().orElse(null),
                a.terminationReason().orElse(null),
                a.notes().orElse(null),
                a.createdAt(),
                a.updatedAt());
    }

    @Override
    public Optional<Agreement> findAgreementById(String id) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM agreement WHERE id = ?",
                SqliteAssetRepository::mapAgreement,
                id);
    }

    @Override
    public Optional<Agreement> findAgreementByCode(String code) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM agreement WHERE code = ?",
                SqliteAssetRepository::mapAgreement,
                code);
    }

    @Override
    public List<Agreement> agreementsFor(String assetId) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM agreement WHERE asset_id = ? ORDER BY start_date DESC",
                SqliteAssetRepository::mapAgreement,
                assetId);
    }

    @Override
    public List<Agreement> agreementsHeldBy(PartyRef holder, int limit) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM agreement WHERE holder_type = ? AND holder_id = ?"
                        + " ORDER BY start_date DESC LIMIT ?",
                SqliteAssetRepository::mapAgreement,
                holder.type().name(),
                holder.id(),
                limit);
    }

    @Override
    public List<Agreement> listAgreements(List<Agreement.Status> statuses, int limit, int offset) {
        if (statuses == null || statuses.isEmpty()) {
            return JdbcHelper.queryList(
                    db.connection(),
                    "SELECT * FROM agreement ORDER BY created_at DESC LIMIT ? OFFSET ?",
                    SqliteAssetRepository::mapAgreement,
                    limit,
                    offset);
        }
        List<Object> params = new ArrayList<>(statuses.stream().map(Enum::name).toList());
        params.add(limit);
        params.add(offset);
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM agreement WHERE status IN ("
                        + String.join(",", java.util.Collections.nCopies(statuses.size(), "?"))
                        + ") ORDER BY created_at DESC LIMIT ? OFFSET ?",
                SqliteAssetRepository::mapAgreement,
                params.toArray());
    }

    @Override
    public List<Agreement> lapsedAgreements(LocalDate asOf, int limit) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM agreement WHERE status = 'ACTIVE' AND end_date IS NOT NULL"
                        + " AND end_date < ? ORDER BY end_date LIMIT ?",
                SqliteAssetRepository::mapAgreement,
                asOf.toString(),
                limit);
    }

    // ── row mappers ──

    private static MunicipalAsset mapAsset(ResultSet rs) throws SQLException {
        Double latitude = rs.getObject("latitude") == null ? null : rs.getDouble("latitude");
        Double longitude = rs.getObject("longitude") == null ? null : rs.getDouble("longitude");
        Long area = rs.getObject("area_sq_m") == null ? null : rs.getLong("area_sq_m");
        return new MunicipalAsset(
                rs.getString("id"),
                rs.getString("code"),
                AssetKind.valueOf(rs.getString("kind")),
                rs.getString("name"),
                str(rs, "parent_id"),
                str(rs, "address_line"),
                str(rs, "sector"),
                str(rs, "municipality"),
                str(rs, "province"),
                latitude,
                longitude,
                area,
                bool(rs, "municipally_owned"),
                JsonDoc.of(rs.getString("attributes_json")),
                Availability.valueOf(rs.getString("availability")),
                ArchiveStatus.valueOf(rs.getString("archive_status")),
                str(rs, "notes"),
                instant(rs, "created_at"),
                instant(rs, "updated_at"));
    }

    private static AssetHolder mapHolder(ResultSet rs) throws SQLException {
        String to = str(rs, "to_date");
        Integer share =
                rs.getObject("share_percent") == null ? null : rs.getInt("share_percent");
        return new AssetHolder(
                rs.getString("id"),
                rs.getString("asset_id"),
                new PartyRef(
                        PartyType.valueOf(rs.getString("party_type")), rs.getString("party_id")),
                HolderRole.valueOf(rs.getString("role")),
                share == null ? OptionalInt.empty() : OptionalInt.of(share),
                LocalDate.parse(rs.getString("from_date")),
                Optional.ofNullable(to).map(LocalDate::parse),
                Optional.ofNullable(str(rs, "legal_reference")),
                instant(rs, "created_at"));
    }

    private static Agreement mapAgreement(ResultSet rs) throws SQLException {
        String end = str(rs, "end_date");
        Currency currency = Currency.getInstance(rs.getString("currency"));
        return new Agreement(
                rs.getString("id"),
                rs.getString("code"),
                AgreementKind.valueOf(rs.getString("kind")),
                str(rs, "asset_id"),
                str(rs, "procedure_id"),
                new PartyRef(
                        PartyType.valueOf(rs.getString("holder_type")), rs.getString("holder_id")),
                Agreement.Status.valueOf(rs.getString("status")),
                LocalDate.parse(rs.getString("start_date")),
                end == null ? null : LocalDate.parse(end),
                bool(rs, "renewable"),
                new Money(rs.getLong("amount_minor"), currency),
                Agreement.BillingFrequency.valueOf(rs.getString("billing_frequency")),
                str(rs, "transferred_from_id"),
                instant(rs, "terminated_at"),
                str(rs, "termination_reason"),
                str(rs, "notes"),
                instant(rs, "created_at"),
                instant(rs, "updated_at"));
    }
}
