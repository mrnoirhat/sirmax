// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.sirmax.shared.SirmaxException;

/** Provides the ordered list of schema migrations to apply. */
interface MigrationSource {

    /** Migrations sorted ascending by {@link Migration#version()}. */
    List<Migration> load();

    /**
     * Reads migrations from the classpath under {@code db/migration/}, listed in
     * {@code db/migration/index.txt} (one file name per line). The index is produced by the Gradle
     * {@code stageMigrations} task so this works identically from a jar and from exploded classes.
     */
    static MigrationSource classpath() {
        return () -> {
            ClassLoader cl = MigrationSource.class.getClassLoader();
            List<String> names = readLines(cl, "db/migration/index.txt");
            List<Migration> migrations = new ArrayList<>();
            for (String name : names) {
                if (name.isBlank()) {
                    continue;
                }
                String sql = read(cl, "db/migration/" + name.trim());
                migrations.add(Migration.fromFile(name.trim(), sql));
            }
            migrations.sort(Comparator.comparingInt(Migration::version));
            return List.copyOf(migrations);
        };
    }

    private static List<String> readLines(ClassLoader cl, String resource) {
        return read(cl, resource).lines().toList();
    }

    private static String read(ClassLoader cl, String resource) {
        try (InputStream in = cl.getResourceAsStream(resource)) {
            if (in == null) {
                throw new SirmaxException("Migration resource not found on classpath: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SirmaxException("Could not read migration resource: " + resource, e);
        }
    }
}
