// SPDX-License-Identifier: AGPL-3.0-or-later
//
// Root build for the SIRMAX Windows desktop application.
// Common conventions live here; module-specific config lives in each
// sirmax-*/build.gradle.kts. See docs/adr/0004-gradle.md and
// docs/adr/0005-modular-domain-architecture.md.
//
// Phase 1 keeps this intentionally minimal. Static analysis / formatting
// (spotless + google-java-format) and -Werror are added in Phase 2 with a
// one-time reformat commit (see ROADMAP.md).

subprojects {
    // java-library (not plain java) so modules can expose transitive API
    // with `api(...)` — e.g. sirmax-domain re-exports sirmax-shared types.
    apply(plugin = "java-library")

    group = "org.sirmax"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    dependencies {
        "testImplementation"(platform(libs.junit.bom))
        "testImplementation"(libs.bundles.test)
        "testRuntimeOnly"(libs.junit.platform.launcher)
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(25)
        options.compilerArgs.add("-Xlint:all,-serial,-this-escape,-processing")
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
