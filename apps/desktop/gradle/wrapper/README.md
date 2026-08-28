# Gradle wrapper

`gradle-wrapper.jar` and `gradle-wrapper.properties` pin **Gradle 9.7.1**, generated with
`gradle wrapper` and integrity-checked (`distributionSha256Sum` in `gradle-wrapper.properties`).

`./gradlew` (or `gradlew.bat` on Windows) is self-contained — no local Gradle install is needed.

**Why Gradle 9.x:** the project compiles and tests with **JDK 25** (see
`docs/adr/0001-java-25.md`). Gradle 8.x cannot run on JDK 25 — its embedded Kotlin fails to parse
the `25.x` Java version string. Gradle 9.1+ supports JDK 25; the wrapper is kept current on 9.x.

To change the Gradle version: edit `gradle-wrapper.properties` (`distributionUrl` +
`distributionSha256Sum` from <https://gradle.org/release-checksums/>) or run
`./gradlew wrapper --gradle-version <x> --gradle-distribution-sha256-sum <sum>`.
