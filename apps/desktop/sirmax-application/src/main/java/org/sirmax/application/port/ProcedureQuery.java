// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.domain.procedure.ProcedureStatus;

/**
 * Filters for a worklist / queue query (master prompt §57).
 *
 * <p>Every field is optional; an empty {@code statuses} list means "every non-terminal status",
 * which is what an operator's default queue shows. {@code text} matches the case code loosely.
 *
 * @param onlyOverdue restrict to cases past their SLA due date
 */
public record ProcedureQuery(
        Optional<String> text,
        List<ProcedureStatus> statuses,
        Optional<String> departmentId,
        Optional<String> assignedUserId,
        Optional<String> serviceDefinitionId,
        boolean onlyOverdue,
        boolean unassignedOnly,
        int limit,
        int offset) {

    public ProcedureQuery {
        text = normalize(text);
        statuses = statuses == null ? List.of() : List.copyOf(statuses);
        departmentId = normalize(departmentId);
        assignedUserId = normalize(assignedUserId);
        serviceDefinitionId = normalize(serviceDefinitionId);
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be > 0");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0");
        }
    }

    /** Everything still open, most urgent first — the shell's default worklist. */
    public static ProcedureQuery openWork(int limit) {
        return new ProcedureQuery(
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false,
                false,
                limit,
                0);
    }

    /** One operator's queue. */
    public static ProcedureQuery assignedTo(String userId, int limit) {
        return new ProcedureQuery(
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.ofNullable(userId),
                Optional.empty(),
                false,
                false,
                limit,
                0);
    }

    /** Cases nobody has picked up — the "sin asignar" tile on the dashboard. */
    public static ProcedureQuery unassigned(int limit) {
        return new ProcedureQuery(
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false,
                true,
                limit,
                0);
    }

    /** Cases past their SLA date. */
    public static ProcedureQuery overdue(int limit) {
        return new ProcedureQuery(
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                true,
                false,
                limit,
                0);
    }

    public ProcedureQuery withPage(int newLimit, int newOffset) {
        return new ProcedureQuery(
                text,
                statuses,
                departmentId,
                assignedUserId,
                serviceDefinitionId,
                onlyOverdue,
                unassignedOnly,
                newLimit,
                newOffset);
    }

    private static Optional<String> normalize(Optional<String> v) {
        Objects.requireNonNullElse(v, Optional.empty());
        if (v == null || v.isEmpty()) {
            return Optional.empty();
        }
        String s = v.get().strip();
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }
}
