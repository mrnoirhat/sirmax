// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.port;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sirmax.domain.common.PartyRef;
import org.sirmax.domain.procedure.Procedure;
import org.sirmax.domain.procedure.ProcedureAttachment;
import org.sirmax.domain.procedure.ProcedureEvent;
import org.sirmax.domain.procedure.ProcedureRequirementItem;

/**
 * Persistence for cases: the {@link Procedure} aggregate plus its checklist, form answers, timeline
 * and attachments (master prompt §16).
 *
 * <p>Kept as one port rather than four because they are written together inside a single
 * transaction and never queried independently of the case they belong to.
 */
public interface ProcedureRepository {

    void save(Procedure procedure);

    Optional<Procedure> findById(String id);

    Optional<Procedure> findByCode(String code);

    /** Every case opened by a party, newest first — the citizen history panel (§58). */
    List<Procedure> findByApplicant(PartyRef applicant, int limit);

    /**
     * Worklist query (§57). All filters are optional: a null/blank value means "any".
     *
     * @param statuses restrict to these statuses; empty means "every non-terminal status"
     */
    List<Procedure> search(ProcedureQuery query);

    long countSearch(ProcedureQuery query);

    // ── checklist ──
    void saveRequirement(ProcedureRequirementItem item);

    List<ProcedureRequirementItem> findRequirements(String procedureId);

    Optional<ProcedureRequirementItem> findRequirement(String procedureId, String requirementKey);

    // ── dynamic form answers ──
    void saveFormValues(String procedureId, Map<String, String> values);

    Map<String, String> findFormValues(String procedureId);

    // ── timeline ──
    void appendEvent(ProcedureEvent event);

    List<ProcedureEvent> findEvents(String procedureId);

    // ── attachments ──
    void saveAttachment(ProcedureAttachment attachment);

    List<ProcedureAttachment> findAttachments(String procedureId);
}
