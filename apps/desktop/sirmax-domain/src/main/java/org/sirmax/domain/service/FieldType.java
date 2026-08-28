// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.domain.service;

/** The input kinds a configurable {@link FormField} can be (master prompt §16 — configurable forms). */
public enum FieldType {
    TEXT,
    TEXT_AREA,
    NUMBER,
    MONEY,
    DATE,
    BOOLEAN,
    SELECT,
    PARTY_REF,
    PROPERTY_REF
}
