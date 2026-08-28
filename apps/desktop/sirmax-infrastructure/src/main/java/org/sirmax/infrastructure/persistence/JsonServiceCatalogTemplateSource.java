// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sirmax.application.catalog.ServiceCatalogTemplates;
import org.sirmax.application.catalog.ServiceCategoryTemplate;
import org.sirmax.application.catalog.ServiceTemplate;
import org.sirmax.application.port.ServiceCatalogTemplateSource;
import org.sirmax.domain.finance.FeeRule;
import org.sirmax.domain.service.RequirementDef;
import org.sirmax.domain.service.ServiceType;
import org.sirmax.domain.service.Sla;
import org.sirmax.domain.service.Validity;
import org.sirmax.domain.workflow.WorkflowDefinition;
import org.sirmax.shared.SirmaxException;

/**
 * Reads a bundled JSON pack of editable service templates (master prompt §54).
 *
 * <p>The typed sub-structures (requirements, workflow, fee rules, SLA, validity) share the exact
 * shapes {@link ServiceJson} already (de)serializes for the {@code service_definition_version}
 * columns, so a template and a stored version round-trip through the same code.
 *
 * <p>Format: {@code categories[]}, named {@code workflowTemplates}, named {@code requirementSets},
 * and {@code services[]} that reference a workflow and a requirement set by name (plus optional
 * inline {@code extraRequirements}). Fee amounts in the pack are {@code 0} placeholders — the
 * municipality sets them on the draft before publishing.
 */
public final class JsonServiceCatalogTemplateSource implements ServiceCatalogTemplateSource {

    /** The Dominican Republic bundle shipped with the desktop client. */
    public static final String DOMINICAN_REPUBLIC =
            "/catalog/dominican-republic/service-catalog.v1.json";

    private static final ObjectMapper M = new ObjectMapper();
    private static final String FEE_EFFECTIVE_FROM = "2026-01-01";
    private static final String FEE_LEGAL_REFERENCE =
            "Monto de referencia (0). Fíjelo según la ordenanza o resolución municipal vigente.";

    private final String resourcePath;

    public JsonServiceCatalogTemplateSource() {
        this(DOMINICAN_REPUBLIC);
    }

    public JsonServiceCatalogTemplateSource(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @Override
    public ServiceCatalogTemplates load() {
        JsonNode root = readRoot();

        String country = root.path("country").asText("DO");
        int version = root.path("version").asInt(1);

        Map<String, String> categoryNumbering = new LinkedHashMap<>();
        List<ServiceCategoryTemplate> categories = new ArrayList<>();
        for (JsonNode c : root.path("categories")) {
            String code = c.path("code").asText();
            categories.add(
                    new ServiceCategoryTemplate(
                            code, c.path("name").asText(), c.path("sortOrder").asInt(0)));
            String numbering = c.path("numbering").asText("");
            if (!numbering.isBlank()) {
                categoryNumbering.put(code.toLowerCase(java.util.Locale.ROOT), numbering);
            }
        }

        Map<String, WorkflowDefinition> workflowByName = new LinkedHashMap<>();
        Map<String, Integer> workflowDefaultSla = new LinkedHashMap<>();
        JsonNode wfNode = root.path("workflowTemplates");
        wfNode.fieldNames()
                .forEachRemaining(
                        name -> {
                            JsonNode entry = wfNode.get(name);
                            workflowByName.put(
                                    name, ServiceJson.workflowFromJson(entry.path("workflow").toString()));
                            workflowDefaultSla.put(name, entry.path("defaultSlaDays").asInt(0));
                        });

        Map<String, List<RequirementDef>> requirementSets = new LinkedHashMap<>();
        JsonNode reqNode = root.path("requirementSets");
        reqNode.fieldNames()
                .forEachRemaining(
                        name ->
                                requirementSets.put(
                                        name, ServiceJson.requirementsFromJson(reqNode.get(name).toString())));

        List<ServiceTemplate> services = new ArrayList<>();
        for (JsonNode s : root.path("services")) {
            services.add(
                    toTemplate(s, workflowByName, workflowDefaultSla, requirementSets, categoryNumbering));
        }

        return new ServiceCatalogTemplates(country, version, categories, services);
    }

    private ServiceTemplate toTemplate(
            JsonNode s,
            Map<String, WorkflowDefinition> workflowByName,
            Map<String, Integer> workflowDefaultSla,
            Map<String, List<RequirementDef>> requirementSets,
            Map<String, String> categoryNumbering) {

        String code = s.path("code").asText();
        String categoryCode = s.path("category").asText();

        String workflowName = s.path("workflow").asText();
        WorkflowDefinition workflow = workflowByName.get(workflowName);
        if (workflow == null) {
            throw new SirmaxException(
                    "Service template " + code + " references unknown workflow '" + workflowName + "'");
        }

        List<RequirementDef> requirements = new ArrayList<>();
        String requirementSetName = s.path("requirements").asText("");
        if (!requirementSetName.isBlank()) {
            List<RequirementDef> set = requirementSets.get(requirementSetName);
            if (set == null) {
                throw new SirmaxException(
                        "Service template "
                                + code
                                + " references unknown requirement set '"
                                + requirementSetName
                                + "'");
            }
            requirements.addAll(set);
        }
        if (s.has("extraRequirements")) {
            requirements.addAll(ServiceJson.requirementsFromJson(s.get("extraRequirements").toString()));
        }

        ServiceType serviceType = ServiceType.valueOf(s.path("serviceType").asText("CON_TASA"));
        boolean requiresPayment = s.path("requiresPayment").asBoolean(serviceType == ServiceType.CON_TASA);

        List<FeeRule> feeRules = List.of();
        if (s.has("fee")) {
            feeRules = feeRuleFrom(code, s.get("fee"));
        }

        int slaDays = s.has("slaDays") ? s.path("slaDays").asInt() : workflowDefaultSla.getOrDefault(workflowName, 0);
        Sla sla = slaDays > 0 ? Sla.businessDays(slaDays) : Sla.none();

        Validity validity =
                s.has("validity")
                        ? ServiceJson.validityFromJson(s.get("validity").toString())
                        : Validity.permanent();

        String numbering = s.path("numbering").asText("");
        if (numbering.isBlank()) {
            numbering = categoryNumbering.getOrDefault(categoryCode.toLowerCase(java.util.Locale.ROOT), "");
        }

        return new ServiceTemplate(
                code,
                s.path("name").asText(),
                categoryCode,
                optText(s, "description"),
                serviceType,
                requiresPayment,
                requirements,
                workflow,
                feeRules,
                sla,
                validity,
                numbering.isBlank() ? Optional.empty() : Optional.of(numbering),
                s.path("municipalOverrideAllowed").asBoolean(true),
                optText(s, "notes"));
    }

    private static List<FeeRule> feeRuleFrom(String serviceCode, JsonNode fee) {
        ObjectNode rule = M.createObjectNode();
        rule.put("id", serviceCode.toLowerCase(java.util.Locale.ROOT).replace('_', '-') + "-fee-1");
        rule.put("type", "FIXED");
        rule.put("chargeType", fee.path("chargeType").asText("TASA"));
        rule.put("concept", fee.path("concept").asText("Tasa del servicio"));
        rule.put("currency", fee.path("currency").asText("DOP"));
        rule.put("amountMinor", fee.path("amountMinor").asLong(0L));
        rule.put("effectiveFrom", fee.path("effectiveFrom").asText(FEE_EFFECTIVE_FROM));
        rule.put("legalReference", fee.path("legalReference").asText(FEE_LEGAL_REFERENCE));
        ArrayNode array = M.createArrayNode();
        array.add(rule);
        return ServiceJson.feeRulesFromJson(array.toString());
    }

    private JsonNode readRoot() {
        try (InputStream in = JsonServiceCatalogTemplateSource.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new SirmaxException("Service catalog template bundle not found: " + resourcePath);
            }
            return M.readTree(in);
        } catch (SirmaxException e) {
            throw e;
        } catch (Exception e) {
            throw new SirmaxException("Could not read service catalog template bundle: " + resourcePath, e);
        }
    }

    private static Optional<String> optText(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return (v == null || v.isNull() || v.asText().isBlank())
                ? Optional.empty()
                : Optional.of(v.asText());
    }
}
