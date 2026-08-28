// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SqlScriptTest {

    @Test
    void splitsOnTopLevelSemicolons() {
        List<String> out = SqlScript.splitStatements("CREATE TABLE a(x); CREATE TABLE b(y);");
        assertThat(out).containsExactly("CREATE TABLE a(x)", "CREATE TABLE b(y)");
    }

    @Test
    void ignoresLineAndBlockComments() {
        String sql =
                """
                -- a comment; with a semicolon
                CREATE TABLE a(x); /* block ; comment */
                CREATE TABLE b(y);
                """;
        assertThat(SqlScript.splitStatements(sql))
                .containsExactly("CREATE TABLE a(x)", "CREATE TABLE b(y)");
    }

    @Test
    void keepsSemicolonsInsideStringLiterals() {
        List<String> out =
                SqlScript.splitStatements("INSERT INTO t VALUES ('a; b'); INSERT INTO t VALUES ('c');");
        assertThat(out)
                .containsExactly("INSERT INTO t VALUES ('a; b')", "INSERT INTO t VALUES ('c')");
    }

    @Test
    void keepsTriggerBodyAsOneStatement() {
        String sql =
                """
                CREATE TABLE t(x);
                CREATE TRIGGER t_no_update BEFORE UPDATE ON t
                BEGIN
                    SELECT RAISE(ABORT, 'no');
                END;
                CREATE INDEX ix_t ON t(x);
                """;
        List<String> out = SqlScript.splitStatements(sql);
        assertThat(out).hasSize(3);
        assertThat(out.get(1)).contains("CREATE TRIGGER").contains("BEGIN").contains("END");
        assertThat(out.get(2)).isEqualTo("CREATE INDEX ix_t ON t(x)");
    }

    @Test
    void handlesEscapedSingleQuotes() {
        List<String> out = SqlScript.splitStatements("INSERT INTO t VALUES ('O''Brien; Co'); SELECT 1;");
        assertThat(out).containsExactly("INSERT INTO t VALUES ('O''Brien; Co')", "SELECT 1");
    }
}
