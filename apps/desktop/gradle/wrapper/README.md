# Gradle wrapper

`gradle-wrapper.jar` is **not committed yet** (Phase 1 pending item in `ROADMAP.md`). Generate it
once with a locally installed Gradle 8.12:

```bash
cd apps/desktop
gradle wrapper --gradle-version 8.12
git add gradle/wrapper/gradle-wrapper.jar
```

After that, `./gradlew` (or `gradlew.bat` on Windows) is self-contained and no local Gradle is
needed. CI uses `gradle/actions/setup-gradle` and calls `gradle` directly, so it does not depend on
the wrapper jar being present.
