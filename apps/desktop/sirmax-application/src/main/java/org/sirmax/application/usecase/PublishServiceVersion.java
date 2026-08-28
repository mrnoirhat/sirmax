// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.util.List;
import org.sirmax.application.UseCase;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.ServiceCatalogRepository;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.security.Permission;
import org.sirmax.domain.service.ServiceDefinition;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.domain.service.ServiceStatus;
import org.sirmax.domain.service.ServiceVersionValidator;
import org.sirmax.shared.Result;

/**
 * Publishes a {@code DRAFT} service version: validates it, deactivates the previously ACTIVE version
 * of the same service, sets it ACTIVE, and points the definition's {@code currentVersionId} at it
 * (docs/adr/0006). Requires {@code service.configure}.
 */
public final class PublishServiceVersion
        implements UseCase<PublishServiceVersion.Command, ServiceDefinitionVersion> {

    public record Command(Session session, String versionId, String source) {}

    private final ServiceCatalogRepository catalog;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public PublishServiceVersion(
            ServiceCatalogRepository catalog, Clock clock, UnitOfWork unitOfWork, Audit audit) {
        this.catalog = catalog;
        this.clock = clock;
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
        ServiceDefinition definition =
                catalog.findDefinitionById(version.serviceDefinitionId()).orElse(null);
        if (definition == null) {
            return Result.err("DEFINITION_NOT_FOUND", "service.definition_not_found");
        }

        List<String> problems =
                ServiceVersionValidator.validate(version, definition.serviceType());
        if (!problems.isEmpty()) {
            return Result.err("VALIDATION_FAILED", problems.get(0));
        }

        return Result.ok(unitOfWork.execute(() -> doPublish(c, version, definition)));
    }

    private ServiceDefinitionVersion doPublish(
            Command c, ServiceDefinitionVersion version, ServiceDefinition definition) {
        Instant now = clock.now();

        catalog
                .findActiveVersion(definition.id())
                .filter(active -> !active.id().equals(version.id()))
                .ifPresent(
                        active -> {
                            active.deactivate();
                            catalog.saveVersion(active);
                        });

        version.publish(now);
        catalog.saveVersion(version);

        definition.setCurrentVersion(version.id(), now);
        catalog.saveDefinition(definition);

        audit.record(
                c.session().audit(c.source()),
                "service.version_published",
                "ServiceDefinitionVersion",
                version.id());
        return version;
    }
}
