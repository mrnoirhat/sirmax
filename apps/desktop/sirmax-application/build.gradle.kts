// SPDX-License-Identifier: AGPL-3.0-or-later
//
// sirmax-application — use cases and PORTS (interfaces the infrastructure
// layer implements). No JavaFX, no JDBC. Depends only on domain + shared.

description = "SIRMAX application layer (use cases + ports)"

dependencies {
    api(project(":sirmax-domain"))
    api(project(":sirmax-shared"))
}
