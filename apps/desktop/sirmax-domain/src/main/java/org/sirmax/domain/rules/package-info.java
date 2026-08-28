// SPDX-License-Identifier: AGPL-3.0-or-later

/**
 * The restricted rule/expression evaluator shared by the requirements engine and the workflow engine
 * (docs/adr/0007). {@link org.sirmax.domain.rules.ExpressionEvaluator} evaluates a boolean
 * expression over a typed context — no function calls, no assignment, no I/O.
 */
package org.sirmax.domain.rules;
