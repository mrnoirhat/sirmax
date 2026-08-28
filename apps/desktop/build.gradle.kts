// SPDX-License-Identifier: AGPL-3.0-or-later
//
// Root build for the SIRMAX Windows desktop application.
// Common conventions live here; module-specific config lives in each
// sirmax-*/build.gradle.kts. See docs/adr/0004-gradle.md and
// docs/adr/0005-modular-domain-architecture.md.

plugins {
    alias(libs.plugins.spotless) apply false
}

allprojects {
    group = "org.sirmax"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")

    repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
        }
    }

    dependencies {
        "testImplementation"(platform(libs.junit.bom))
        "testImplementation"(libs.bundles.test)
        "testRuntimeOnly"(libs.junit.platform.launcher)
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(libs.versions.java.get().toInt())
        // -Werror on -Xlint:all, minus lints that are noisy for JavaFX subclasses
        // (this-escape) and framework exceptions (serial), and annotation processing.
        options.compilerArgs.addAll(
            listOf("-Xlint:all,-serial,-this-escape,-processing", "-Werror"))
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            target("src/**/*.java")
            // Phase 1: lightweight, non-reformatting rules so `check` is stable.
            // google-java-format(.aosp()) and SPDX license-header enforcement are
            // enabled in Phase 2 with a one-time `spotlessApply` commit (see ROADMAP.md).
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}
