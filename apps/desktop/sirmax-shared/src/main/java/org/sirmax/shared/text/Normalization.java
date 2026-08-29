// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.shared.text;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Accent- and case-insensitive text folding for search and matching.
 *
 * <p>Operators at a municipal counter type names without accents, and Dominican registries spell the
 * same person as "Peña" and "Pena" across decades of records. Every search key SIRMAX persists or
 * compares goes through {@link #fold} so those all meet: SQLite's {@code LIKE} is only
 * case-insensitive for ASCII and knows nothing about diacritics.
 *
 * <p>Kept in {@code shared} because both the domain (duplicate detection) and infrastructure (the
 * stored {@code search_name} column) must fold identically — a mismatch would silently hide records.
 */
public final class Normalization {

    private static final java.util.regex.Pattern DIACRITICS =
            java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private Normalization() {}

    /**
     * Lower-cased, accent-stripped, whitespace-collapsed form of {@code value}. {@code null} folds to
     * an empty string so callers never branch on it.
     *
     * <p>{@code "José Luis Peña  Gómez"} → {@code "jose luis pena gomez"}. Ñ folds to N: it costs the
     * ability to distinguish "cana" from "caña" but wins every "Nunez"/"Núñez" lookup, which is the
     * trade a registry actually needs.
     */
    public static String fold(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        String stripped = DIACRITICS.matcher(decomposed).replaceAll("");
        return stripped.toLowerCase(Locale.ROOT).strip().replaceAll("\\s+", " ");
    }

    /** The folded words of {@code value}, longer than one character, in order and de-duplicated. */
    public static Set<String> tokens(String value) {
        Set<String> out = new LinkedHashSet<>();
        String folded = fold(value);
        if (folded.isEmpty()) {
            return out;
        }
        for (String token : folded.split(" ")) {
            if (token.length() > 1) {
                out.add(token);
            }
        }
        return out;
    }

    /**
     * Token-overlap (Jaccard) similarity of two names, in {@code [0, 1]}.
     *
     * <p>Symmetric, cheap, and forgiving of the extra or missing middle name that causes most real
     * duplicates — unlike edit distance, which punishes a whole missing word.
     */
    public static double similarity(String a, String b) {
        Set<String> left = tokens(a);
        Set<String> right = tokens(b);
        if (left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        Set<String> union = new LinkedHashSet<>(left);
        union.addAll(right);
        Set<String> intersection = new LinkedHashSet<>(left);
        intersection.retainAll(right);
        return (double) intersection.size() / union.size();
    }
}
