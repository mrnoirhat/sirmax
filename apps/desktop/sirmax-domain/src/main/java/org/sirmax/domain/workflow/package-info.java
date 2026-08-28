// SPDX-License-Identifier: AGPL-3.0-or-later

/**
 * The pragmatic, data-driven workflow engine (master prompt §18; docs/adr/0007).
 *
 * <p>A {@link org.sirmax.domain.workflow.WorkflowDefinition} is an ordered list of {@link
 * org.sirmax.domain.workflow.WorkflowStep}s with a closed set of {@link
 * org.sirmax.domain.workflow.TransitionKind}s — no arbitrary scripting; branching uses the
 * restricted {@link org.sirmax.domain.rules.ExpressionEvaluator}. {@link
 * org.sirmax.domain.workflow.WorkflowValidator} checks structure before publish; {@link
 * org.sirmax.domain.workflow.WorkflowEngine} resolves available transitions and where they lead.
 * Execution state lives on the procedure aggregate (Phase 5).
 */
package org.sirmax.domain.workflow;
