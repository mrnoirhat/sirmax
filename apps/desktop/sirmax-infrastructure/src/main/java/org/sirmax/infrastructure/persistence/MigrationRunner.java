// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sirmax.shared.SirmaxException;

/**
 * Applies pending schema migrations and verifies already-applied ones have not drifted.
 *
 * <p>Contract (see {@code DATABASE.md} §3):
 *
 * <ul>
 *   <li>each migration runs inside its own transaction; a failure rolls that migration back;
 *   <li>applied migrations are recorded in {@code schema_migrations} (created by {@code V0001});
 *   <li>migrations must be strictly increasing — inserting a version below one already applied is
 *       rejected rather than silently applied;
 *   <li>{@link #validate} throws if an applied migration's checksum no longer matches its file.
 * </ul>
 */
public final class MigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigrationRunner.class);

    private final MigrationSource source;
    private final Clock clock;

    public MigrationRunner() {
        this(MigrationSource.classpath(), Clock.systemUTC());
    }

    MigrationRunner(MigrationSource source, Clock clock) {
        this.source = source;
        this.clock = clock;
    }

    /**
     * Apply every migration not yet recorded as applied.
     *
     * @return the versions applied by this call, in order
     */
    public List<Integer> migrate(Connection connection) {
        List<Migration> all = source.load();
        Map<Integer, String> applied = readApplied(connection);
        int maxApplied = applied.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);

        verifyNoDrift(all, applied);

        List<Integer> justApplied = new ArrayList<>();
        for (Migration m : all) {
            if (applied.containsKey(m.version())) {
                continue;
            }
            if (m.version() <= maxApplied) {
                throw new SirmaxException(
                        "Out-of-order migration V"
                                + pad(m.version())
                                + " — a version below the highest applied ("
                                + pad(maxApplied)
                                + ") cannot be introduced. Add a new higher-numbered migration.");
            }
            applyOne(connection, m);
            justApplied.add(m.version());
            maxApplied = m.version();
        }
        if (justApplied.isEmpty()) {
            log.info("Database schema is up to date (version {}).", pad(maxApplied));
        } else {
            log.info("Applied migrations: {}", justApplied);
        }
        return List.copyOf(justApplied);
    }

    /** Verify applied migrations still match their files; does not change the database. */
    public void validate(Connection connection) {
        verifyNoDrift(source.load(), readApplied(connection));
    }

    // ------------------------------------------------------------------

    private void applyOne(Connection connection, Migration m) {
        boolean previousAutoCommit;
        try {
            previousAutoCommit = connection.getAutoCommit();
        } catch (SQLException e) {
            throw new SirmaxException("Could not read auto-commit state", e);
        }
        try {
            connection.setAutoCommit(false);
            try (Statement st = connection.createStatement()) {
                for (String sql : SqlScript.splitStatements(m.sql())) {
                    st.execute(sql);
                }
            }
            recordApplied(connection, m);
            connection.commit();
            log.info("Applied V{} — {}", pad(m.version()), m.description());
        } catch (SQLException e) {
            safeRollback(connection);
            throw new SirmaxException(
                    "Migration V" + pad(m.version()) + " failed: " + e.getMessage(), e);
        } finally {
            try {
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException ignored) {
                // best effort
            }
        }
    }

    private void recordApplied(Connection connection, Migration m) throws SQLException {
        String sql =
                "INSERT INTO schema_migrations (version, description, checksum, applied_at, success)"
                        + " VALUES (?, ?, ?, ?, 1)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, m.version());
            ps.setString(2, m.description());
            ps.setString(3, m.checksum());
            ps.setString(4, clock.instant().toString());
            ps.executeUpdate();
        }
    }

    private Map<Integer, String> readApplied(Connection connection) {
        Map<Integer, String> applied = new LinkedHashMap<>();
        String sql = "SELECT version, checksum FROM schema_migrations WHERE success = 1 ORDER BY version";
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                applied.put(rs.getInt("version"), rs.getString("checksum"));
            }
        } catch (SQLException e) {
            // schema_migrations does not exist yet on a brand-new database — nothing applied.
            return Map.of();
        }
        return applied;
    }

    private void verifyNoDrift(List<Migration> all, Map<Integer, String> applied) {
        Map<Integer, Migration> byVersion = new LinkedHashMap<>();
        for (Migration m : all) {
            byVersion.put(m.version(), m);
        }
        for (Map.Entry<Integer, String> e : applied.entrySet()) {
            Migration m = byVersion.get(e.getKey());
            if (m == null) {
                throw new SirmaxException(
                        "Database has migration V"
                                + pad(e.getKey())
                                + " applied, but no such migration file exists.");
            }
            if (!m.checksum().equals(e.getValue())) {
                throw new SirmaxException(
                        "Migration V"
                                + pad(e.getKey())
                                + " has changed since it was applied (checksum mismatch). Never"
                                + " edit a published migration — add a new one.");
            }
        }
    }

    private static void safeRollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // best effort
        }
    }

    private static String pad(int version) {
        return String.format("%04d", version);
    }
}
