// SPDX-License-Identifier: AGPL-3.0-or-later
//
// sirmax-ui — JavaFX presentation layer: application shell, design system,
// views and view-models. JavaFX lives here and ONLY here. Must not touch
// java.sql. Depends on application + shared.

plugins {
    alias(libs.plugins.javafx)
}

description = "SIRMAX JavaFX UI (shell + design system)"

javafx {
    version = libs.versions.javafx.get()
    // javafx.swing is test-only: the screenshot generator converts a scene snapshot
    // to a PNG through SwingFXUtils. It is not used by the application itself.
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.swing")
}

dependencies {
    implementation(project(":sirmax-application"))
    implementation(project(":sirmax-shared"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
