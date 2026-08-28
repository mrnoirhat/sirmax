// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExpressionEvaluatorTest {

    private static boolean eval(String expr, Map<String, Object> ctx) {
        return ExpressionEvaluator.evaluate(expr, ctx);
    }

    @Test
    void blankOrNullIsTrue() {
        assertThat(eval("", Map.of())).isTrue();
        assertThat(eval(null, Map.of())).isTrue();
        assertThat(eval("   ", Map.of())).isTrue();
    }

    @Test
    void stringEquality() {
        Map<String, Object> ctx = Map.of("tipo", "COMERCIAL");
        assertThat(eval("tipo == 'COMERCIAL'", ctx)).isTrue();
        assertThat(eval("tipo != 'RESIDENCIAL'", ctx)).isTrue();
        assertThat(eval("tipo == 'RESIDENCIAL'", ctx)).isFalse();
    }

    @Test
    void numericComparisonsCoerceStringsAndNumbers() {
        Map<String, Object> ctx = Map.of("area", new BigDecimal("150"), "aforo", 30);
        assertThat(eval("area > 100", ctx)).isTrue();
        assertThat(eval("area >= 150 && aforo <= 30", ctx)).isTrue();
        assertThat(eval("area < 100 || aforo == 30", ctx)).isTrue();
        assertThat(eval("'42' == 42", Map.of())).isTrue();
    }

    @Test
    void booleanLogicAndNegationAndParentheses() {
        Map<String, Object> ctx = Map.of("urgente", true, "zona", "HISTORICA");
        assertThat(eval("urgente && (zona == 'HISTORICA' || zona == 'CENTRO')", ctx)).isTrue();
        assertThat(eval("!urgente", ctx)).isFalse();
        assertThat(eval("!(zona == 'PERIFERIA')", ctx)).isTrue();
    }

    @Test
    void unknownIdentifierIsNullyAndFalsey() {
        assertThat(eval("desconocido", Map.of())).isFalse();
        assertThat(eval("desconocido == null", Map.of())).isTrue();
        assertThat(eval("desconocido != 'x'", Map.of())).isTrue();
    }

    @Test
    void malformedExpressionThrows() {
        assertThatThrownBy(() -> eval("area > ", Map.of())).isInstanceOf(ExpressionException.class);
        assertThatThrownBy(() -> eval("(a == 1", Map.of())).isInstanceOf(ExpressionException.class);
        assertThatThrownBy(() -> eval("a == 1)", Map.of())).isInstanceOf(ExpressionException.class);
    }
}
