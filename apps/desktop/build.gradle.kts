// SPDX-License-Identifier: AGPL-3.0-or-later
//
// Root build for the SIRMAX Windows desktop application.
//
// Common, catalog-independent conventions live here. Dependency wiring
// (which needs the type-safe `libs` accessor) lives in each module's own
// build.gradle.kts, because the `libs` accessor is not available inside a
// `subprojects {}` block. See docs/adr/0004-gradle.md.
//
// Gradle runs on JDK 21 (see gradle/wrapper + CI); the Java toolchain
// compiles and tests every module with JDK 25 (docs/adr/0001-java-25.md).

subprojects {
    // java-library (not plain java) so modules can expose transitive API
    // with `api(...)` — e.g. sirmax-domain re-exports sirmax-shared types.
    apply(plugin = "java-library")

    group = "org.sirmax"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
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
        // Opt-in switches reach the test JVM: -D on the Gradle command line otherwise sets a
        // property on Gradle's own JVM, where no test can see it.
        listOf("sirmax.screenshots").forEach { key ->
            System.getProperty(key)?.let { systemProperty(key, it) }
        }
    }
}
