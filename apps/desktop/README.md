# SIRMAX Desktop

The SIRMAX Windows desktop application — Java 25 + JavaFX, built with Gradle.

> Architecture: [`../../ARCHITECTURE.md`](../../ARCHITECTURE.md) ·
> ADRs: [`../../docs/adr/`](../../docs/adr/) ·
> Module map: [`../../docs/domain/module-map.md`](../../docs/domain/module-map.md) ·
> Dev setup: [`../../DEVELOPMENT.md`](../../DEVELOPMENT.md)

## Modules

| Module | Layer | Depends on | Notes |
| --- | --- | --- | --- |
| `sirmax-shared` | cross-cutting | — | `Money`, `Result`, `Ids`, `MessageKey`, base errors |
| `sirmax-domain` | domain | `shared` | Pure Java. No JavaFX, no JDBC, no I/O, no user strings |
| `sirmax-application` | application | `domain`, `shared` | Use cases + ports (`Clock`, `AuditSink`, `UnitOfWork`, …) |
| `sirmax-infrastructure` | infrastructure | `application`, `domain`, `shared` | Adapters: SQLite/JDBC, files, PDF, printing, Drive |
| `sirmax-ui` | presentation | `application`, `shared` | JavaFX shell + design system. No JDBC |
| `sirmax-app` | composition root | all | `main`, manual DI, jpackage config (Phase 11) |
| `sirmax-architecture-tests` | test-only | all | ArchUnit checks that the boundaries above hold |

Dependency direction: `ui → application → domain → shared`; `infrastructure → {application, domain,
shared}`; `app → all`. Verified by `sirmax-architecture-tests`.

## Build & run

```bash
# from apps/desktop/
./gradlew build                 # compile every module + run tests
./gradlew test                  # tests only
./gradlew :sirmax-app:run       # launch the shell (JavaFX)
./gradlew check                 # tests + static analysis (spotless)
./gradlew spotlessApply         # auto-format
```

On Windows use `gradlew.bat`. Requires **JDK 25** (`java -version` must report 25).

> First-time setup: the Gradle wrapper jar is not committed yet — see
> [`gradle/wrapper/README.md`](gradle/wrapper/README.md).

## Status

Phase 1 skeleton: modules compile, `Money` has tests, the shell renders the task-first home screen.
Domain entities, persistence, use cases and features arrive from Phase 3 on
([`../../ROADMAP.md`](../../ROADMAP.md)).
