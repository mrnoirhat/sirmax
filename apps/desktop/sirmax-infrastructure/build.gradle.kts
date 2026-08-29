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
    implementation(libs.jackson.databind)
    implementation(libs.pdfbox)
    implementation(libs.zxing.core)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// The authored SQL migrations live at the repo root (database/migrations/, see
// master prompt §3). Copy them onto this module's classpath as db/migration/*
// plus a sorted index.txt the runner reads (reliable inside a jar).
val migrationsSource = file("$rootDir/../../database/migrations")
val migrationsStaging = layout.buildDirectory.dir("generated-resources/migrations")

val stageMigrations by tasks.registering(Copy::class) {
    from(migrationsSource) { include("V*__*.sql") }
    into(migrationsStaging.map { it.dir("db/migration") })
    doLast {
        val dir = migrationsStaging.get().dir("db/migration").asFile
        val names = dir.listFiles { f -> f.name.endsWith(".sql") }?.map { it.name }?.sorted().orEmpty()
        dir.resolve("index.txt").writeText(names.joinToString("\n", postfix = "\n"))
    }
}

sourceSets.named("main") { resources.srcDir(migrationsStaging) }
tasks.named("processResources") { dependsOn(stageMigrations) }
