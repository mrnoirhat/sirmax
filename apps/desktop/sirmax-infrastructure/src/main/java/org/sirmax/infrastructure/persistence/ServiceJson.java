// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.sirmax.domain.service.RequirementDef;
import org.sirmax.domain.service.RequirementKind;
import org.sirmax.domain.service.RequirementStage;
import org.sirmax.domain.service.Sla;
import org.sirmax.domain.service.Validity;
import org.sirmax.shared.SirmaxException;

/**
 * Serializes the typed parts of a service version to/from the {@code *_json} columns.
 *
 * <p>Explicit node building (no annotations) keeps the domain model framework-free. The still-opaque
 * parts (form schema, workflow, fee rules, output documents, authorization) are stored verbatim as
 * {@code JsonDoc} strings and do not go through here yet.
 */
final class ServiceJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ServiceJson() {}

    // ── requirements ──

    static String requirementsToJson(List<RequirementDef> requirements) {
        ArrayNode array = MAPPER.createArrayNode();
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
        JsonNode root = read(json);
        List<RequirementDef> out = new ArrayList<>();
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

    // ── SLA ──

    static String slaToJson(Sla sla) {
        ObjectNode o = MAPPER.createObjectNode();
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
        int target = n.path("targetDays").asInt(0);
        Sla.Basis basis = Sla.Basis.valueOf(n.path("basis").asText(Sla.Basis.BUSINESS_DAYS.name()));
        OptionalInt escalation =
                n.has("escalationThresholdDays")
                        ? OptionalInt.of(n.get("escalationThresholdDays").asInt())
                        : OptionalInt.empty();
        return new Sla(target, basis, escalation);
    }

    // ── validity ──

    static String validityToJson(Validity validity) {
        ObjectNode o = MAPPER.createObjectNode();
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
            return MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            throw new SirmaxException("Could not serialize service JSON", e);
        }
    }

    private static JsonNode read(String json) {
        try {
            return (json == null || json.isBlank()) ? null : MAPPER.readTree(json);
        } catch (Exception e) {
            throw new SirmaxException("Could not parse service JSON: " + json, e);
        }
    }
}
