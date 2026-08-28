// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import org.sirmax.application.UseCase;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.ServiceCatalogRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.security.Permission;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.shared.Result;

/**
 * Starts a new editable {@code DRAFT} version by cloning the service's current ACTIVE version
 * (docs/adr/0006 — old procedures keep their version; changes go into a new one). Requires
 * {@code service.configure}.
 */
public final class CreateServiceDraftVersion
        implements UseCase<CreateServiceDraftVersion.Command, ServiceDefinitionVersion> {

    public record Command(Session session, String definitionId, String source) {}

    private final ServiceCatalogRepository catalog;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public CreateServiceDraftVersion(
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
    public Result<ServiceDefinitionVersion> execute(Command c) {
        if (!c.session().can(Permission.SERVICE_CONFIGURE)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        if (catalog.findDefinitionById(c.definitionId()).isEmpty()) {
            return Result.err("DEFINITION_NOT_FOUND", "service.definition_not_found");
        }
        ServiceDefinitionVersion active = catalog.findActiveVersion(c.definitionId()).orElse(null);
        if (active == null) {
            // no active version to clone; also refuse if a draft already exists
            boolean hasDraft =
                    catalog.listVersions(c.definitionId()).stream()
                            .anyMatch(v -> v.status().isEditable());
            return Result.err(
                    hasDraft ? "DRAFT_EXISTS" : "NO_ACTIVE_VERSION",
                    hasDraft ? "service.draft_exists" : "service.no_active_version");
        }
        boolean hasDraft =
                catalog.listVersions(c.definitionId()).stream()
                        .anyMatch(v -> v.status().isEditable());
        if (hasDraft) {
            return Result.err("DRAFT_EXISTS", "service.draft_exists");
        }

        int next = catalog.nextVersionNumber(c.definitionId());
        ServiceDefinitionVersion draft =
                active.copyAsNewDraft(ids.newId(), next, clock.now());

        return Result.ok(
                unitOfWork.execute(
                        () -> {
                            catalog.saveVersion(draft);
                            audit.record(
                                    c.session().audit(c.source()),
                                    "service.draft_version_created",
                                    "ServiceDefinitionVersion",
                                    draft.id());
                            return draft;
                        }));
    }
}
