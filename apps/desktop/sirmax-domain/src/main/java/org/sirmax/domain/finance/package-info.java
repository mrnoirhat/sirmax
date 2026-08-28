// SPDX-License-Identifier: AGPL-3.0-or-later

/**
 * The fee engine and invoice status (master prompt §19–§21; docs/adr/0008).
 *
 * <p>A {@link org.sirmax.domain.finance.FeeRule} is immutable and dated; {@link
 * org.sirmax.domain.finance.FeeCalculator} applies the rules effective on a date to a {@link
 * org.sirmax.domain.finance.FeeInput} and produces a {@link org.sirmax.domain.finance.Charge}
 * (lines + total). A fee is <em>not</em> an invoice — the billing module (Phase 6) turns a
 * {@code Charge} into an {@code Invoice}. All money is integer minor units via {@link
 * org.sirmax.shared.Money}; never floating point.
 */
package org.sirmax.domain.finance;
