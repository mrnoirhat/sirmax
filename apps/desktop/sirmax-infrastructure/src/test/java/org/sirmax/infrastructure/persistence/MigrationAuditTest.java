// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The Phase 13 migration audit (master prompt §13, release gate).
 *
 * <p>These are properties of the whole schema rather than of any one migration, and they are the
 * ones that only fail years later: a table with no primary key that quietly accumulates duplicates,
 * a foreign key that lets a citizen be deleted out from under their invoices, a money column stored
 * as a float. Checking them here means a future migration cannot introduce one by accident.
 */
class MigrationAuditTest {

    private SqliteDatabase db;

    @BeforeEach
    void setUp() {
        db = SqliteDatabase.openInMemory();
        db.migrate();
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void migratingTwiceChangesNothing() {
        List<String> before = tableNames();
        int applied = appliedMigrations();

        db.migrate();

        assertThat(tableNames()).isEqualTo(before);
        assertThat(appliedMigrations()).isEqualTo(applied);
    }

    @Test
    void theSchemaPassesSqlitesOwnIntegrityCheck() {
        assertThat(scalar("PRAGMA integrity_check")).isEqualTo("ok");
        // Empty result means no violations; a populated one names the offending rows.
        assertThat(rows("PRAGMA foreign_key_check")).isEmpty();
    }

    @Test
    void foreignKeysAreEnforcedNotJustDeclared() {
        // SQLite defaults foreign keys OFF. Declaring them without this pragma is decoration.
        assertThat(scalar("PRAGMA foreign_keys")).isEqualTo("1");
    }

    @Test
    void everyTableHasAPrimaryKey() {
        List<String> without = new ArrayList<>();
        for (String table : tableNames()) {
            boolean hasKey = false;
            for (String[] column : columns(table)) {
                if (!"0".equals(column[5])) { // pk column of PRAGMA table_info
                    hasKey = true;
                    break;
                }
            }
            if (!hasKey) {
                without.add(table);
            }
        }
        assertThat(without).as("tables without a primary key").isEmpty();
    }

    @Test
    void moneyIsNeverStoredAsAFloat() {
        // The single rule of §2.3. A REAL column is how a municipality ends up 3 centavos short
        // at the end of a year, with no way to say where they went.
        List<String> floats = new ArrayList<>();
        for (String table : tableNames()) {
            for (String[] column : columns(table)) {
                String name = column[1];
                String type = column[2].toUpperCase(java.util.Locale.ROOT);
                boolean monetary =
                        name.endsWith("_minor")
                                || name.contains("amount")
                                || name.contains("total")
                                || name.contains("price");
                if (monetary && (type.contains("REAL") || type.contains("FLOAT")
                        || type.contains("DOUBLE"))) {
                    floats.add(table + "." + name + " " + type);
                }
            }
        }
        assertThat(floats).as("monetary columns stored as floating point").isEmpty();
    }

    /**
     * Tables whose amounts take their currency from a parent row.
     *
     * <p>An invoice line cannot be in a different currency from its invoice, so storing the code
     * twice would only create somewhere for the two to disagree. The join that reads it lives in
     * {@code SqliteBillingRepository}.
     */
    private static final java.util.Map<String, String> CURRENCY_INHERITED_FROM =
            java.util.Map.of("invoice_line", "invoice");

    @Test
    void everyMonetaryTableCarriesItsCurrency() {
        // An integer amount with no currency anywhere in reach is a number, not money.
        for (String table : tableNames()) {
            boolean hasAmount = columns(table).stream().anyMatch(c -> c[1].endsWith("_minor"));
            if (!hasAmount) {
                continue;
            }
            String source = CURRENCY_INHERITED_FROM.getOrDefault(table, table);
            assertThat(columns(source).stream().map(c -> c[1]))
                    .as("%s stores amounts; %s must name their currency", table, source)
                    .contains("currency");
        }
    }

    @Test
    void theAuditTrailRefusesToBeRewritten() {
        assertThat(rows("SELECT name FROM sqlite_master WHERE type = 'trigger'"))
                .contains("audit_event_no_update", "audit_event_no_delete");
    }

    /**
     * Foreign keys deliberately left unindexed, and why.
     *
     * <p>Almost all of these name <em>who did something</em>, for the audit trail. Nothing queries
     * by them, and indexing each would cost write throughput on the busiest tables in the system to
     * serve a report nobody runs. Listing them here makes each one a decision on record: a new
     * foreign key that is not on this list fails the audit until someone either indexes it or adds
     * it with a reason.
     */
    private static final java.util.Set<String> UNINDEXED_BY_DESIGN =
            java.util.Set.of(
                    // authorship: written constantly, never queried by
                    "audit_event.actor_user_id",
                    "backup_record.created_by",
                    "decision.decided_by",
                    "document_print.printed_by",
                    "invoice.cashier_user_id",
                    "issued_document.issued_by",
                    "payment.received_by",
                    "procedure_attachment.uploaded_by",
                    "procedure_event.actor_user_id",
                    "procedure_requirement.satisfied_by",
                    "refund.authorized_by",
                    "registered_document_annotation.annotated_by",
                    "restore_record.performed_by",
                    // small tables, or columns reached only through another index
                    "cash_session.department_id",
                    "decision.document_id",
                    "document_print.printer_profile_id",
                    "issued_document.template_id",
                    "login_attempt.user_id", // attempts are looked up by username, as typed
                    "restore_record.emergency_backup_id");

    @Test
    void everyForeignKeyTheApplicationTraversesIsIndexed() {
        // SQLite indexes primary keys but never foreign keys. An unindexed FK turns "the invoices
        // of this case" into a full scan, and makes deleting a parent scan the child table —
        // both invisible until the archive is large.
        List<String> unindexed = new ArrayList<>();
        for (String table : tableNames()) {
            for (String[] fk : foreignKeys(table)) {
                String column = fk[3];
                String qualified = table + "." + column;
                if (column != null
                        && !UNINDEXED_BY_DESIGN.contains(qualified)
                        && !isIndexedFirst(table, column)) {
                    unindexed.add(qualified);
                }
            }
        }
        assertThat(unindexed).as("foreign keys with no index and no recorded exemption").isEmpty();
    }

    // ── schema reflection ──

    private List<String> tableNames() {
        return rows(
                "SELECT name FROM sqlite_master WHERE type = 'table'"
                        + " AND name NOT LIKE 'sqlite_%' ORDER BY name");
    }

    /** {@code PRAGMA table_info} rows: cid, name, type, notnull, dflt_value, pk. */
    private List<String[]> columns(String table) {
        return query("PRAGMA table_info(" + table + ")", 6);
    }

    /** {@code PRAGMA foreign_key_list} rows: id, seq, table, from, to, on_update, on_delete, match. */
    private List<String[]> foreignKeys(String table) {
        return query("PRAGMA foreign_key_list(" + table + ")", 8);
    }

    /** {@code true} when {@code column} is the first column of some index — the usable position. */
    private boolean isIndexedFirst(String table, String column) {
        for (String[] index : query("PRAGMA index_list(" + table + ")", 5)) {
            for (String[] indexed : query("PRAGMA index_info(" + index[1] + ")", 3)) {
                if ("0".equals(indexed[0]) && column.equalsIgnoreCase(indexed[2])) {
                    return true;
                }
            }
        }
        return false;
    }

    private int appliedMigrations() {
        return Integer.parseInt(scalar("SELECT count(*) FROM schema_migrations"));
    }

    private String scalar(String sql) {
        try (Statement statement = db.connection().createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        } catch (SQLException e) {
            throw new IllegalStateException(sql, e);
        }
    }

    private List<String> rows(String sql) {
        List<String> out = new ArrayList<>();
        try (Statement statement = db.connection().createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                out.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(sql, e);
        }
        return out;
    }

    private List<String[]> query(String sql, int columnCount) {
        List<String[]> out = new ArrayList<>();
        try (Statement statement = db.connection().createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                String[] row = new String[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    row[i] = rs.getString(i + 1);
                }
                out.add(row);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(sql, e);
        }
        return out;
    }
}
