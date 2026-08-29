// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.sirmax.infrastructure.persistence.JdbcHelper.bool;
import static org.sirmax.infrastructure.persistence.JdbcHelper.instant;
import static org.sirmax.infrastructure.persistence.JdbcHelper.str;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sirmax.application.port.ProcedureQuery;
import org.sirmax.application.port.ProcedureRepository;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.common.PartyType;
import org.sirmax.domain.procedure.Priority;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.procedure.ProcedureAttachment;
import org.sirmax.domain.procedure.ProcedureEvent;
import org.sirmax.domain.procedure.ProcedureEventKind;
import org.sirmax.domain.procedure.ProcedureOutcome;
import org.sirmax.domain.procedure.ProcedureRequirementItem;
import org.sirmax.domain.procedure.ProcedureStatus;
import org.sirmax.domain.service.RequirementKind;
import org.sirmax.domain.service.RequirementStage;

/** SQLite persistence for cases, their checklist, form answers, timeline and attachments. */
public final class SqliteProcedureRepository implements ProcedureRepository {

    private final SqliteDatabase db;

    public SqliteProcedureRepository(SqliteDatabase db) {
        this.db = db;
    }

    @Override
    public void save(Procedure p) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO procedure"
                    + " (id, code, service_definition_id, service_version_id, applicant_type,"
                    + "  applicant_id, subject_type, subject_id, status, priority, current_step_key,"
                    + "  department_id, assigned_user_id, opened_at, due_date, closed_at, outcome,"
                    + "  outcome_reason, notes, created_at, updated_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT(id) DO UPDATE SET subject_type=excluded.subject_type,"
                    + " subject_id=excluded.subject_id, status=excluded.status,"
                    + " priority=excluded.priority, current_step_key=excluded.current_step_key,"
                    + " department_id=excluded.department_id,"
                    + " assigned_user_id=excluded.assigned_user_id, due_date=excluded.due_date,"
                    + " closed_at=excluded.closed_at, outcome=excluded.outcome,"
                    + " outcome_reason=excluded.outcome_reason, notes=excluded.notes,"
                    + " updated_at=excluded.updated_at",
                p.id(),
                p.code(),
                p.serviceDefinitionId(),
                p.serviceVersionId(),
                p.applicant().type().name(),
                p.applicant().id(),
                p.subjectType().orElse(null),
                p.subjectId().orElse(null),
                p.status().name(),
                p.priority().name(),
                p.currentStepKey().orElse(null),
                p.departmentId().orElse(null),
                p.assignedUserId().orElse(null),
                p.openedAt(),
                p.dueDate().map(LocalDate::toString).orElse(null),
                p.closedAt().orElse(null),
                p.outcome().map(Enum::name).orElse(null),
                p.outcomeReason().orElse(null),
                p.notes().orElse(null),
                p.createdAt(),
                p.updatedAt());
    }

    @Override
    public Optional<Procedure> findById(String id) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM procedure WHERE id = ?",
                SqliteProcedureRepository::mapProcedure,
                id);
    }

    @Override
    public Optional<Procedure> findByCode(String code) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM procedure WHERE code = ?",
                SqliteProcedureRepository::mapProcedure,
                code);
    }

    @Override
    public List<Procedure> findByApplicant(PartyRef applicant, int limit) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM procedure WHERE applicant_type = ? AND applicant_id = ?"
                        + " ORDER BY opened_at DESC LIMIT ?",
                SqliteProcedureRepository::mapProcedure,
                applicant.type().name(),
                applicant.id(),
                limit);
    }

    @Override
    public List<Procedure> search(ProcedureQuery query) {
        Where where = buildWhere(query);
        List<Object> params = new ArrayList<>(where.params());
        params.add(query.limit());
        params.add(query.offset());
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM procedure WHERE " + where.sql()
                        // most urgent first, then oldest due date, then oldest case
                        + " ORDER BY CASE priority WHEN 'URGENT' THEN 0 WHEN 'HIGH' THEN 1"
                        + " WHEN 'NORMAL' THEN 2 ELSE 3 END,"
                        + " CASE WHEN due_date IS NULL THEN 1 ELSE 0 END, due_date, opened_at"
                        + " LIMIT ? OFFSET ?",
                SqliteProcedureRepository::mapProcedure,
                params.toArray());
    }

    @Override
    public long countSearch(ProcedureQuery query) {
        Where where = buildWhere(query);
        return JdbcHelper.queryLong(
                db.connection(),
                "SELECT count(*) FROM procedure WHERE " + where.sql(),
                where.params().toArray());
    }

    /** A rendered SQL predicate plus its bound parameters, in matching order. */
    private record Where(String sql, List<Object> params) {}

    private static Where buildWhere(ProcedureQuery q) {
        List<String> clauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (q.statuses().isEmpty()) {
            clauses.add("status NOT IN ('REJECTED','DELIVERED','CLOSED','CANCELLED')");
        } else {
            clauses.add(
                    "status IN ("
                            + String.join(",", java.util.Collections.nCopies(q.statuses().size(), "?"))
                            + ")");
            q.statuses().forEach(s -> params.add(s.name()));
        }
        q.departmentId()
                .ifPresent(
                        d -> {
                            clauses.add("department_id = ?");
                            params.add(d);
                        });
        q.assignedUserId()
                .ifPresent(
                        u -> {
                            clauses.add("assigned_user_id = ?");
                            params.add(u);
                        });
        q.serviceDefinitionId()
                .ifPresent(
                        s -> {
                            clauses.add("service_definition_id = ?");
                            params.add(s);
                        });
        q.text()
                .ifPresent(
                        t -> {
                            clauses.add("code LIKE ?");
                            params.add("%" + t + "%");
                        });
        if (q.unassignedOnly()) {
            clauses.add("assigned_user_id IS NULL");
        }
        if (q.onlyOverdue()) {
            clauses.add("due_date IS NOT NULL AND due_date < date('now')");
        }
        return new Where(String.join(" AND ", clauses), params);
    }

    // ── checklist ──

    @Override
    public void saveRequirement(ProcedureRequirementItem item) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO procedure_requirement"
                    + " (id, procedure_id, requirement_key, label, kind, stage, required,"
                    + "  condition_expression, satisfied, waived, note, satisfied_at, satisfied_by)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT(procedure_id, requirement_key) DO UPDATE SET"
                    + " satisfied=excluded.satisfied, waived=excluded.waived, note=excluded.note,"
                    + " satisfied_at=excluded.satisfied_at, satisfied_by=excluded.satisfied_by",
                item.id(),
                item.procedureId(),
                item.requirementKey(),
                item.label(),
                item.kind().name(),
                item.stage().name(),
                item.required(),
                item.conditionExpression().orElse(null),
                item.isWaived() ? false : item.isSatisfied(),
                item.isWaived(),
                item.note().orElse(null),
                item.satisfiedAt().orElse(null),
                item.satisfiedBy().orElse(null));
    }

    @Override
    public List<ProcedureRequirementItem> findRequirements(String procedureId) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM procedure_requirement WHERE procedure_id = ? ORDER BY rowid",
                SqliteProcedureRepository::mapRequirement,
                procedureId);
    }

    @Override
    public Optional<ProcedureRequirementItem> findRequirement(String procedureId, String key) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM procedure_requirement WHERE procedure_id = ? AND requirement_key = ?",
                SqliteProcedureRepository::mapRequirement,
                procedureId,
                key);
    }

    // ── dynamic form answers ──

    @Override
    public void saveFormValues(String procedureId, Map<String, String> values) {
        for (Map.Entry<String, String> e : values.entrySet()) {
            JdbcHelper.update(
                    db.connection(),
                    "INSERT INTO procedure_form_value (procedure_id, field_key, value_text)"
                            + " VALUES (?, ?, ?)"
                            + " ON CONFLICT(procedure_id, field_key) DO UPDATE SET"
                            + " value_text=excluded.value_text",
                    procedureId,
                    e.getKey(),
                    e.getValue());
        }
    }

    @Override
    public Map<String, String> findFormValues(String procedureId) {
        Map<String, String> out = new LinkedHashMap<>();
        JdbcHelper.queryList(
                        db.connection(),
                        "SELECT field_key, value_text FROM procedure_form_value"
                                + " WHERE procedure_id = ? ORDER BY field_key",
                        rs -> Map.entry(rs.getString(1), Optional.ofNullable(rs.getString(2)).orElse("")),
                        procedureId)
                .forEach(e -> out.put(e.getKey(), e.getValue()));
        return out;
    }

    // ── timeline ──

    @Override
    public void appendEvent(ProcedureEvent e) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO procedure_event"
                        + " (id, procedure_id, occurred_at, actor_user_id, kind, from_step_key,"
                        + "  to_step_key, detail)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                e.id(),
                e.procedureId(),
                e.occurredAt(),
                e.actorUserId().orElse(null),
                e.kind().name(),
                e.fromStepKey().orElse(null),
                e.toStepKey().orElse(null),
                e.detail().orElse(null));
    }

    @Override
    public List<ProcedureEvent> findEvents(String procedureId) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM procedure_event WHERE procedure_id = ? ORDER BY occurred_at, rowid",
                SqliteProcedureRepository::mapEvent,
                procedureId);
    }

    // ── attachments ──

    @Override
    public void saveAttachment(ProcedureAttachment a) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO procedure_attachment"
                        + " (id, procedure_id, requirement_key, file_name, content_type, size_bytes,"
                        + "  storage_path, sha256, uploaded_at, uploaded_by)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        + " ON CONFLICT(id) DO UPDATE SET sha256=excluded.sha256,"
                        + " size_bytes=excluded.size_bytes",
                a.id(),
                a.procedureId(),
                a.requirementKey().orElse(null),
                a.fileName(),
                a.contentType().orElse(null),
                a.sizeBytes(),
                a.storagePath(),
                a.sha256().orElse(null),
                a.uploadedAt(),
                a.uploadedBy().orElse(null));
    }

    @Override
    public List<ProcedureAttachment> findAttachments(String procedureId) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM procedure_attachment WHERE procedure_id = ? ORDER BY uploaded_at",
                SqliteProcedureRepository::mapAttachment,
                procedureId);
    }

    // ── row mappers ──

    private static Procedure mapProcedure(ResultSet rs) throws SQLException {
        String due = str(rs, "due_date");
        String outcome = str(rs, "outcome");
        return new Procedure(
                rs.getString("id"),
                rs.getString("code"),
                rs.getString("service_definition_id"),
                rs.getString("service_version_id"),
                new PartyRef(
                        PartyType.valueOf(rs.getString("applicant_type")),
                        rs.getString("applicant_id")),
                str(rs, "subject_type"),
                str(rs, "subject_id"),
                ProcedureStatus.valueOf(rs.getString("status")),
                Priority.valueOf(rs.getString("priority")),
                str(rs, "current_step_key"),
                str(rs, "department_id"),
                str(rs, "assigned_user_id"),
                instant(rs, "opened_at"),
                due == null ? null : LocalDate.parse(due),
                instant(rs, "closed_at"),
                outcome == null ? null : ProcedureOutcome.valueOf(outcome),
                str(rs, "outcome_reason"),
                str(rs, "notes"),
                instant(rs, "created_at"),
                instant(rs, "updated_at"));
    }

    private static ProcedureRequirementItem mapRequirement(ResultSet rs) throws SQLException {
        return new ProcedureRequirementItem(
                rs.getString("id"),
                rs.getString("procedure_id"),
                rs.getString("requirement_key"),
                rs.getString("label"),
                RequirementKind.valueOf(rs.getString("kind")),
                RequirementStage.valueOf(rs.getString("stage")),
                bool(rs, "required"),
                str(rs, "condition_expression"),
                bool(rs, "satisfied"),
                bool(rs, "waived"),
                str(rs, "note"),
                instant(rs, "satisfied_at"),
                str(rs, "satisfied_by"));
    }

    private static ProcedureEvent mapEvent(ResultSet rs) throws SQLException {
        return new ProcedureEvent(
                rs.getString("id"),
                rs.getString("procedure_id"),
                instant(rs, "occurred_at"),
                Optional.ofNullable(str(rs, "actor_user_id")),
                ProcedureEventKind.valueOf(rs.getString("kind")),
                Optional.ofNullable(str(rs, "from_step_key")),
                Optional.ofNullable(str(rs, "to_step_key")),
                Optional.ofNullable(str(rs, "detail")));
    }

    private static ProcedureAttachment mapAttachment(ResultSet rs) throws SQLException {
        return new ProcedureAttachment(
                rs.getString("id"),
                rs.getString("procedure_id"),
                Optional.ofNullable(str(rs, "requirement_key")),
                rs.getString("file_name"),
                Optional.ofNullable(str(rs, "content_type")),
                rs.getLong("size_bytes"),
                rs.getString("storage_path"),
                Optional.ofNullable(str(rs, "sha256")),
                instant(rs, "uploaded_at"),
                Optional.ofNullable(str(rs, "uploaded_by")));
    }
}
