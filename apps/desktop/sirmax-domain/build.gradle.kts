// SPDX-License-Identifier: AGPL-3.0-or-later
//
// sirmax-domain — entities, aggregates, invariants and domain services.
// PURE JAVA: no JavaFX, no JDBC, no I/O, no user-facing string literals
// (see docs/adr/0005-modular-domain-architecture.md). Only depends on
// sirmax-shared.

description = "SIRMAX domain model (pure Java)"

dependencies {
    api(project(":sirmax-shared"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
