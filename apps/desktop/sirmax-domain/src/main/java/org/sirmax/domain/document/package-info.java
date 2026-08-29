// SPDX-License-Identifier: AGPL-3.0-or-later
/**
 * Official documents, their frozen content and how they get printed — master prompt §46, §47,
 * §59B–§59F.
 *
 * <p>The load-bearing idea is {@link org.sirmax.domain.document.DocumentSnapshot}: an issued
 * document carries everything needed to reproduce itself, institution branding included, so a
 * rebrand or a corrected citizen name never rewrites a document already in someone's hands.
 * {@link org.sirmax.domain.document.IssuedDocument} separates issuing from printing — a reprint
 * never renumbers and is always audited — and
 * {@link org.sirmax.domain.document.VerificationCode} is the public, information-free code a
 * municipality can check a document against.
 */
package org.sirmax.domain.document;
