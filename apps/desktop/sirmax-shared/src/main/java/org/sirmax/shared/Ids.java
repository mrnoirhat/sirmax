// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.shared;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Time-ordered identifier generation (UUIDv7-style: 48-bit millisecond timestamp + 74 random bits).
 *
 * <p>Ordered identifiers keep SQLite primary-key indexes compact and make rows sortable by creation
 * time. The exact PK strategy per table is decided in Phase 3 (see {@code docs/domain/erd.md} §8).
 */
public final class Ids {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Ids() {}

    /** A new time-ordered 128-bit id as a 36-char canonical UUID string. */
    public static String newId() {
        return newId(Instant.now());
    }

    static String newId(Instant now) {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);

        long ts = now.toEpochMilli();
        bytes[0] = (byte) (ts >>> 40);
        bytes[1] = (byte) (ts >>> 32);
        bytes[2] = (byte) (ts >>> 24);
        bytes[3] = (byte) (ts >>> 16);
        bytes[4] = (byte) (ts >>> 8);
        bytes[5] = (byte) ts;

        bytes[6] = (byte) (0x70 | (bytes[6] & 0x0f)); // version 7
        bytes[8] = (byte) (0x80 | (bytes[8] & 0x3f)); // IETF variant

        StringBuilder sb = new StringBuilder(36);
        for (int i = 0; i < 16; i++) {
            if (i == 4 || i == 6 || i == 8 || i == 10) {
                sb.append('-');
            }
            int v = bytes[i] & 0xff;
            sb.append(HEX[v >>> 4]).append(HEX[v & 0x0f]);
        }
        return sb.toString();
    }
}
