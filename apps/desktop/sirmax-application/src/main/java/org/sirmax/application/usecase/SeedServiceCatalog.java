// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.application.usecase;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.sirmax.application.UseCase;
import org.sirmax.application.catalog.ServiceCatalogTemplates;
import org.sirmax.application.catalog.ServiceCategoryTemplate;
import org.sirmax.application.catalog.ServiceTemplate;
import org.sirmax.application.port.Clock;
import org.sirmax.application.port.IdGenerator;
import org.sirmax.application.port.ServiceCatalogRepository;
import org.sirmax.application.port.ServiceCatalogTemplateSource;
import org.sirmax.application.port.UnitOfWork;
import org.sirmax.application.security.Audit;
import org.sirmax.application.security.Session;
import org.sirmax.domain.common.ArchiveStatus;
import org.sirmax.domain.security.Permission;
import org.sirmax.domain.service.ServiceCategory;
import org.sirmax.domain.service.ServiceDefinition;
import org.sirmax.domain.service.ServiceDefinitionVersion;
import org.sirmax.shared.Result;

/**
 * Loads the editable seed templates into the catalog (master prompt §54).
 *
 * <p>Idempotent: a category or service whose code already exists is left untouched, so an operator
 * can re-run it after adding templates. Every seeded service gets a v1 {@code DRAFT} carrying the
 * template's requirements / workflow / fee rules / SLA / validity — the municipality reviews the
 * amounts and flow and publishes. Requires {@code service.configure}.
 */
public final class SeedServiceCatalog
        implements UseCase<SeedServiceCatalog.Command, SeedServiceCatalog.Summary> {

    public record Command(Session session, String source) {}

    /**
     * @param categoriesCreated new categories inserted
     * @param servicesCreated new services inserted (each with a v1 DRAFT)
     * @param servicesSkipped services whose code already existed
     */
    public record Summary(int categoriesCreated, int servicesCreated, int servicesSkipped) {}

    private final ServiceCatalogTemplateSource templates;
    private final ServiceCatalogRepository catalog;
    private final IdGenerator ids;
    private final Clock clock;
    private final UnitOfWork unitOfWork;
    private final Audit audit;

    public SeedServiceCatalog(
            ServiceCatalogTemplateSource templates,
            ServiceCatalogRepository catalog,
            IdGenerator ids,
            Clock clock,
            UnitOfWork unitOfWork,
            Audit audit) {
        this.templates = templates;
        this.catalog = catalog;
        this.ids = ids;
        this.clock = clock;
        this.unitOfWork = unitOfWork;
        this.audit = audit;
    }

    @Override
    public Result<Summary> execute(Command c) {
        if (!c.session().can(Permission.SERVICE_CONFIGURE)) {
            return Result.err("FORBIDDEN", "error.forbidden");
        }

        ServiceCatalogTemplates bundle = templates.load();

        // Every service must point at a category the bundle declares or the catalog already has.
        Map<String, String> categoryIdByCode = new HashMap<>();
        catalog.listActiveCategories().forEach(cat -> categoryIdByCode.put(key(cat.code()), cat.id()));
        for (ServiceCategoryTemplate cat : bundle.categories()) {
            categoryIdByCode.putIfAbsent(key(cat.code()), null); // known, id filled in below
        }
        for (ServiceTemplate t : bundle.services()) {
            if (!categoryIdByCode.containsKey(key(t.categoryCode()))) {
                return Result.err("INVALID_BUNDLE", "service.catalog.invalid_bundle");
            }
        }

        Summary summary = unitOfWork.execute(() -> seed(bundle, categoryIdByCode));

        audit.record(
                c.session().audit(c.source()),
                "service.catalog_seeded",
                "ServiceCatalog",
                bundle.country(),
                null,
                "bundle=" + bundle.country() + " v" + bundle.version(),
                "categories=" + summary.categoriesCreated()
                        + " services=" + summary.servicesCreated()
                        + " skipped=" + summary.servicesSkipped());
        return Result.ok(summary);
    }

    private Summary seed(ServiceCatalogTemplates bundle, Map<String, String> categoryIdByCode) {
        Instant now = clock.now();
        int categoriesCreated = 0;
        int servicesCreated = 0;
        int servicesSkipped = 0;

        for (ServiceCategoryTemplate cat : bundle.categories()) {
            String existingId = categoryIdByCode.get(key(cat.code()));
            if (existingId != null) {
                continue;
            }
            ServiceCategory created =
                    ServiceCategory.create(
                            ids.newId(), cat.code(), cat.name(), cat.sortOrder(), now);
            catalog.saveCategory(created);
            categoryIdByCode.put(key(cat.code()), created.id());
            categoriesCreated++;
        }

        for (ServiceTemplate t : bundle.services()) {
            if (catalog.findDefinitionByCode(t.code()).isPresent()) {
                servicesSkipped++;
                continue;
            }
            String categoryId = categoryIdByCode.get(key(t.categoryCode()));

            ServiceDefinition definition =
                    new ServiceDefinition(
                            ids.newId(),
                            t.code(),
                            categoryId,
                            t.name(),
                            t.description().orElse(null),
                            t.serviceType(),
                            null,
                            bundle.country(),
                            t.municipalOverrideAllowed(),
                            null,
                            ArchiveStatus.ACTIVE,
                            now,
                            now);
            catalog.saveDefinition(definition);

            ServiceDefinitionVersion draft =
                    ServiceDefinitionVersion.draft(ids.newId(), definition.id(), 1, now);
            draft.setRequiresPayment(t.requiresPayment());
            draft.setRequirements(t.requirements());
            draft.setWorkflow(t.workflow());
            draft.setFeeRules(t.feeRules());
            draft.setSla(t.sla());
            draft.setValidity(t.validity());
            t.numberingSequenceCode().ifPresent(draft::setNumberingSequenceCode);
            draft.setNotes(
                    t.notes()
                            .orElse(
                                    "Plantilla semilla. Revise requisitos, montos y flujo antes de"
                                            + " publicar."));
            catalog.saveVersion(draft);
            servicesCreated++;
        }

        return new Summary(categoriesCreated, servicesCreated, servicesSkipped);
    }

    private static String key(String code) {
        return code.strip().toLowerCase(java.util.Locale.ROOT);
    }
}
