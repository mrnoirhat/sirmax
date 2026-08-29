// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.document;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Objects;

/**
 * The public code printed on an official document so it can be checked later (master prompt §47).
 *
 * <p>Format: {@code XXXX-XXXX-XXXX}, from a 28-character alphabet with the shapes that get misread
 * off a low-resolution receipt removed — no {@code O}/{@code 0}, no {@code I}/{@code 1}, no
 * {@code S}/{@code 5}. Someone reading a code aloud over the phone is a real verification channel in
 * a municipality, and it fails on exactly those pairs.
 *
 * <p>The code carries <b>no information</b>: it is random, not derived from the invoice number or
 * the citizen's data. §48 requires that public verification never expose private records, and a code
 * you can decode is already a leak.
 */
public record VerificationCode(String value) {

    /** Ambiguity-free alphabet: 0/O, 1/I/L, 5/S, 8/B and 2/Z are all dropped. */
    private static final String ALPHABET = "34679ACDEFGHJKMNPQRTUVWXY";

    private static final int GROUPS = 3;
    private static final int GROUP_SIZE = 4;
    private static final SecureRandom RANDOM = new SecureRandom();

    public VerificationCode {
        Objects.requireNonNull(value, "value");
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[" + ALPHABET + "]{4}(-[" + ALPHABET + "]{4}){2}")) {
            throw new IllegalArgumentException("Not a SIRMAX verification code: " + value);
        }
        value = normalized;
    }

    /**
     * A fresh random code. {@code 25^12} ≈ 6×10^16 possibilities, so collisions are not a practical
     * concern; the unique index on the column is the backstop that makes that guarantee real.
     */
    public static VerificationCode generate() {
        StringBuilder out = new StringBuilder(GROUPS * GROUP_SIZE + GROUPS - 1);
        for (int group = 0; group < GROUPS; group++) {
            if (group > 0) {
                out.append('-');
            }
            for (int i = 0; i < GROUP_SIZE; i++) {
                out.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
        }
        return new VerificationCode(out.toString());
    }

    /**
     * Parse a code someone typed or read aloud, forgiving the mistakes the alphabet anticipates:
     * lower case, missing dashes, and the confusable characters mapped to their intended letter.
     */
    public static VerificationCode parse(String typed) {
        Objects.requireNonNull(typed, "typed");
        String cleaned =
                typed.toUpperCase(Locale.ROOT)
                        .replace("O", "Q")
                        .replace("0", "Q")
                        .replace("I", "J")
                        .replace("1", "7")
                        .replace("L", "J")
                        .replace("S", "3")
                        .replace("5", "3")
                        .replace("B", "6")
                        .replace("8", "6")
                        .replace("Z", "4")
                        .replace("2", "4")
                        .replaceAll("[^" + ALPHABET + "]", "");
        if (cleaned.length() != GROUPS * GROUP_SIZE) {
            throw new IllegalArgumentException("A verification code has 12 characters");
        }
        return new VerificationCode(
                cleaned.substring(0, 4) + "-" + cleaned.substring(4, 8) + "-" + cleaned.substring(8));
    }

    /** The URL a QR code points at, under the municipality's own verification host. */
    public String verificationUrl(String baseUrl) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + "/verificar/" + value;
    }

    @Override
    public String toString() {
        return value;
    }
}
