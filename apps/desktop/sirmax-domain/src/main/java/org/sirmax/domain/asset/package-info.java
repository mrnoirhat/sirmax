// SPDX-License-Identifier: AGPL-3.0-or-later
/**
 * Municipal assets and the agreements over them — master prompt §25, §26, and the cemetery (§6),
 * market (§7) and public-space (§9) modules.
 *
 * <p>Deliberately one model rather than five. {@link org.sirmax.domain.asset.MunicipalAsset} carries
 * a parcel, a cemetery niche, a market stall and a kiosk alike, distinguished by
 * {@link org.sirmax.domain.asset.AssetKind} and by module-specific JSON attributes;
 * {@link org.sirmax.domain.asset.Agreement} is the one lease/concession/assignment/permit contract,
 * with a single transfer and termination story; {@link org.sirmax.domain.asset.AssetHolder} records
 * who held what, when, as a history rather than a mutable column.
 */
package org.sirmax.domain.asset;
