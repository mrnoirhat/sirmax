// SPDX-License-Identifier: AGPL-3.0-or-later
rootProject.name = "sirmax-desktop"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    // The version catalog `libs` is picked up automatically from
    // gradle/libs.versions.toml (conventional location).
}

// ── Layered modules (see docs/adr/0005-modular-domain-architecture.md) ──
include("sirmax-shared")          // Money, Result, IDs, base errors, i18n keys
include("sirmax-domain")          // entities, aggregates, invariants (pure Java)
include("sirmax-application")     // use cases + ports (interfaces)
include("sirmax-infrastructure")  // adapters: SQLite/JDBC, migrations, files, PDF, printing, Drive
include("sirmax-ui")              // JavaFX: shell, design system, views
include("sirmax-app")             // composition root: main, manual DI, jpackage config
include("sirmax-architecture-tests") // ArchUnit boundary checks across the modules above
