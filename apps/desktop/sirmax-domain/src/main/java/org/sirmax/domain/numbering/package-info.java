// SPDX-License-Identifier: AGPL-3.0-or-later
/**
 * Independent, configurable document numbering sequences — master prompt §27 and §59A.3.
 *
 * <p>Procedures, invoices, receipts and certificates each draw from their own
 * {@link org.sirmax.domain.numbering.NumberingSequence}, allocated inside the transaction that
 * writes the numbered row so numbers are unique and never reused.
 */
package org.sirmax.domain.numbering;
