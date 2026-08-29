// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VerificationCodeTest {

    @Test
    void generatedCodesLookLikeCodesAndDoNotRepeat() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            VerificationCode code = VerificationCode.generate();
            assertThat(code.value()).matches("[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}");
            seen.add(code.value());
        }
        assertThat(seen).hasSize(500);
    }

    @Test
    void theAlphabetExcludesTheShapesThatGetMisreadOffAReceipt() {
        for (int i = 0; i < 300; i++) {
            assertThat(VerificationCode.generate().value())
                    .doesNotContain("O", "0", "I", "1", "L", "S", "5", "B", "8", "Z", "2");
        }
    }

    @Test
    void parseForgivesTheMistakesTheAlphabetAnticipates() {
        VerificationCode canonical = new VerificationCode("34AC-6799-QQTU");

        // lower case, no dashes, and the confusable characters someone would type instead
        assertThat(VerificationCode.parse("34ac 6799 qqtu")).isEqualTo(canonical);
        assertThat(VerificationCode.parse("34ACS799OOTU").value()).isEqualTo("34AC-3799-QQTU");
    }

    @Test
    void aWronglySizedCodeIsRejectedRatherThanGuessed() {
        assertThatThrownBy(() -> VerificationCode.parse("34AC-6799"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VerificationCode("nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theVerificationUrlHangsOffTheMunicipalitysOwnHost() {
        VerificationCode code = new VerificationCode("34AC-6799-QQTU");

        assertThat(code.verificationUrl("https://santiago.gob.do/"))
                .isEqualTo("https://santiago.gob.do/verificar/34AC-6799-QQTU");
        assertThat(code.verificationUrl("https://santiago.gob.do"))
                .isEqualTo("https://santiago.gob.do/verificar/34AC-6799-QQTU");
    }
}
