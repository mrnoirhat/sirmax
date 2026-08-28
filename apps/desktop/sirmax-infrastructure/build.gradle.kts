// SPDX-License-Identifier: AGPL-3.0-or-later
//
// sirmax-infrastructure — adapters that implement the application ports:
// SQLite/JDBC, migrations, filesystem, PDF, Windows printing, Google Drive,
// encryption, secret storage. This is the only module allowed to touch
// java.sql and external services.

description = "SIRMAX infrastructure adapters (SQLite, files, PDF, printing, Drive)"

dependencies {
    api(project(":sirmax-application"))
    implementation(project(":sirmax-domain"))
    implementation(project(":sirmax-shared"))

    implementation(libs.sqlite.jdbc)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
