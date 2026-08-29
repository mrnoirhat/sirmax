// SPDX-License-Identifier: AGPL-3.0-or-later
/**
 * The generic procedure (trámite) model — master prompt §16.
 *
 * <p>One aggregate, {@link org.sirmax.domain.procedure.Procedure}, carries every kind of municipal
 * case. What differs between a birth certificate, a construction permit and a noise complaint is the
 * service version the case was opened with, never a parallel class hierarchy.
 *
 * <p>Around it: the materialized requirement checklist
 * ({@link org.sirmax.domain.procedure.ProcedureRequirementItem},
 * {@link org.sirmax.domain.procedure.ProcedureChecklist}), the append-only timeline
 * ({@link org.sirmax.domain.procedure.ProcedureEvent}), attachments and SLA due-date arithmetic
 * ({@link org.sirmax.domain.procedure.DueDates}).
 */
package org.sirmax.domain.procedure;
