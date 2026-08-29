// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.procedure;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.domain.service.RequirementDef;
import org.sirmax.domain.service.RequirementKind;
import org.sirmax.domain.service.RequirementStage;

/**
 * One line of a procedure's materialized checklist (master prompt §56).
 *
 * <p>The label/kind/stage are copied from the {@link RequirementDef} of the service version at open
 * time, so a later edit of the service never rewrites history on an in-flight case.
 *
 * <p>A waived item counts as satisfied but stays visibly distinct: the operator who waived it and
 * their note are part of the record.
 */
public final class ProcedureRequirementItem {

    private final String id;
    private final String procedureId;
    private final String requirementKey;
    private final String label;
    private final RequirementKind kind;
    private final RequirementStage stage;
    private final boolean required;
    private final String conditionExpression; // nullable — null means "always applies"

    private boolean satisfied;
    private boolean waived;
    private String note; // nullable
    private Instant satisfiedAt; // nullable
    private String satisfiedBy; // nullable user id

    public ProcedureRequirementItem(
            String id,
            String procedureId,
            String requirementKey,
            String label,
            RequirementKind kind,
            RequirementStage stage,
            boolean required,
            String conditionExpression,
            boolean satisfied,
            boolean waived,
            String note,
            Instant satisfiedAt,
            String satisfiedBy) {
        this.id = Objects.requireNonNull(id, "id");
        this.procedureId = Objects.requireNonNull(procedureId, "procedureId");
        this.requirementKey = Objects.requireNonNull(requirementKey, "requirementKey");
        this.label = Objects.requireNonNull(label, "label");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.stage = Objects.requireNonNull(stage, "stage");
        this.required = required;
        this.conditionExpression = blankToNull(conditionExpression);
        this.satisfied = satisfied;
        this.waived = waived;
        this.note = note;
        this.satisfiedAt = satisfiedAt;
        this.satisfiedBy = satisfiedBy;
    }

    /** Materialize a checklist line from the service version's declared requirement. */
    public static ProcedureRequirementItem from(String id, String procedureId, RequirementDef def) {
        return new ProcedureRequirementItem(
                id,
                procedureId,
                def.key(),
                def.label(),
                def.kind(),
                def.stage(),
                def.required(),
                def.conditionExpression().orElse(null),
                false,
                false,
                null,
                null,
                null);
    }

    public String id() {
        return id;
    }

    public String procedureId() {
        return procedureId;
    }

    public String requirementKey() {
        return requirementKey;
    }

    public String label() {
        return label;
    }

    public RequirementKind kind() {
        return kind;
    }

    public RequirementStage stage() {
        return stage;
    }

    public boolean required() {
        return required;
    }

    /** The condition under which this requirement applies at all; empty means "always". */
    public Optional<String> conditionExpression() {
        return Optional.ofNullable(conditionExpression);
    }

    /** Satisfied outright or waived by an authorized operator — both clear the blocker. */
    public boolean isSatisfied() {
        return satisfied || waived;
    }

    public boolean isWaived() {
        return waived;
    }

    public Optional<String> note() {
        return Optional.ofNullable(note);
    }

    public Optional<Instant> satisfiedAt() {
        return Optional.ofNullable(satisfiedAt);
    }

    public Optional<String> satisfiedBy() {
        return Optional.ofNullable(satisfiedBy);
    }

    /** Still blocking: mandatory and neither provided nor waived. */
    public boolean isPending() {
        return required && !isSatisfied();
    }

    public void markSatisfied(String userId, String reason, Instant now) {
        this.satisfied = true;
        this.waived = false;
        this.note = blankToNull(reason);
        this.satisfiedAt = now;
        this.satisfiedBy = userId;
    }

    public void markMissing(Instant now) {
        this.satisfied = false;
        this.waived = false;
        this.satisfiedAt = null;
        this.satisfiedBy = null;
        // the note is kept: it usually explains what is still wrong
        Objects.requireNonNull(now, "now");
    }

    /**
     * Accept the case without this requirement. A waiver must be justified — it is the audited
     * escape hatch operators reach for at the counter (§56).
     */
    public void waive(String userId, String reason, Instant now) {
        String r = blankToNull(reason);
        if (r == null) {
            throw new IllegalArgumentException("A waiver must carry a reason");
        }
        this.waived = true;
        this.satisfied = false;
        this.note = r;
        this.satisfiedAt = now;
        this.satisfiedBy = userId;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String v = value.strip();
        return v.isEmpty() ? null : v;
    }
}
