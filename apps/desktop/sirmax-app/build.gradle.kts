// SPDX-License-Identifier: AGPL-3.0-or-later
//
// sirmax-app — the composition root. Wires the dependency graph by hand
// (no DI framework, see docs/adr/0005), owns `main`, and will own the
// jpackage / jlink configuration for the Windows installer (Phase 11).

plugins {
    application
    alias(libs.plugins.javafx)
}

description = "SIRMAX desktop composition root and launcher"

javafx {
    version = libs.versions.javafx.get()
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation(project(":sirmax-ui"))
    implementation(project(":sirmax-application"))
    implementation(project(":sirmax-infrastructure"))
    implementation(project(":sirmax-domain"))
    implementation(project(":sirmax-shared"))
    runtimeOnly(libs.logback.classic)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass.set("org.sirmax.app.Launcher")
}

// Windows installer wiring (jpackage/jlink) is added in Phase 11; see ROADMAP.md.
