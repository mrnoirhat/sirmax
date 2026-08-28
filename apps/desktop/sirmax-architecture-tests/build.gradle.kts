// SPDX-License-Identifier: AGPL-3.0-or-later
//
// sirmax-architecture-tests — ArchUnit checks that the layer boundaries
// from docs/adr/0005 actually hold. Has no main source set.

description = "SIRMAX architecture boundary tests (ArchUnit)"

dependencies {
    testImplementation(project(":sirmax-shared"))
    testImplementation(project(":sirmax-domain"))
    testImplementation(project(":sirmax-application"))
    testImplementation(project(":sirmax-infrastructure"))
    testImplementation(project(":sirmax-ui"))
    testImplementation(project(":sirmax-app"))
    testImplementation(libs.archunit.junit5)
}
