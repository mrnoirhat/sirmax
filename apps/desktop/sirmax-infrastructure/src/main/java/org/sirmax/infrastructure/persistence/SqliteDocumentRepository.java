// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.sirmax.infrastructure.persistence.JdbcHelper.bool;
import static org.sirmax.infrastructure.persistence.JdbcHelper.instant;
import static org.sirmax.infrastructure.persistence.JdbcHelper.str;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.sirmax.application.port.DocumentRepository;
import org.sirmax.domain.document.DocumentKind;
import org.sirmax.domain.document.IssuedDocument;
import org.sirmax.domain.document.PaperFormat;
import org.sirmax.domain.document.PrinterProfile;
import org.sirmax.domain.document.VerificationCode;

/** SQLite persistence for issued documents, their print history and printer profiles. */
public final class SqliteDocumentRepository implements DocumentRepository {

    private final SqliteDatabase db;
    private final DocumentJson json = new DocumentJson();

    public SqliteDocumentRepository(SqliteDatabase db) {
        this.db = db;
    }

    // ── issued documents ──

    @Override
    public void save(IssuedDocument d) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO issued_document"
                    + " (id, document_number, kind, template_id, paper_format, invoice_id,"
                    + "  payment_id, procedure_id, registered_document_id, verification_code,"
                    + "  issued_at, issued_by, snapshot_json, storage_path, sha256, print_count,"
                    + "  last_printed_at, voided, created_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT(id) DO UPDATE SET storage_path=excluded.storage_path,"
                    + " sha256=excluded.sha256, print_count=excluded.print_count,"
                    + " last_printed_at=excluded.last_printed_at, voided=excluded.voided",
                d.id(),
                d.documentNumber(),
                d.kind().name(),
                d.templateId().orElse(null),
                d.paperFormat().name(),
                d.invoiceId().orElse(null),
                d.paymentId().orElse(null),
                d.procedureId().orElse(null),
                d.registeredDocumentId().orElse(null),
                d.verificationCode().value(),
                d.issuedAt(),
                d.issuedBy().orElse(null),
                json.toJson(d.snapshot()),
                d.storagePath().orElse(null),
                d.sha256().orElse(null),
                d.printCount(),
                d.lastPrintedAt().orElse(null),
                d.isVoided(),
                d.createdAt());
    }

    @Override
    public Optional<IssuedDocument> findById(String id) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM issued_document WHERE id = ?",
                this::mapDocument,
                id);
    }

    @Override
    public Optional<IssuedDocument> findByNumber(String documentNumber) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM issued_document WHERE document_number = ?",
                this::mapDocument,
                documentNumber);
    }

    @Override
    public Optional<IssuedDocument> findByVerificationCode(VerificationCode code) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM issued_document WHERE verification_code = ?",
                this::mapDocument,
                code.value());
    }

    @Override
    public List<IssuedDocument> findForInvoice(String invoiceId) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM issued_document WHERE invoice_id = ? ORDER BY issued_at",
                this::mapDocument,
                invoiceId);
    }

    @Override
    public List<IssuedDocument> findForProcedure(String procedureId) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM issued_document WHERE procedure_id = ? ORDER BY issued_at",
                this::mapDocument,
                procedureId);
    }

    // ── print history ──

    @Override
    public void recordPrint(
            String id,
            String issuedDocumentId,
            Instant printedAt,
            String printedBy,
            String printerProfileId,
            boolean isReprint,
            String reason) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO document_print"
                        + " (id, issued_document_id, printed_at, printed_by, printer_profile_id,"
                        + "  is_reprint, reason)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                id,
                issuedDocumentId,
                printedAt,
                printedBy,
                printerProfileId,
                isReprint,
                reason);
    }

    @Override
    public List<PrintEntry> printHistory(String issuedDocumentId) {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM document_print WHERE issued_document_id = ?"
                        + " ORDER BY printed_at, rowid",
                SqliteDocumentRepository::mapPrint,
                issuedDocumentId);
    }

    // ── printer profiles ──

    @Override
    public void save(PrinterProfile p) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO printer_profile"
                        + " (id, name, printer_name, paper_format, workstation, is_default, copies,"
                        + "  silent, created_at, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        + " ON CONFLICT(id) DO UPDATE SET name=excluded.name,"
                        + " printer_name=excluded.printer_name, paper_format=excluded.paper_format,"
                        + " workstation=excluded.workstation, is_default=excluded.is_default,"
                        + " copies=excluded.copies, silent=excluded.silent,"
                        + " updated_at=excluded.updated_at",
                p.id(),
                p.name(),
                p.printerName().orElse(null),
                p.paperFormat().name(),
                p.workstation().orElse(null),
                p.isDefault(),
                p.copies(),
                p.silent(),
                p.createdAt(),
                p.updatedAt());
    }

    @Override
    public Optional<PrinterProfile> findProfileById(String id) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM printer_profile WHERE id = ?",
                SqliteDocumentRepository::mapProfile,
                id);
    }

    @Override
    public List<PrinterProfile> listProfiles() {
        return JdbcHelper.queryList(
                db.connection(),
                "SELECT * FROM printer_profile ORDER BY paper_format, name",
                SqliteDocumentRepository::mapProfile);
    }

    @Override
    public Optional<PrinterProfile> resolveProfile(PaperFormat format, String workstation) {
        // Most specific wins: this workstation's profile, then a general one, and an explicit
        // default ahead of the rest. A counter with its own receipt printer must never fall
        // through to the office laser.
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT * FROM printer_profile WHERE paper_format = ?"
                        + " AND (workstation IS NULL OR lower(workstation) = lower(?))"
                        + " ORDER BY CASE WHEN workstation IS NULL THEN 1 ELSE 0 END,"
                        + " is_default DESC, name LIMIT 1",
                SqliteDocumentRepository::mapProfile,
                format.name(),
                workstation == null ? "" : workstation);
    }

    // ── row mappers ──

    private IssuedDocument mapDocument(ResultSet rs) throws SQLException {
        return new IssuedDocument(
                rs.getString("id"),
                rs.getString("document_number"),
                DocumentKind.valueOf(rs.getString("kind")),
                str(rs, "template_id"),
                PaperFormat.valueOf(rs.getString("paper_format")),
                str(rs, "invoice_id"),
                str(rs, "payment_id"),
                str(rs, "procedure_id"),
                str(rs, "registered_document_id"),
                new VerificationCode(rs.getString("verification_code")),
                instant(rs, "issued_at"),
                str(rs, "issued_by"),
                json.fromJson(rs.getString("snapshot_json")),
                str(rs, "storage_path"),
                str(rs, "sha256"),
                rs.getInt("print_count"),
                instant(rs, "last_printed_at"),
                bool(rs, "voided"),
                instant(rs, "created_at"));
    }

    private static PrintEntry mapPrint(ResultSet rs) throws SQLException {
        return new PrintEntry(
                rs.getString("id"),
                rs.getString("issued_document_id"),
                instant(rs, "printed_at"),
                Optional.ofNullable(str(rs, "printed_by")),
                Optional.ofNullable(str(rs, "printer_profile_id")),
                bool(rs, "is_reprint"),
                Optional.ofNullable(str(rs, "reason")));
    }

    private static PrinterProfile mapProfile(ResultSet rs) throws SQLException {
        return new PrinterProfile(
                rs.getString("id"),
                rs.getString("name"),
                Optional.ofNullable(str(rs, "printer_name")),
                PaperFormat.valueOf(rs.getString("paper_format")),
                Optional.ofNullable(str(rs, "workstation")),
                bool(rs, "is_default"),
                rs.getInt("copies"),
                bool(rs, "silent"),
                instant(rs, "created_at"),
                instant(rs, "updated_at"));
    }
}
