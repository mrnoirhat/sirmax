// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.procedure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.sirmax.domain.service.Sla;

class DueDatesTest {

    /** A Thursday, so a 3-business-day target has to cross a weekend. */
    private static final LocalDate THURSDAY = LocalDate.of(2026, 3, 5);

    @Test
    void businessDaysSkipTheWeekend() {
        assertThat(DueDates.dueDateFor(Sla.businessDays(3), THURSDAY))
                .contains(LocalDate.of(2026, 3, 10)); // Fri, Mon, Tue
    }

    @Test
    void calendarDaysDoNot() {
        Sla sla = new Sla(3, Sla.Basis.CALENDAR_DAYS, java.util.OptionalInt.empty());

        assertThat(DueDates.dueDateFor(sla, THURSDAY)).contains(LocalDate.of(2026, 3, 8));
    }

    @Test
    void aServiceWithoutAnSlaHasNoDueDate() {
        assertThat(DueDates.dueDateFor(Sla.none(), THURSDAY)).isEmpty();
    }

    @Test
    void daysRemainingGoesNegativeOnceOverdue() {
        assertThat(DueDates.daysRemaining(THURSDAY, LocalDate.of(2026, 3, 10))).isEqualTo(5);
        assertThat(DueDates.daysRemaining(THURSDAY, LocalDate.of(2026, 3, 3))).isEqualTo(-2);
    }
}
