// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a SQL migration script into individual statements.
 *
 * <p>The sqlite-jdbc driver executes one statement per call, so a multi-statement {@code .sql} file
 * has to be split first. This splitter is aware of:
 *
 * <ul>
 *   <li>single-quoted string literals (with {@code ''} escaping);
 *   <li>{@code --} line comments and {@code /* *&#47;} block comments;
 *   <li>{@code BEGIN} / {@code END} nesting, so a trigger body's inner {@code ;} does not end the
 *       {@code CREATE TRIGGER} statement.
 * </ul>
 *
 * <p>It is intentionally simple; migration SQL in this project is written to stay within what it
 * handles (no {@code ;} inside identifiers, no {@code CASE ... END} at the top level of a script).
 */
final class SqlScript {

    private SqlScript() {}

    static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        int i = 0;
        int n = script.length();

        while (i < n) {
            char c = script.charAt(i);

            // line comment
            if (c == '-' && i + 1 < n && script.charAt(i + 1) == '-') {
                int eol = script.indexOf('\n', i);
                i = (eol < 0) ? n : eol;
                continue;
            }
            // block comment
            if (c == '/' && i + 1 < n && script.charAt(i + 1) == '*') {
                int end = script.indexOf("*/", i + 2);
                i = (end < 0) ? n : end + 2;
                continue;
            }
            // string literal
            if (c == '\'') {
                current.append(c);
                i++;
                while (i < n) {
                    char s = script.charAt(i);
                    current.append(s);
                    i++;
                    if (s == '\'') {
                        if (i < n && script.charAt(i) == '\'') {
                            current.append('\'');
                            i++;
                        } else {
                            break;
                        }
                    }
                }
                continue;
            }

            if (isKeywordAt(script, i, "BEGIN")) {
                depth++;
                current.append(script, i, i + 5);
                i += 5;
                continue;
            }
            if (isKeywordAt(script, i, "END") && depth > 0) {
                depth--;
                current.append(script, i, i + 3);
                i += 3;
                continue;
            }

            if (c == ';' && depth == 0) {
                addIfNotBlank(statements, current);
                current.setLength(0);
                i++;
                continue;
            }

            current.append(c);
            i++;
        }

        addIfNotBlank(statements, current);
        return statements;
    }

    private static boolean isKeywordAt(String s, int idx, String keyword) {
        int len = keyword.length();
        if (idx + len > s.length()) {
            return false;
        }
        if (!s.regionMatches(true, idx, keyword, 0, len)) {
            return false;
        }
        boolean beforeOk = idx == 0 || !isWordChar(s.charAt(idx - 1));
        boolean afterOk = idx + len == s.length() || !isWordChar(s.charAt(idx + len));
        return beforeOk && afterOk;
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static void addIfNotBlank(List<String> out, CharSequence sql) {
        String trimmed = sql.toString().trim();
        if (!trimmed.isEmpty()) {
            out.add(trimmed);
        }
    }
}
