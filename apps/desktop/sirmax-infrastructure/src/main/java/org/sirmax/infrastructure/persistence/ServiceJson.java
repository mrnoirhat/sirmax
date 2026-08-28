// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.sirmax.domain.finance.ChargeType;
import org.sirmax.domain.finance.FeeRule;
import org.sirmax.domain.finance.FeeRuleType;
import org.sirmax.domain.finance.FeeTier;
import org.sirmax.domain.service.FieldType;
import org.sirmax.domain.service.FormField;
import org.sirmax.domain.service.FormSchema;
import org.sirmax.domain.service.RequirementDef;
import org.sirmax.domain.service.RequirementKind;
import org.sirmax.domain.service.RequirementStage;
import org.sirmax.domain.service.Sla;
import org.sirmax.domain.service.Validity;
import org.sirmax.domain.workflow.StepType;
import org.sirmax.domain.workflow.Transition;
import org.sirmax.domain.workflow.TransitionKind;
import org.sirmax.domain.workflow.WorkflowDefinition;
import org.sirmax.domain.workflow.WorkflowStep;
import org.sirmax.shared.SirmaxException;

/**
 * Serializes the typed parts of a service version to/from the {@code *_json} columns.
 *
 * <p>Explicit node building (no annotations) keeps the domain model framework-free. Only {@code
 * output_documents_json} and {@code authorization_json} remain opaque {@code JsonDoc} strings stored
 * verbatim and do not pass through here.
 */
final class ServiceJson {

    private static final ObjectMapper M = new ObjectMapper();

    private ServiceJson() {}

    // ── requirements ──

    static String requirementsToJson(List<RequirementDef> requirements) {
        ArrayNode array = M.createArrayNode();
        for (RequirementDef r : requirements) {
            ObjectNode o = array.addObject();
            o.put("key", r.key());
            o.put("label", r.label());
            o.put("kind", r.kind().name());
            o.put("stage", r.stage().name());
            o.put("required", r.required());
            r.conditionExpression().ifPresent(v -> o.put("condition", v));
            r.validationRule().ifPresent(v -> o.put("validation", v));
        }
        return write(array);
    }

    static List<RequirementDef> requirementsFromJson(String json) {
        List<RequirementDef> out = new ArrayList<>();
        JsonNode root = read(json);
        if (root != null && root.isArray()) {
            for (JsonNode n : root) {
                out.add(
                        new RequirementDef(
                                n.path("key").asText(),
                                n.path("label").asText(),
                                RequirementKind.valueOf(n.path("kind").asText("DOCUMENT")),
                                RequirementStage.valueOf(n.path("stage").asText("INTAKE")),
                                n.path("required").asBoolean(true),
                                optText(n, "condition"),
                                optText(n, "validation")));
            }
        }
        return out;
    }

    // ── fee rules ──

    static String feeRulesToJson(List<FeeRule> rules) {
        ArrayNode array = M.createArrayNode();
        for (FeeRule r : rules) {
            ObjectNode o = array.addObject();
            o.put("id", r.id());
            o.put("type", r.type().name());
            o.put("chargeType", r.chargeType().name());
            o.put("concept", r.concept());
            o.put("currency", r.currencyCode());
            o.put("amountMinor", r.amountMinor());
            o.put("unitRateMinor", r.unitRateMinor());
            o.put("ratePerAreaMinor", r.ratePerAreaMinor());
            o.put("ratePerDayMinor", r.ratePerDayMinor());
            o.put("effectiveFrom", r.effectiveFrom().toString());
            r.effectiveTo().ifPresent(d -> o.put("effectiveTo", d.toString()));
            r.legalReference().ifPresent(s -> o.put("legalReference", s));
            if (!r.byKey().isEmpty()) {
                ObjectNode byKey = o.putObject("byKey");
                r.byKey().forEach(byKey::put);
            }
            if (!r.tiers().isEmpty()) {
                ArrayNode tiers = o.putArray("tiers");
                for (FeeTier t : r.tiers()) {
                    ObjectNode to = tiers.addObject();
                    to.put("upToQuantity", t.upToQuantity());
                    to.put("unitRateMinor", t.unitRateMinor());
                }
            }
        }
        return write(array);
    }

    static List<FeeRule> feeRulesFromJson(String json) {
        List<FeeRule> out = new ArrayList<>();
        JsonNode root = read(json);
        if (root != null && root.isArray()) {
            for (JsonNode n : root) {
                Map<String, Long> byKey = new LinkedHashMap<>();
                if (n.has("byKey")) {
                    n.get("byKey").fields().forEachRemaining(e -> byKey.put(e.getKey(), e.getValue().asLong()));
                }
                List<FeeTier> tiers = new ArrayList<>();
                if (n.has("tiers")) {
                    for (JsonNode t : n.get("tiers")) {
                        tiers.add(
                                new FeeTier(
                                        t.path("upToQuantity").asLong(), t.path("unitRateMinor").asLong()));
                    }
                }
                out.add(
                        new FeeRule(
                                n.path("id").asText(),
                                FeeRuleType.valueOf(n.path("type").asText("FIXED")),
                                ChargeType.valueOf(n.path("chargeType").asText("TASA")),
                                n.path("concept").asText(),
                                n.path("currency").asText("DOP"),
                                n.path("amountMinor").asLong(),
                                n.path("unitRateMinor").asLong(),
                                n.path("ratePerAreaMinor").asLong(),
                                n.path("ratePerDayMinor").asLong(),
                                byKey,
                                tiers,
                                LocalDate.parse(n.path("effectiveFrom").asText("2026-01-01")),
                                optText(n, "effectiveTo").map(LocalDate::parse),
                                optText(n, "legalReference")));
            }
        }
        return out;
    }

    // ── workflow ──

    static String workflowToJson(WorkflowDefinition wf) {
        ObjectNode root = M.createObjectNode();
        root.put("firstStepKey", wf.firstStepKey());
        ArrayNode steps = root.putArray("steps");
        for (WorkflowStep s : wf.steps()) {
            ObjectNode so = steps.addObject();
            so.put("key", s.key());
            so.put("label", s.label());
            so.put("type", s.type().name());
            s.requiredPermission().ifPresent(p -> so.put("requiredPermission", p));
            so.put("slaDays", s.slaDays());
            ArrayNode ts = so.putArray("transitions");
            for (Transition t : s.transitions()) {
                ObjectNode to = ts.addObject();
                to.put("kind", t.kind().name());
                t.toStepKey().ifPresent(k -> to.put("to", k));
                t.condition().ifPresent(c -> to.put("condition", c));
            }
        }
        return write(root);
    }

    static WorkflowDefinition workflowFromJson(String json) {
        JsonNode root = read(json);
        if (root == null || !root.isObject() || !root.has("steps") || root.get("steps").isEmpty()) {
            return WorkflowDefinition.empty();
        }
        List<WorkflowStep> steps = new ArrayList<>();
        for (JsonNode s : root.get("steps")) {
            List<Transition> transitions = new ArrayList<>();
            for (JsonNode t : s.path("transitions")) {
                transitions.add(
                        new Transition(
                                TransitionKind.valueOf(t.path("kind").asText("ADVANCE")),
                                optText(t, "to"),
                                optText(t, "condition")));
            }
            steps.add(
                    new WorkflowStep(
                            s.path("key").asText(),
                            s.path("label").asText(),
                            StepType.valueOf(s.path("type").asText("TASK")),
                            optText(s, "requiredPermission"),
                            s.path("slaDays").asInt(0),
                            transitions));
        }
        return new WorkflowDefinition(root.path("firstStepKey").asText(""), steps);
    }

    // ── form schema ──

    static String formSchemaToJson(FormSchema schema) {
        ObjectNode root = M.createObjectNode();
        ArrayNode fields = root.putArray("fields");
        for (FormField f : schema.fields()) {
            ObjectNode fo = fields.addObject();
            fo.put("key", f.key());
            fo.put("label", f.label());
            fo.put("type", f.type().name());
            fo.put("required", f.required());
            f.helpText().ifPresent(h -> fo.put("help", h));
            if (!f.options().isEmpty()) {
                ArrayNode opts = fo.putArray("options");
                for (FormField.Option o : f.options()) {
                    ObjectNode oo = opts.addObject();
                    oo.put("value", o.value());
                    oo.put("label", o.label());
                }
            }
        }
        return write(root);
    }

    static FormSchema formSchemaFromJson(String json) {
        JsonNode root = read(json);
        if (root == null || !root.isObject() || !root.has("fields")) {
            return FormSchema.empty();
        }
        List<FormField> fields = new ArrayList<>();
        for (JsonNode f : root.get("fields")) {
            List<FormField.Option> options = new ArrayList<>();
            for (JsonNode o : f.path("options")) {
                options.add(new FormField.Option(o.path("value").asText(), o.path("label").asText()));
            }
            fields.add(
                    new FormField(
                            f.path("key").asText(),
                            f.path("label").asText(),
                            FieldType.valueOf(f.path("type").asText("TEXT")),
                            f.path("required").asBoolean(false),
                            optText(f, "help"),
                            options));
        }
        return new FormSchema(fields);
    }

    // ── SLA / validity ──

    static String slaToJson(Sla sla) {
        ObjectNode o = M.createObjectNode();
        o.put("targetDays", sla.targetDays());
        o.put("basis", sla.basis().name());
        sla.escalationThreshold().ifPresent(v -> o.put("escalationThresholdDays", v));
        return write(o);
    }

    static Sla slaFromJson(String json) {
        JsonNode n = read(json);
        if (n == null || !n.isObject()) {
            return Sla.none();
        }
        OptionalInt escalation =
                n.has("escalationThresholdDays")
                        ? OptionalInt.of(n.get("escalationThresholdDays").asInt())
                        : OptionalInt.empty();
        return new Sla(
                n.path("targetDays").asInt(0),
                Sla.Basis.valueOf(n.path("basis").asText(Sla.Basis.BUSINESS_DAYS.name())),
                escalation);
    }

    static String validityToJson(Validity validity) {
        ObjectNode o = M.createObjectNode();
        validity.validForDays().ifPresent(v -> o.put("validForDays", v));
        o.put("renewable", validity.renewable());
        return write(o);
    }

    static Validity validityFromJson(String json) {
        JsonNode n = read(json);
        if (n == null || !n.isObject()) {
            return Validity.permanent();
        }
        OptionalInt days =
                n.has("validForDays") ? OptionalInt.of(n.get("validForDays").asInt()) : OptionalInt.empty();
        return new Validity(days, n.path("renewable").asBoolean(false));
    }

    // ── helpers ──

    private static Optional<String> optText(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return (v == null || v.isNull()) ? Optional.empty() : Optional.of(v.asText());
    }

    private static String write(JsonNode node) {
        try {
            return M.writeValueAsString(node);
        } catch (Exception e) {
            throw new SirmaxException("Could not serialize service JSON", e);
        }
    }

    private static JsonNode read(String json) {
        try {
            return (json == null || json.isBlank()) ? null : M.readTree(json);
        } catch (Exception e) {
            throw new SirmaxException("Could not parse service JSON: " + json, e);
        }
    }
}
