// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.sirmax.application.port.SettingsRepository;

/** {@link SettingsRepository} backed by the {@code app_setting} table. */
public final class SqliteSettingsRepository implements SettingsRepository {

    private final SqliteDatabase db;

    public SqliteSettingsRepository(SqliteDatabase db) {
        this.db = db;
    }

    @Override
    public Optional<String> get(String key) {
        return JdbcHelper.queryOne(
                db.connection(),
                "SELECT value_json FROM app_setting WHERE key = ?",
                rs -> rs.getString(1),
                key);
    }

    @Override
    public void put(String key, String valueJson, Classification classification) {
        JdbcHelper.update(
                db.connection(),
                "INSERT INTO app_setting (key, value_json, classification, updated_at)"
                    + " VALUES (?, ?, ?, ?)"
                    + " ON CONFLICT(key) DO UPDATE SET value_json=excluded.value_json,"
                    + " classification=excluded.classification, updated_at=excluded.updated_at",
                key,
                valueJson,
                classification.name(),
                java.time.Instant.now());
    }

    @Override
    public void remove(String key) {
        JdbcHelper.update(db.connection(), "DELETE FROM app_setting WHERE key = ?", key);
    }

    @Override
    public Map<String, String> all() {
        Map<String, String> out = new LinkedHashMap<>();
        JdbcHelper.queryList(
                        db.connection(),
                        "SELECT key, value_json FROM app_setting ORDER BY key",
                        rs -> Map.entry(rs.getString(1), rs.getString(2)))
                .forEach(e -> out.put(e.getKey(), e.getValue()));
        return out;
    }
}
