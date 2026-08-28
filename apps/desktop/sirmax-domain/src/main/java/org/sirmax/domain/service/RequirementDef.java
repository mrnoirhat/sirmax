// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.service;

import java.util.Objects;
import java.util.Optional;

/**
 * A declared requirement of a service (master prompt §17).
 *
 * <p>Requirements are configured per service by an administrator, so {@code label} is
 * administrator-authored data, not program text. A procedure materializes these into a visible
 * checklist ("Faltan 2 requisitos…").
 *
 * @param key stable key within the service version, e.g. {@code "cedula"}
 * @param label operator-facing label, e.g. {@code "Cédula de identidad"}
 * @param kind what kind of thing satisfies it
 * @param stage the workflow stage by which it must be satisfied
 * @param required {@code true} = mandatory; {@code false} = optional
 * @param conditionExpression optional restricted expression; the requirement only applies when it
 *     evaluates true (empty = always applies)
 * @param validationRule optional rule descriptor evaluated by the requirements engine
 */
public record RequirementDef(
        String key,
        String label,
        RequirementKind kind,
        RequirementStage stage,
        boolean required,
        Optional<String> conditionExpression,
        Optional<String> validationRule) {

    public RequirementDef {
        key = requireKey(key);
        label = requireText(label, "label");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(stage, "stage");
        conditionExpression = normalize(conditionExpression);
        validationRule = normalize(validationRule);
    }

    public static RequirementDef mandatoryDocument(String key, String label, RequirementStage stage) {
        return new RequirementDef(
                key, label, RequirementKind.DOCUMENT, stage, true, Optional.empty(), Optional.empty());
    }

    public boolean isConditional() {
        return conditionExpression.isPresent();
    }

    private static String requireKey(String key) {
        String k = requireText(key, "key").toLowerCase(java.util.Locale.ROOT);
        if (!k.matches("[a-z0-9_]{1,40}")) {
            throw new IllegalArgumentException("key must be 1–40 chars of a–z, 0–9 or '_'");
        }
        return k;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String v = value.strip();
        if (v.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return v;
    }

    private static Optional<String> normalize(Optional<String> v) {
        if (v == null || v.isEmpty()) {
            return Optional.empty();
        }
        String s = v.get().strip();
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }
}
