// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sirmax.domain.registry.Inspection;
import org.sirmax.shared.SirmaxException;

/**
 * Serializes an inspection's checklist to and from {@code inspection.checklist_json}.
 *
 * <p>Explicit node building, like {@link ServiceJson}: the domain model stays free of Jackson
 * annotations, and Jackson stays inside infrastructure (ADR 0005, enforced by ArchUnit).
 */
final class RegistryJson {

    private static final ObjectMapper M = new ObjectMapper();

    String checklistToJson(List<Inspection.ChecklistAnswer> answers) {
        ArrayNode array = M.createArrayNode();
        for (Inspection.ChecklistAnswer answer : answers) {
            ObjectNode node = array.addObject();
            node.put("key", answer.key());
            node.put("label", answer.label());
            // Tri-state: null means "not assessed", which is not the same as failed.
            if (answer.compliant() == null) {
                node.putNull("compliant");
            } else {
                node.put("compliant", answer.compliant());
            }
            answer.note().ifPresent(note -> node.put("note", note));
        }
        return array.toString();
    }

    List<Inspection.ChecklistAnswer> checklistFromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = M.readTree(json);
            List<Inspection.ChecklistAnswer> answers = new ArrayList<>();
            for (JsonNode node : root) {
                JsonNode compliant = node.get("compliant");
                answers.add(
                        new Inspection.ChecklistAnswer(
                                node.path("key").asText(),
                                node.path("label").asText(),
                                compliant == null || compliant.isNull()
                                        ? null
                                        : compliant.asBoolean(),
                                optText(node, "note")));
            }
            return List.copyOf(answers);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new SirmaxException("Could not read the inspection checklist", e);
        }
    }

    private static Optional<String> optText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asText().isBlank()
                ? Optional.empty()
                : Optional.of(value.asText());
    }
}
