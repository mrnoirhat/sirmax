// SPDX-License-Identifier: AGPL-3.0-or-later
//
// sirmax-app — the composition root. Wires the dependency graph by hand
// (no DI framework, see docs/adr/0005), owns `main`, and owns the jlink /
// jpackage configuration for the Windows build (master prompt §44).

plugins {
    application
    alias(libs.plugins.javafx)
}

description = "SIRMAX desktop composition root and launcher"

javafx {
    version = libs.versions.javafx.get()
    // javafx.swing is only used by the screenshot pass in the integration tests, which converts a
    // scene snapshot to a BufferedImage. The plugin has no test-only scope, so it ships with the
    // app; it costs a module on the jlink runtime and nothing at run time.
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.swing")
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

// ── Windows packaging (master prompt §44) ───────────────────────────────────
//
// The goal is that a municipal PC needs no Java, no Node and no Python: the
// artifact carries its own runtime.
//
// Two rules from §44 shape what follows:
//   · the database must NOT live under the install directory, because an upgrade
//     replaces that directory wholesale. AppPaths already puts it under
//     %LOCALAPPDATA%, and --win-upgrade-uuid makes an upgrade an upgrade rather
//     than a second installation beside the first;
//   · uninstalling must leave the data alone. jpackage removes only what it
//     installed, which is right — a municipality reinstalling must not lose its
//     register.

val appVersion: String = (version as String).removeSuffix("-SNAPSHOT")

// Stable for the life of the product line. Changing it would turn every future
// installer into a parallel installation, so it is a constant rather than
// something derived from the version.
val windowsUpgradeUuid = "6f4a9c2e-1d3b-4a77-9c51-0b2e8f3a5d64"

val runtimeImageDir = layout.buildDirectory.dir("runtime-image")
val stagedAppDir = layout.buildDirectory.dir("jpackage-input")
val appImageDir = layout.buildDirectory.dir("app-image")
val installerDir = layout.buildDirectory.dir("installer")

/** The JDK modules the application actually needs, plus what its libraries reach for. */
val requiredJdkModules = listOf(
    "java.base",
    "java.desktop",      // AWT/Swing types JavaFX and printing sit on
    "java.logging",
    "java.management",   // logback
    "java.naming",       // JDBC
    "java.net.http",     // Google Drive backups
    "java.prefs",
    "java.sql",
    "java.xml",
    "jdk.crypto.ec",     // TLS to Google
    "jdk.unsupported"    // sqlite-jdbc and PDFBox both reach for sun.misc.Unsafe
)

/** A trimmed runtime, so the artifact stays a download a municipality can manage. */
val jlinkRuntime by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Builds a minimal Java runtime image for packaging"

    val javaHome = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    }.get().metadata.installationPath.asFile

    doFirst { delete(runtimeImageDir) }

    commandLine(
        File(javaHome, "bin/jlink").absolutePath,
        "--add-modules", requiredJdkModules.joinToString(","),
        "--strip-debug",
        "--no-header-files",
        "--no-man-pages",
        "--compress", "zip-6",
        "--output", runtimeImageDir.get().asFile.absolutePath
    )
    outputs.dir(runtimeImageDir)
}

/** Everything that goes inside the package: the app jar and its dependencies. */
val stageForPackaging by tasks.registering(Sync::class) {
    group = "distribution"
    description = "Collects the application jar and its runtime classpath"

    from(tasks.named("jar"))
    from(configurations.runtimeClasspath)
    into(stagedAppDir)
}

/** Shared jpackage arguments, so the app image and the MSI cannot describe different products. */
fun jpackageCommon(): List<String> = listOf(
    "--name", "SIRMAX",
    "--app-version", appVersion,
    "--vendor", "SIRMAX",
    "--description", "Sistema Integral de Registros Municipales y Administracion eXtensible",
    "--copyright", "AGPL-3.0-or-later",
    "--input", stagedAppDir.get().asFile.absolutePath,
    "--main-jar", tasks.named<Jar>("jar").get().archiveFileName.get(),
    "--main-class", "org.sirmax.app.Launcher",
    "--runtime-image", runtimeImageDir.get().asFile.absolutePath,
    // The same .ico for the executable, the Start menu entry, the desktop shortcut and the
    // installer, so the product is recognisable everywhere Windows shows it.
    "--icon", file("src/main/resources/org/sirmax/app/sirmax.ico").absolutePath,
    // sqlite-jdbc and PDFBox load native code and reach for sun.misc.Unsafe. Granting access
    // explicitly keeps the warning off a municipal operator's first launch — and means the
    // application still starts when a future JDK turns that warning into a refusal.
    "--java-options", "--enable-native-access=ALL-UNNAMED",
    // Windows still defaults some consoles to a legacy code page; Spanish accents in file
    // names and log output depend on this.
    "--java-options", "-Dfile.encoding=UTF-8"
)

fun jpackageBinary(): String = File(
    javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    }.get().metadata.installationPath.asFile,
    "bin/jpackage"
).absolutePath

/**
 * A self-contained folder that runs SIRMAX.exe with no installation at all.
 *
 * This is the artifact that always exists: unlike the MSI it needs no external
 * toolchain, so it can be built and verified on any Windows machine. It is also
 * genuinely useful — a municipality can run SIRMAX from a shared folder or a USB
 * stick while evaluating it.
 */
val packageAppImage by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Builds a self-contained SIRMAX folder (no installer required)"
    dependsOn(stageForPackaging, jlinkRuntime)

    // Packaging is Windows-only by nature; elsewhere this is a no-op rather than
    // a build failure, so CI on Linux stays useful.
    onlyIf { org.gradle.internal.os.OperatingSystem.current().isWindows }

    doFirst { delete(appImageDir) }

    commandLine(
        listOf(jpackageBinary(), "--type", "app-image")
            + jpackageCommon()
            + listOf("--dest", appImageDir.get().asFile.absolutePath)
    )
    outputs.dir(appImageDir)
}

/**
 * The Windows MSI (§44).
 *
 * jpackage builds MSIs through the WiX toolset, which is not part of the JDK.
 * Rather than failing the whole build on a machine that has not got it, this
 * checks first and says exactly what is missing: a developer building the app
 * should not be stopped by a tool only the release job needs.
 */
val packageWindows by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Builds the Windows installer (MSI) — requires WiX 3.x on PATH"
    dependsOn(stageForPackaging, jlinkRuntime)

    onlyIf {
        val windows = org.gradle.internal.os.OperatingSystem.current().isWindows
        val wix = org.gradle.internal.os.OperatingSystem.current().findInPath("candle") != null
        if (windows && !wix) {
            logger.lifecycle(
                "Skipping the MSI: WiX 3.x is not on PATH. Install it from " +
                    "https://wixtoolset.org to build installers; packageAppImage produces a " +
                    "runnable SIRMAX folder without it."
            )
        }
        windows && wix
    }

    doFirst { mkdir(installerDir) }

    commandLine(
        listOf(jpackageBinary(), "--type", "msi")
            + jpackageCommon()
            + listOf(
                "--dest", installerDir.get().asFile.absolutePath,
                // §44: Start Menu entry, optional desktop shortcut, real upgrade path
                "--win-menu",
                "--win-menu-group", "SIRMAX",
                "--win-shortcut",
                "--win-shortcut-prompt",
                "--win-dir-chooser",
                "--win-upgrade-uuid", windowsUpgradeUuid,
                "--win-per-user-install"
            )
    )
    outputs.dir(installerDir)
}

/**
 * Authenticode-signs the launcher and the installer, when a signing certificate is available.
 *
 * Deliberately skipped rather than failed when there is no certificate: CI has none, and a release
 * pipeline that goes red because a developer machine holds the only key would stop the build for
 * everyone. The signature is a property of the artifact, not of whether the code compiles.
 *
 * Runs between packaging and the ZIP, so the portable archive contains the *signed* launcher —
 * zipping first would ship an unsigned executable inside a signed-looking download.
 */
val signWindowsArtifacts by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Signs SIRMAX.exe and the MSI with the developer's code-signing certificate"
    dependsOn(packageAppImage, packageWindows)

    onlyIf { org.gradle.internal.os.OperatingSystem.current().isWindows }
    isIgnoreExitValue = true

    val script = rootProject.file("../../tools/sign-windows.ps1")
    commandLine(
        "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.absolutePath,
        "-AppImage", File(appImageDir.get().asFile, "SIRMAX").absolutePath,
        "-Msi", File(installerDir.get().asFile, "SIRMAX-$appVersion.msi").absolutePath
    )

    doLast {
        val code = executionResult.get().exitValue
        if (code == 2) {
            logger.lifecycle("Sin certificado de firma; los artefactos quedan sin firmar.")
        } else if (code != 0) {
            logger.warn("La firma terminó con código $code; revisa la salida de arriba.")
        }
    }
}

/**
 * Zips the app image, so the portable download is one file rather than a folder
 * a browser cannot deliver.
 */
val packageZip by tasks.registering(Zip::class) {
    group = "distribution"
    description = "Packs the self-contained SIRMAX folder into a single downloadable archive"
    dependsOn(packageAppImage)
    // Signing rewrites SIRMAX.exe in place, so it has to happen before the folder is zipped.
    mustRunAfter(signWindowsArtifacts)

    onlyIf { org.gradle.internal.os.OperatingSystem.current().isWindows }

    from(appImageDir)
    archiveFileName.set("SIRMAX-$appVersion-windows.zip")
    destinationDirectory.set(installerDir)
}

/**
 * Checks the produced artifacts are real ones (§67).
 *
 * The size floor is not arbitrary: the bundled runtime alone is tens of
 * megabytes, so anything smaller means jlink's output did not make it in — which
 * produces an artifact that installs cleanly and then fails to start.
 */
val verifyReleaseArtifacts by tasks.registering {
    group = "verification"
    description = "Checks the packaged artifacts look like real, runnable artifacts"
    dependsOn(packageAppImage, packageZip, packageWindows, signWindowsArtifacts)

    onlyIf { org.gradle.internal.os.OperatingSystem.current().isWindows }

    doLast {
        val image = File(appImageDir.get().asFile, "SIRMAX")
        check(image.isDirectory) { "jpackage produced no app image at $image" }
        check(File(image, "SIRMAX.exe").isFile) { "The app image has no SIRMAX.exe launcher" }
        check(File(image, "runtime").isDirectory) {
            "The app image bundles no runtime — it would need a JDK on the target PC"
        }

        val size = image.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        check(size > 40L * 1024 * 1024) {
            "The app image is only ${size / 1024 / 1024} MB — the runtime image is missing"
        }
        logger.lifecycle("App image: $image (${size / 1024 / 1024} MB)")

        val zip = File(installerDir.get().asFile, "SIRMAX-$appVersion-windows.zip")
        check(zip.isFile) { "The portable archive was not produced at $zip" }
        check(zip.length() > 20L * 1024 * 1024) {
            "${zip.name} is only ${zip.length() / 1024} KB — the runtime image is missing"
        }
        logger.lifecycle("Portable: ${zip.name} (${zip.length() / 1024 / 1024} MB)")

        installerDir.get().asFile.listFiles { f -> f.name.endsWith(".msi") }?.forEach { msi ->
            check(msi.length() > 20L * 1024 * 1024) {
                "${msi.name} is only ${msi.length() / 1024} KB — the runtime image is missing"
            }
            logger.lifecycle("Installer: ${msi.name} (${msi.length() / 1024 / 1024} MB)")
        }
    }
}
