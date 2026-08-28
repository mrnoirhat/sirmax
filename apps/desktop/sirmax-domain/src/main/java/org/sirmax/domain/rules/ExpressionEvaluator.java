// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.rules;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/**
 * A deliberately small boolean expression evaluator for service-configuration conditions
 * (docs/adr/0007 — "evaluador de expresiones restringido").
 *
 * <p>Grammar: {@code || } &gt; {@code && } &gt; {@code ! } &gt; comparisons ({@code == != < <= > >=})
 * &gt; primaries (parenthesised expression, number, quoted string, {@code true}/{@code false}, or an
 * identifier resolved from the typed context). There are <strong>no function calls, no assignment,
 * and no I/O</strong>. Unknown identifiers resolve to {@code null}.
 *
 * <p>Examples: {@code "tipo == 'COMERCIAL'"}, {@code "area > 100 && zona != 'HISTORICA'"}.
 */
public final class ExpressionEvaluator {

    private final String src;
    private final Map<String, Object> context;
    private int pos;

    private ExpressionEvaluator(String src, Map<String, Object> context) {
        this.src = src;
        this.context = context;
    }

    /** Evaluate {@code expression} against {@code context}; a blank expression is {@code true}. */
    public static boolean evaluate(String expression, Map<String, Object> context) {
        Objects.requireNonNull(context, "context");
        if (expression == null || expression.isBlank()) {
            return true;
        }
        ExpressionEvaluator e = new ExpressionEvaluator(expression, context);
        Object v = e.parseOr();
        e.skipWs();
        if (e.pos != e.src.length()) {
            throw new ExpressionException("Unexpected input at " + e.pos + " in: " + expression);
        }
        return truthy(v);
    }

    // ── grammar ──

    private Object parseOr() {
        Object left = parseAnd();
        while (match("||")) {
            boolean r = truthy(parseAnd());
            left = truthy(left) || r;
        }
        return left;
    }

    private Object parseAnd() {
        Object left = parseNot();
        while (match("&&")) {
            boolean r = truthy(parseNot());
            left = truthy(left) && r;
        }
        return left;
    }

    private Object parseNot() {
        skipWs();
        if (match("!")) {
            return !truthy(parseNot());
        }
        return parseComparison();
    }

    private Object parseComparison() {
        Object left = parsePrimary();
        skipWs();
        for (String op : new String[] {"==", "!=", "<=", ">=", "<", ">"}) {
            if (match(op)) {
                Object right = parsePrimary();
                return compare(op, left, right);
            }
        }
        return left;
    }

    private Object parsePrimary() {
        skipWs();
        if (pos >= src.length()) {
            throw new ExpressionException("Unexpected end of expression");
        }
        char c = src.charAt(pos);

        if (c == '(') {
            pos++;
            Object v = parseOr();
            skipWs();
            expect(')');
            return v;
        }
        if (c == '\'' || c == '"') {
            return readString(c);
        }
        if (Character.isDigit(c) || (c == '-' && pos + 1 < src.length() && Character.isDigit(src.charAt(pos + 1)))) {
            return readNumber();
        }
        String ident = readIdentifier();
        return switch (ident) {
            case "true" -> Boolean.TRUE;
            case "false" -> Boolean.FALSE;
            case "null" -> null;
            default -> context.get(ident);
        };
    }

    // ── helpers ──

    private static boolean truthy(Object v) {
        if (v == null) {
            return false;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof CharSequence s) {
            return !s.isEmpty();
        }
        if (v instanceof Number n) {
            return n.doubleValue() != 0.0;
        }
        return true;
    }

    private static Object compare(String op, Object a, Object b) {
        if (op.equals("==")) {
            return equalsLoose(a, b);
        }
        if (op.equals("!=")) {
            return !equalsLoose(a, b);
        }
        int cmp = order(a, b);
        return switch (op) {
            case "<" -> cmp < 0;
            case "<=" -> cmp <= 0;
            case ">" -> cmp > 0;
            case ">=" -> cmp >= 0;
            default -> throw new ExpressionException("Unknown operator " + op);
        };
    }

    private static boolean equalsLoose(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a instanceof Number || b instanceof Number) {
            return toNumber(a).compareTo(toNumber(b)) == 0;
        }
        return a.toString().equals(b.toString());
    }

    private static int order(Object a, Object b) {
        if (a instanceof Number || b instanceof Number) {
            return toNumber(a).compareTo(toNumber(b));
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    private static BigDecimal toNumber(Object v) {
        if (v instanceof BigDecimal d) {
            return d;
        }
        if (v instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new ExpressionException("Not a number: " + v);
        }
    }

    private boolean match(String token) {
        skipWs();
        if (src.startsWith(token, pos)) {
            // don't let "!" swallow "!="
            if (token.equals("!") && src.startsWith("!=", pos)) {
                return false;
            }
            pos += token.length();
            return true;
        }
        return false;
    }

    private void expect(char c) {
        if (pos >= src.length() || src.charAt(pos) != c) {
            throw new ExpressionException("Expected '" + c + "' at " + pos);
        }
        pos++;
    }

    private void skipWs() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
    }

    private String readString(char quote) {
        pos++; // opening quote
        StringBuilder sb = new StringBuilder();
        while (pos < src.length() && src.charAt(pos) != quote) {
            sb.append(src.charAt(pos++));
        }
        expect(quote);
        return sb.toString();
    }

    private BigDecimal readNumber() {
        int start = pos;
        if (src.charAt(pos) == '-') {
            pos++;
        }
        while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
            pos++;
        }
        return new BigDecimal(src.substring(start, pos));
    }

    private String readIdentifier() {
        int start = pos;
        while (pos < src.length()
                && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_'
                        || src.charAt(pos) == '.')) {
            pos++;
        }
        if (pos == start) {
            throw new ExpressionException("Expected an identifier at " + pos + " in: " + src);
        }
        return src.substring(start, pos);
    }
}
