// SPDX-License-Identifier: AGPL-3.0-or-later
//
// sirmax-shared — cross-cutting primitives with no dependencies:
// Money, Result, identifiers, base errors, i18n keys.
// Must not depend on any other sirmax module.

description = "SIRMAX shared primitives (Money, Result, IDs, i18n keys)"

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
