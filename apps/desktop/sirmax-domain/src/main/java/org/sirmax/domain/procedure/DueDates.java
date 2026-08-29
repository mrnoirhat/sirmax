// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.procedure;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.domain.service.Sla;

/**
 * Turns a service version's {@link Sla} into a concrete due date (master prompt §30).
 *
 * <p>Business days skip Saturday and Sunday. Public holidays are municipality-specific
 * configuration, not a hard-coded calendar, so they are not baked in here — an SLA is an internal
 * management target, never a legal deadline claim (§60).
 */
public final class DueDates {

    private DueDates() {}

    /** The due date for a case opened on {@code openedOn}, or empty when the service declares no SLA. */
    public static Optional<LocalDate> dueDateFor(Sla sla, LocalDate openedOn) {
        Objects.requireNonNull(sla, "sla");
        Objects.requireNonNull(openedOn, "openedOn");
        if (!sla.isDefined()) {
            return Optional.empty();
        }
        return Optional.of(
                switch (sla.basis()) {
                    case CALENDAR_DAYS -> openedOn.plusDays(sla.targetDays());
                    case BUSINESS_DAYS -> addBusinessDays(openedOn, sla.targetDays());
                });
    }

    /** {@code from} plus {@code days} working days (Mon–Fri), not counting {@code from} itself. */
    public static LocalDate addBusinessDays(LocalDate from, int days) {
        Objects.requireNonNull(from, "from");
        if (days < 0) {
            throw new IllegalArgumentException("days must be >= 0");
        }
        LocalDate date = from;
        int added = 0;
        while (added < days) {
            date = date.plusDays(1);
            if (isBusinessDay(date)) {
                added++;
            }
        }
        return date;
    }

    public static boolean isBusinessDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    /**
     * Days remaining until {@code dueDate} as of {@code today}; negative when overdue. Used by the
     * queue view to colour cases (§57).
     */
    public static long daysRemaining(LocalDate today, LocalDate dueDate) {
        return java.time.temporal.ChronoUnit.DAYS.between(today, dueDate);
    }
}
