// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.ServiceCatalogRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.security.Permission;
import org.sirmax.domain.service.ServiceDefinition;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.domain.service.ServiceType;
import org.sirmax.shared.Result;

/**
 * Creates a new service in the catalog with an empty v1 {@code DRAFT} version to configure
 * (master prompt §22, §55). Requires {@code service.configure}.
 */
public final class CreateServiceDraft implements UseCase<CreateServiceDraft.Command, CreateServiceDraft.Created> {

    public record Command(
            Session session,
            String code,
            String categoryId,
            String name,
            ServiceType serviceType,
            String countryScope,
            String source) {}

    public record Created(String definitionId, String draftVersionId) {}

    private final ServiceCatalogRepository catalog;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public CreateServiceDraft(
            ServiceCatalogRepository catalog,
            IdGenerator ids,
            Clock clock,
            UnitOfWork unitOfWork,
            Audit audit) {
        this.catalog = catalog;
        this.ids = ids;
        this.clock = clock;
        this.unitOfWork = unitOfWork;
        this.audit = audit;
    }

    @Override
    public Result<Created> execute(Command c) {
        if (!c.session().can(Permission.SERVICE_CONFIGURE)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        if (catalog.findCategoryById(c.categoryId()).isEmpty()) {
            return Result.err("CATEGORY_NOT_FOUND", "service.category_not_found");
        }
        ServiceDefinition definition;
        try {
            definition =
                    ServiceDefinition.create(
                            ids.newId(),
                            c.code(),
                            c.categoryId(),
                            c.name(),
                            c.serviceType(),
                            c.countryScope(),
                            clock.now());
        } catch (IllegalArgumentException e) {
            return Result.err("INVALID_SERVICE", "service.invalid");
        }
        if (catalog.findDefinitionByCode(definition.code()).isPresent()) {
            return Result.err("CODE_TAKEN", "service.code_taken");
        }

        return Result.ok(unitOfWork.execute(() -> doCreate(c, definition)));
    }

    private Created doCreate(Command c, ServiceDefinition definition) {
        Instant now = clock.now();
        catalog.saveDefinition(definition);

        ServiceDefinitionVersion draft =
                ServiceDefinitionVersion.draft(ids.newId(), definition.id(), 1, now);
        catalog.saveVersion(draft);

        audit.record(
                c.session().audit(c.source()),
                "service.created",
                "ServiceDefinition",
                definition.id());
        return new Created(definition.id(), draft.id());
    }
}
