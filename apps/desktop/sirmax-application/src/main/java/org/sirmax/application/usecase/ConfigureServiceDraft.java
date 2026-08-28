// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.util.List;
import java.util.Optional;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.ServiceCatalogRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.finance.FeeRule;
import org.sirmax.domain.security.Permission;
import org.sirmax.domain.service.FormSchema;
import org.sirmax.domain.service.RequirementDef;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.domain.service.ServiceStatus;
import org.sirmax.domain.service.Sla;
import org.sirmax.domain.service.Validity;
import org.sirmax.domain.workflow.WorkflowDefinition;
import org.sirmax.shared.JsonDoc;
import org.sirmax.shared.Result;

/**
 * Applies configuration to a {@code DRAFT} service version. Each field of the command is optional —
 * {@code null}/{@link Optional#empty()} leaves that part unchanged. Requires {@code service.configure}.
 */
public final class ConfigureServiceDraft
        implements UseCase<ConfigureServiceDraft.Command, ServiceDefinitionVersion> {

    public record Command(
            Session session,
            String versionId,
            Optional<List<RequirementDef>> requirements,
            Optional<Boolean> requiresPayment,
            Optional<Sla> sla,
            Optional<Validity> validity,
            Optional<FormSchema> formSchema,
            Optional<WorkflowDefinition> workflow,
            Optional<List<FeeRule>> feeRules,
            Optional<JsonDoc> outputDocuments,
            Optional<JsonDoc> authorization,
            Optional<String> numberingSequenceCode,
            Optional<String> notes,
            String source) {}

    private final ServiceCatalogRepository catalog;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public ConfigureServiceDraft(
            ServiceCatalogRepository catalog, UnitOfWork unitOfWork, Audit audit) {
        this.catalog = catalog;
        this.unitOfWork = unitOfWork;
        this.audit = audit;
    }

    @Override
    public Result<ServiceDefinitionVersion> execute(Command c) {
        if (!c.session().can(Permission.SERVICE_CONFIGURE)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        ServiceDefinitionVersion version = catalog.findVersionById(c.versionId()).orElse(null);
        if (version == null) {
            return Result.err("VERSION_NOT_FOUND", "service.version_not_found");
        }
        if (version.status() != ServiceStatus.DRAFT) {
            return Result.err("NOT_DRAFT", "service.version_not_draft");
        }

        try {
            return Result.ok(unitOfWork.execute(() -> apply(c, version)));
        } catch (IllegalArgumentException e) {
            return Result.err("INVALID_CONFIG", "service.invalid_config");
        }
    }

    private ServiceDefinitionVersion apply(Command c, ServiceDefinitionVersion v) {
        c.requirements().ifPresent(v::setRequirements);
        c.requiresPayment().ifPresent(v::setRequiresPayment);
        c.sla().ifPresent(v::setSla);
        c.validity().ifPresent(v::setValidity);
        c.formSchema().ifPresent(v::setFormSchema);
        c.workflow().ifPresent(v::setWorkflow);
        c.feeRules().ifPresent(v::setFeeRules);
        c.outputDocuments().ifPresent(v::setOutputDocuments);
        c.authorization().ifPresent(v::setAuthorization);
        c.numberingSequenceCode().ifPresent(v::setNumberingSequenceCode);
        c.notes().ifPresent(v::setNotes);
        catalog.saveVersion(v);
        audit.record(
                c.session().audit(c.source()),
                "service.configured",
                "ServiceDefinitionVersion",
                v.id());
        return v;
    }
}
