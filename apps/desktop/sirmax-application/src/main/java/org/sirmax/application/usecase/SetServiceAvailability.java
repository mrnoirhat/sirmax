// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import org.sirmax.application.UseCase;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.ServiceCatalogRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.security.Permission;
import org.sirmax.domain.service.ServiceDefinition;
import org.sirmax.shared.Result;

/**
 * Activates or deactivates a whole service (master prompt §22 — never delete a service with
 * history; archive/deactivate). Requires {@code service.configure}.
 */
public final class SetServiceAvailability
        implements UseCase<SetServiceAvailability.Command, ServiceDefinition> {

    public record Command(Session session, String definitionId, boolean active, String source) {}

    private final ServiceCatalogRepository catalog;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public SetServiceAvailability(
            ServiceCatalogRepository catalog, Clock clock, UnitOfWork unitOfWork, Audit audit) {
        this.catalog = catalog;
        this.clock = clock;
        this.unitOfWork = unitOfWork;
        this.audit = audit;
    }

    @Override
    public Result<ServiceDefinition> execute(Command c) {
        if (!c.session().can(Permission.SERVICE_CONFIGURE)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }
        ServiceDefinition definition = catalog.findDefinitionById(c.definitionId()).orElse(null);
        if (definition == null) {
            return Result.err("DEFINITION_NOT_FOUND", "service.definition_not_found");
        }

        return Result.ok(
                unitOfWork.execute(
                        () -> {
                            if (c.active()) {
                                definition.reactivate(clock.now());
                            } else {
                                definition.deactivate(clock.now());
                            }
                            catalog.saveDefinition(definition);
                            audit.record(
                                    c.session().audit(c.source()),
                                    c.active() ? "service.activated" : "service.deactivated",
                                    "ServiceDefinition",
                                    definition.id());
                            return definition;
                        }));
    }
}
