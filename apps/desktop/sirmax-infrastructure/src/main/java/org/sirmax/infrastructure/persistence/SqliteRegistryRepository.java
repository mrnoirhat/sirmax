// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.sirmax.infrastructure.persistence.JdbcHelper.instant;
import static org.sirmax.infrastructure.persistence.JdbcHelper.str;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sirmax.application.port.RegistryRepository;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.common.PartyType;
import org.sirmax.domain.registry.Decision;
import org.sirmax.domain.registry.Inspection;
import org.sirmax.domain.registry.RegisteredDocument;
import org.sirmax.shared.text.Normalization;

/** SQLite persistence for the municipal register, inspections and decisions. */
public final class SqliteRegistryRepository implements RegistryRepository {

    private final SqliteDatabase db;
    private final RegistryJson json;

    public SqliteRegistryRepository(SqliteDatabase db) {
        this.db = db;
        this.json = new RegistryJson();
    }

    // ── registered documents ──

    @Override
    public void save(RegisteredDocument d) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO registered_document"
                    + " (id, registration_number, document_type, title, procedure_id, document_date,"
                    + "  presented_at, registered_at, book, volume, folio, status,"
                    + "  related_asset_id, storage_path, sha256, notes, created_at, updated_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT(id) DO UPDATE SET document_type=excluded.document_type,"
                    + " title=excluded.title, document_date=excluded.document_date,"
                    + " registered_at=excluded.registered_at, book=excluded.book,"
                    + " volume=excluded.volume, folio=excluded.folio, status=excluded.status,"
                    + " related_asset_id=excluded.related_asset_id,"
                    + " storage_path=excluded.storage_path, sha256=excluded.sha256,"
                    + " notes=excluded.notes, updated_at=excluded.updated_at",
                d.id(),
                d.registrationNumber(),
                d.documentType(),
                d.title(),
                d.procedureId().orElse(null),
                d.documentDate().map(LocalDate::toString).orElse(null),
                d.presentedAt(),
                d.registeredAt().orElse(null),
                d.book().orElse(null),
                d.volume().orElse(null),
                d.folio().orElse(null),
                d.status().name(),
                d.relatedAssetId().orElse(null),
                d.storagePath().orElse(null),
                d.sha256().orElse(null),
                d.notes().orElse(null),
                d.createdAt(),
                d.updatedAt());

        // Parties are replaced wholesale; they are only editable before registration anyway.
        JdbcHelper.update(
                db.connection(),
                "DELETE FROM registered_document_party WHERE document_id = ?",
                d.id());
        for (RegisteredDocument.Party party : d.parties()) {
            JdbcHelper.update(
                    db.connection(),
                    "INSERT INTO registered_document_party"
                            + " (id, document_id, party_type, party_id, role)"
                            + " VALUES (?, ?, ?, ?, ?)",
                    party.id(),
                    party.documentId(),
                    party.party().type().name(),
                    party.party().id(),
                    party.role());
        }
        // Annotations are append-only: never deleted, only inserted if new.
        for (RegisteredDocument.Annotation note : d.annotations()) {
            JdbcHelper.update(
                    db.connection(),
                    "INSERT OR IGNORE INTO registered_document_annotation"
                            + " (id, document_id, text, annotated_by, annotated_at)"
                            + " VALUES (?, ?, ?, ?, ?)",
                    note.id(),
                    note.documentId(),
                    note.text(),
                    note.annotatedBy().orElse(null),
                    note.annotatedAt());
        }
    }

    @Override
    public Optional<RegisteredDocument> findDocumentById(String id) {
        return JdbcHelper.queryOne(
                        db.connection(),
                        "SELECT * FROM registered_document WHERE id = ?",
                        SqliteRegistryRepository::mapDocument,
                        id)
                .map(this::withChildren);
    }

    @Override
    public Optional<RegisteredDocument> findDocumentByNumber(String registrationNumber) {
        return JdbcHelper.queryOne(
                        db.connection(),
                        "SELECT * FROM registered_document WHERE registration_number = ?",
                        SqliteRegistryRepository::mapDocument,
                        registrationNumber)
                .map(this::withChildren);
    }

    @Override
    public List<RegisteredDocument> searchDocuments(
            Optional<String> text,
            Optional<String> documentType,
            Optional<RegisteredDocument.Status> status,
            int limit,
            int offset) {
        List<String> clauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        clauses.add("1 = 1");

        text.ifPresent(
                t -> {
                    clauses.add(
                            "(lower(title) LIKE ? OR lower(registration_number) LIKE ?"
                                    + " OR lower(coalesce(folio, '')) LIKE ?)");
                    String like = "%" + Normalization.fold(t) + "%";
                    params.add(like);
                    params.add(like);
                    params.add(like);
                });
        documentType.ifPresent(
                t -> {
                    clauses.add("document_type = ?");
                    params.add(t);
                });
        status.ifPresent(
                s -> {
                    clauses.add("status = ?");
                    params.add(s.name());
                });
        params.add(limit);
        params.add(offset);

        return JdbcHelper.queryList(
                        db.connection(),
                        "SELECT * FROM registered_document WHERE "
                                + String.join(" AND ", clauses)
                                + " ORDER BY presented_at DESC LIMIT ? OFFSET ?",
                        SqliteRegistryRepository::mapDocument,
                        params.toArray())
                .stream()
                .map(this::withChildren)
                .toList();
    }

    @Override
    public List<RegisteredDocument> documentsNaming(PartyRef party, int limit) {
        return JdbcHelper.queryList(
                        db.connection(),
                        "SELECT d.* FROM registered_document d"
                                + " JOIN registered_document_party p ON p.document_id = d.id"
                                + " WHERE p.party_type = ? AND p.party_id = ?"
                                + " GROUP BY d.id ORDER BY d.presented_at DESC LIMIT ?",
                        SqliteRegistryRepository::mapDocument,
                        party.type().name(),
                        party.id(),
                        limit)
                .stream()
                .map(this::withChildren)
                .toList();
    }

    @Override
    public List<RegisteredDocument> documentsForAsset(String assetId) {
        return JdbcHelper.queryList(
                        db.connection(),
                        "SELECT * FROM registered_document WHERE related_asset_id = ?"
                                + " ORDER BY presented_at DESC",
                        SqliteRegistryRepository::mapDocument,
                        assetId)
                .stream()
                .map(this::withChildren)
                .toList();
    }

    private RegisteredDocument withChildren(RegisteredDocument document) {
        document.restoreParties(
                JdbcHelper.queryList(
                        db.connection(),
                        "SELECT * FROM registered_document_party WHERE document_id = ?"
                                + " ORDER BY rowid",
                        SqliteRegistryRepository::mapParty,
                        document.id()));
        document.restoreAnnotations(
                JdbcHelper.queryList(
                        db.connection(),
                        "SELECT * FROM registered_document_annotation WHERE document_id = ?"
                                + " ORDER BY annotated_at, rowid",
                        SqliteRegistryRepository::mapAnnotation,
                        document.id()));
        return document;
    }

    // ── inspections ──

    @Override
    public void save(Inspection i) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO inspection"
                    + " (id, code, procedure_id, asset_id, inspector_user_id, status, result,"
                    + "  scheduled_date, performed_at, location, findings, checklist_json,"
                    + "  follow_up_date, created_at, updated_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT(id) DO UPDATE SET asset_id=excluded.asset_id,"
                    + " inspector_user_id=excluded.inspector_user_id, status=excluded.status,"
                    + " result=excluded.result, scheduled_date=excluded.scheduled_date,"
                    + " performed_at=excluded.performed_at, location=excluded.location,"
                    + " findings=excluded.findings, checklist_json=excluded.checklist_json,"
                    + " follow_up_date=excluded.follow_up_date, updated_at=excluded.updated_at",
                i.id(),
                i.code(),
                i.procedureId(),
                i.assetId().orElse(null),
                i.inspectorUserId().orElse(null),
                i.status().name(),
                i.result().map(Enum::name).orElse(null),
                i.scheduledDate().map(LocalDate::toString).orElse(null),
                i.performedAt().orElse(null),
                i.location().orElse(null),
                i.findings().orElse(null),
                json.checklistToJson(i.checklist()),
                i.followUpDate().map(LocalDate::toString).orElse(null),
                i.createdAt(),
                i.updatedAt());
    }

    @Override
    public Optional<Inspection> findInspectionById(String id) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM inspection WHERE id = ?",
                this::mapInspection,
                id);
    }

    @Override
    public List<Inspection> inspectionsFor(String procedureId) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM inspection WHERE procedure_id = ? ORDER BY created_at",
                this::mapInspection,
                procedureId);
    }

    @Override
    public List<Inspection> inspectionsAssignedTo(String userId, int limit) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM inspection WHERE inspector_user_id = ?"
                        + " AND status IN ('SCHEDULED','IN_PROGRESS')"
                        + " ORDER BY CASE WHEN scheduled_date IS NULL THEN 1 ELSE 0 END,"
                        + " scheduled_date LIMIT ?",
                this::mapInspection,
                userId,
                limit);
    }

    @Override
    public List<Inspection> overdueInspections(LocalDate asOf, int limit) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM inspection WHERE status IN ('SCHEDULED','IN_PROGRESS')"
                        + " AND scheduled_date IS NOT NULL AND scheduled_date < ?"
                        + " ORDER BY scheduled_date LIMIT ?",
                this::mapInspection,
                asOf.toString(),
                limit);
    }

    // ── decisions ──

    @Override
    public void save(Decision d) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO decision"
                        + " (id, procedure_id, step_key, outcome, decided_by, decided_by_role,"
                        + "  decided_at, reason, comments, conditions, document_id)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                d.id(),
                d.procedureId(),
                d.stepKey().orElse(null),
                d.outcome().name(),
                d.decidedBy().orElse(null),
                d.decidedByRole().orElse(null),
                d.decidedAt(),
                d.reason().orElse(null),
                d.comments().orElse(null),
                d.conditions().orElse(null),
                d.documentId().orElse(null));
    }

    @Override
    public List<Decision> decisionsFor(String procedureId) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM decision WHERE procedure_id = ? ORDER BY decided_at, rowid",
                SqliteRegistryRepository::mapDecision,
                procedureId);
    }

    // ── row mappers ──

    private static RegisteredDocument mapDocument(ResultSet rs) throws SQLException {
        String documentDate = str(rs, "document_date");
        return new RegisteredDocument(
                rs.getString("id"),
                rs.getString("registration_number"),
                rs.getString("document_type"),
                rs.getString("title"),
                str(rs, "procedure_id"),
                documentDate == null ? null : LocalDate.parse(documentDate),
                instant(rs, "presented_at"),
                instant(rs, "registered_at"),
                str(rs, "book"),
                str(rs, "volume"),
                str(rs, "folio"),
                RegisteredDocument.Status.valueOf(rs.getString("status")),
                str(rs, "related_asset_id"),
                str(rs, "storage_path"),
                str(rs, "sha256"),
                str(rs, "notes"),
                instant(rs, "created_at"),
                instant(rs, "updated_at"));
    }

    private static RegisteredDocument.Party mapParty(ResultSet rs) throws SQLException {
        return new RegisteredDocument.Party(
                rs.getString("id"),
                rs.getString("document_id"),
                new PartyRef(
                        PartyType.valueOf(rs.getString("party_type")), rs.getString("party_id")),
                rs.getString("role"));
    }

    private static RegisteredDocument.Annotation mapAnnotation(ResultSet rs) throws SQLException {
        return new RegisteredDocument.Annotation(
                rs.getString("id"),
                rs.getString("document_id"),
                rs.getString("text"),
                Optional.ofNullable(str(rs, "annotated_by")),
                instant(rs, "annotated_at"));
    }

    private Inspection mapInspection(ResultSet rs) throws SQLException {
        String scheduled = str(rs, "scheduled_date");
        String followUp = str(rs, "follow_up_date");
        String result = str(rs, "result");
        return new Inspection(
                rs.getString("id"),
                rs.getString("code"),
                rs.getString("procedure_id"),
                str(rs, "asset_id"),
                str(rs, "inspector_user_id"),
                Inspection.Status.valueOf(rs.getString("status")),
                result == null ? null : Inspection.Result.valueOf(result),
                scheduled == null ? null : LocalDate.parse(scheduled),
                instant(rs, "performed_at"),
                str(rs, "location"),
                str(rs, "findings"),
                json.checklistFromJson(rs.getString("checklist_json")),
                followUp == null ? null : LocalDate.parse(followUp),
                instant(rs, "created_at"),
                instant(rs, "updated_at"));
    }

    private static Decision mapDecision(ResultSet rs) throws SQLException {
        return new Decision(
                rs.getString("id"),
                rs.getString("procedure_id"),
                Optional.ofNullable(str(rs, "step_key")),
                Decision.Outcome.valueOf(rs.getString("outcome")),
                Optional.ofNullable(str(rs, "decided_by")),
                Optional.ofNullable(str(rs, "decided_by_role")),
                instant(rs, "decided_at"),
                Optional.ofNullable(str(rs, "reason")),
                Optional.ofNullable(str(rs, "comments")),
                Optional.ofNullable(str(rs, "conditions")),
                Optional.ofNullable(str(rs, "document_id")));
    }
}
