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
        options.compilerArgs.addAll(listOf("-Xlint:all,-processing", "-Werror"))
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
            googleJavaFormat("1.25.2").aosp()
            licenseHeader("// SPDX-License-Identifier: AGPL-3.0-or-later")
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}
