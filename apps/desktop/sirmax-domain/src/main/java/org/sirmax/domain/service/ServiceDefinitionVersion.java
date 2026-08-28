// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sirmax.domain.finance.FeeRule;
import org.sirmax.domain.workflow.WorkflowDefinition;
import org.sirmax.shared.JsonDoc;

/**
 * One immutable-once-published version of a service's configuration (docs/adr/0006).
 *
 * <p>Editable only while {@link ServiceStatus#DRAFT}. The configurable parts are now typed:
 * {@link RequirementDef requirements}, {@link FormSchema}, {@link WorkflowDefinition} and {@link
 * FeeRule fee rules}, plus {@link Sla} and {@link Validity}. Output-document definitions and
 * authorization rules remain validated JSON until they gain typed models (Phase 7/8).
 */
public final class ServiceDefinitionVersion {

    private final String id;
    private final String serviceDefinitionId;
    private final int versionNumber;
    private ServiceStatus status;
    private boolean requiresPayment;
    private String numberingSequenceCode; // nullable
    private String notes; // nullable

    private final List<RequirementDef> requirements = new ArrayList<>();
    private final List<FeeRule> feeRules = new ArrayList<>();
    private FormSchema formSchema;
    private WorkflowDefinition workflow;
    private Sla sla;
    private Validity validity;
    private JsonDoc outputDocuments;
    private JsonDoc authorization;

    private final Instant createdAt;
    private Instant publishedAt; // nullable until published

    public ServiceDefinitionVersion(
            String id,
            String serviceDefinitionId,
            int versionNumber,
            ServiceStatus status,
            boolean requiresPayment,
            String numberingSequenceCode,
            String notes,
            List<RequirementDef> requirements,
            List<FeeRule> feeRules,
            FormSchema formSchema,
            WorkflowDefinition workflow,
            Sla sla,
            Validity validity,
            JsonDoc outputDocuments,
            JsonDoc authorization,
            Instant createdAt,
            Instant publishedAt) {
        this.id = requireText(id, "id");
        this.serviceDefinitionId = requireText(serviceDefinitionId, "serviceDefinitionId");
        if (versionNumber < 1) {
            throw new IllegalArgumentException("versionNumber must be >= 1");
        }
        this.versionNumber = versionNumber;
        this.status = Objects.requireNonNull(status, "status");
        this.requiresPayment = requiresPayment;
        this.numberingSequenceCode = blankToNull(numberingSequenceCode);
        this.notes = blankToNull(notes);
        if (requirements != null) {
            this.requirements.addAll(requirements);
        }
        if (feeRules != null) {
            this.feeRules.addAll(feeRules);
        }
        this.formSchema = formSchema == null ? FormSchema.empty() : formSchema;
        this.workflow = workflow == null ? WorkflowDefinition.empty() : workflow;
        this.sla = sla == null ? Sla.none() : sla;
        this.validity = validity == null ? Validity.permanent() : validity;
        this.outputDocuments = outputDocuments == null ? JsonDoc.EMPTY_ARRAY : outputDocuments;
        this.authorization = authorization == null ? JsonDoc.EMPTY_OBJECT : authorization;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.publishedAt = publishedAt;
    }

    /** A brand-new empty DRAFT version. */
    public static ServiceDefinitionVersion draft(
            String id, String serviceDefinitionId, int versionNumber, Instant now) {
        return new ServiceDefinitionVersion(
                id,
                serviceDefinitionId,
                versionNumber,
                ServiceStatus.DRAFT,
                false,
                null,
                null,
                List.of(),
                List.of(),
                FormSchema.empty(),
                WorkflowDefinition.empty(),
                Sla.none(),
                Validity.permanent(),
                JsonDoc.EMPTY_ARRAY,
                JsonDoc.EMPTY_OBJECT,
                now,
                null);
    }

    /** Clone this version's configuration into a new DRAFT with a higher version number. */
    public ServiceDefinitionVersion copyAsNewDraft(String newId, int newVersionNumber, Instant now) {
        if (newVersionNumber <= versionNumber) {
            throw new IllegalArgumentException("newVersionNumber must be greater than this version");
        }
        return new ServiceDefinitionVersion(
                newId,
                serviceDefinitionId,
                newVersionNumber,
                ServiceStatus.DRAFT,
                requiresPayment,
                numberingSequenceCode,
                notes,
                new ArrayList<>(requirements),
                new ArrayList<>(feeRules),
                formSchema,
                workflow,
                sla,
                validity,
                outputDocuments,
                authorization,
                now,
                null);
    }

    // ── accessors ──

    public String id() {
        return id;
    }

    public String serviceDefinitionId() {
        return serviceDefinitionId;
    }

    public int versionNumber() {
        return versionNumber;
    }

    public ServiceStatus status() {
        return status;
    }

    public boolean requiresPayment() {
        return requiresPayment;
    }

    public Optional<String> numberingSequenceCode() {
        return Optional.ofNullable(numberingSequenceCode);
    }

    public Optional<String> notes() {
        return Optional.ofNullable(notes);
    }

    public List<RequirementDef> requirements() {
        return Collections.unmodifiableList(requirements);
    }

    public List<FeeRule> feeRules() {
        return Collections.unmodifiableList(feeRules);
    }

    public FormSchema formSchema() {
        return formSchema;
    }

    public WorkflowDefinition workflow() {
        return workflow;
    }

    public Sla sla() {
        return sla;
    }

    public Validity validity() {
        return validity;
    }

    public JsonDoc outputDocuments() {
        return outputDocuments;
    }

    public JsonDoc authorization() {
        return authorization;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Optional<Instant> publishedAt() {
        return Optional.ofNullable(publishedAt);
    }

    // ── lifecycle ──

    /** DRAFT → ACTIVE. Callers validate the configuration first. */
    public void publish(Instant now) {
        if (status != ServiceStatus.DRAFT) {
            throw new IllegalStateException("Only a DRAFT version can be published (is " + status + ")");
        }
        this.status = ServiceStatus.ACTIVE;
        this.publishedAt = Objects.requireNonNull(now, "now");
    }

    /** ACTIVE → INACTIVE (also used when a newer version supersedes this one). */
    public void deactivate() {
        if (status != ServiceStatus.ACTIVE) {
            throw new IllegalStateException("Only an ACTIVE version can be deactivated (is " + status + ")");
        }
        this.status = ServiceStatus.INACTIVE;
    }

    public void reactivate() {
        if (status != ServiceStatus.INACTIVE) {
            throw new IllegalStateException("Only an INACTIVE version can be reactivated (is " + status + ")");
        }
        this.status = ServiceStatus.ACTIVE;
    }

    public void archive() {
        if (status == ServiceStatus.ARCHIVED) {
            return;
        }
        this.status = ServiceStatus.ARCHIVED;
    }

    // ── configuration (DRAFT only) ──

    public void setRequirements(List<RequirementDef> newRequirements) {
        requireDraft();
        this.requirements.clear();
        if (newRequirements != null) {
            this.requirements.addAll(newRequirements);
        }
    }

    public void setFeeRules(List<FeeRule> newFeeRules) {
        requireDraft();
        this.feeRules.clear();
        if (newFeeRules != null) {
            this.feeRules.addAll(newFeeRules);
        }
    }

    public void setRequiresPayment(boolean value) {
        requireDraft();
        this.requiresPayment = value;
    }

    public void setNumberingSequenceCode(String code) {
        requireDraft();
        this.numberingSequenceCode = blankToNull(code);
    }

    public void setNotes(String value) {
        requireDraft();
        this.notes = blankToNull(value);
    }

    public void setSla(Sla value) {
        requireDraft();
        this.sla = Objects.requireNonNull(value, "sla");
    }

    public void setValidity(Validity value) {
        requireDraft();
        this.validity = Objects.requireNonNull(value, "validity");
    }

    public void setFormSchema(FormSchema value) {
        requireDraft();
        this.formSchema = Objects.requireNonNull(value, "formSchema");
    }

    public void setWorkflow(WorkflowDefinition value) {
        requireDraft();
        this.workflow = Objects.requireNonNull(value, "workflow");
    }

    public void setOutputDocuments(JsonDoc value) {
        requireDraft();
        this.outputDocuments = Objects.requireNonNull(value, "outputDocuments");
    }

    public void setAuthorization(JsonDoc value) {
        requireDraft();
        this.authorization = Objects.requireNonNull(value, "authorization");
    }

    private void requireDraft() {
        if (status != ServiceStatus.DRAFT) {
            throw new IllegalStateException(
                    "A published service version is immutable (status " + status + ")");
        }
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ServiceDefinitionVersion v && id.equals(v.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
