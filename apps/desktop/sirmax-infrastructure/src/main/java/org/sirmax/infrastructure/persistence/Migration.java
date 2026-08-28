// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sirmax.shared.SirmaxException;

/**
 * One versioned schema migration, e.g. {@code V0002__core_schema.sql}.
 *
 * @param version monotonic version number (the {@code NNNN} in the file name)
 * @param description human description (the part after {@code __}, underscores → spaces)
 * @param sql the raw script
 * @param checksum SHA-256 of {@code sql}, used to detect drift in already-applied migrations
 */
record Migration(int version, String description, String sql, String checksum) {

    private static final Pattern FILE_NAME =
            Pattern.compile("V(\\d{1,4})__([A-Za-z0-9_]+)\\.sql");

    Migration {
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(sql, "sql");
        Objects.requireNonNull(checksum, "checksum");
    }

    static Migration fromFile(String fileName, String sql) {
        Matcher m = FILE_NAME.matcher(fileName);
        if (!m.matches()) {
            throw new SirmaxException("Not a valid migration file name: " + fileName);
        }
        int version = Integer.parseInt(m.group(1));
        String description = m.group(2).replace('_', ' ');
        return new Migration(version, description, sql, sha256(sql));
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new SirmaxException("SHA-256 unavailable", e);
        }
    }
}
