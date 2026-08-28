// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sirmax.shared.i18n.MessageKey;

class MessagesTest {

    @BeforeAll
    static void useSpanish() {
        Messages.setLocale(Locale.of("es"));
    }

    @AfterAll
    static void restore() {
        Messages.setLocale(Locale.getDefault());
    }

    @Test
    void resolvesAKeyFromTheBaseBundle() {
        assertThat(Messages.get("app.brand")).isEqualTo("SIRMAX");
        assertThat(Messages.get("nav.home")).isEqualTo("Inicio");
    }

    @Test
    void missingKeyIsVisibleButNotAStackTrace() {
        assertThat(Messages.get("no.such.key.exists")).isEqualTo("!no.such.key.exists!");
    }

    @Test
    void interpolatesArguments() {
        assertThat(Messages.get("common.count.results", 3)).isEqualTo("3 resultados");
        assertThat(Messages.get("common.pending", 2)).isEqualTo("2 pendientes");
    }

    @Test
    void acceptsMessageKeyType() {
        assertThat(Messages.get(new MessageKey("nav.billing"))).isEqualTo("Facturación");
    }
}
