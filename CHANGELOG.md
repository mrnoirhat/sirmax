# Changelog

Todos los cambios notables de SIRMAX se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa
[Versionado Semántico](https://semver.org/lang/es/).

## [Unreleased]

### Added
- **Fase 4 (en curso) — Motor configurable de servicios.**
  - `V0003__service_engine.sql`: catálogo de servicios, definiciones y versiones (columnas tipadas +
    JSON validado).
  - Dominio `service`: `ServiceCategory`, `ServiceDefinition`, `ServiceDefinitionVersion` (editable
    solo en `DRAFT`, inmutable al publicar), `RequirementDef`/`RequirementKind`/`RequirementStage`,
    `Sla`, `Validity`, `ServiceVersionValidator`. `shared`: `JsonDoc`.
  - Casos de uso: `CreateServiceDraft`, `ConfigureServiceDraft`, `PublishServiceVersion` (valida +
    supersede la ACTIVE), `CreateServiceDraftVersion`, `SetServiceAvailability` — permiso
    `service.configure`, auditados.
  - `SqliteServiceCatalogRepository` (JSON de config vía **Jackson 2.18**, solo en infraestructura).
  - Tests: `ServiceDefinitionVersionTest`, `ServiceEngineTest`, `SqliteServiceCatalogRepositoryTest`.
- **Fase 3 ✅ — Dominio central y base de datos.**
  - Runner de migraciones: `SqlScript` (splitter consciente de `BEGIN`/`END`, literales y
    comentarios), `MigrationRunner` (una transacción por migración, `schema_migrations`, rechazo de
    orden y de deriva de checksum), migraciones expuestas al classpath vía Gradle (`db/migration/` +
    `index.txt`).
  - `V0002__core_schema.sql`: organización/institución, departamentos, RBAC (usuarios, roles,
    25 permisos, 4 roles de sistema), personas y organizaciones, identificaciones/direcciones/
    contactos (dueño polimórfico), `app_setting`.
  - Dominio puro: `identity`, `org`, `security` (incl. `AccessPolicy`, `PasswordHash`).
  - Aplicación: puertos de repositorio, `PasswordHasher`/`IdGenerator`, `Session`/`Audit`; casos de
    uso `ProvisionInitialAdmin`, `Authenticate`, `RegisterPerson`.
  - Infraestructura: `SqliteDatabase` + `JdbcUnitOfWork` + adaptadores SQLite de todos los repos,
    `SqliteAuditSink`, `Pbkdf2PasswordHasher` (ADR 0014), `UuidV7IdGenerator`, `AppPaths`,
    `CompositionRoot`.
  - Pruebas: 55+ nuevas (SqlScript, MigrationRunner, dominio, casos de uso con fakes, adaptadores
    SQLite, PBKDF2, `ProvisionAndAuthenticateIT` end-to-end). CI verde.
- **Fase 2 ✅ — Shell de escritorio y Design System.**
  - i18n de UI (`Messages` + `messages.properties`, español base); sin texto literal en código.
  - Tema `sirmax.css` con tokens de color/tipografía/espaciado (_looked-up colors_).
  - Design System: `Styles`, `Typography`, `Buttons`, `Cards`, `Banner`, `StatefulContent`
    (loading/empty/error/success), `ToastHost`, `Dialogs`, `FormField`, `DataTable`.
  - Navegación service-first: `RouteKey`, `NavItem`, `Navigator`/`ShellNavigator` (sin JavaFX),
    `ShellView` (menú + barra superior + navegación por tareas + área de contenido + toasts),
    `HomeView` / `DashboardView` / `GlobalSearchView` / `PlaceholderView` / `StyleGuideView`.
  - Barra de menú (`Archivo`/`Ver`/`Ayuda`); atajos `Ctrl+K`, `Alt+Home`, `Ctrl+Shift+G`, `F1`,
    `Ctrl+Q`.
  - Tema claro/oscuro: `Theme` + `ThemeManager` + toggle en el menú; `-Dsirmax.theme=dark`.
  - Área de contenido con scroll; `GlobalSearchView` con resultados por categoría (estado vacío).
  - Pruebas: `ShellNavigatorTest`, `NavItemTest`, `MessagesTest`, `ThemeManagerTest` +
    `ShellViewSmokeTest` (arranca el toolkit JavaFX; 21 pruebas en `sirmax-ui`). ADR 0013 (JavaFX
    programático sin FXML). Verificado en CI (Desktop + Security en verde).
- **Fase 0/1 — Fundación del repositorio.**
  - Estructura del monorepo: `apps/desktop`, `apps/landing`, `apps/docs`, `backend`, `database`,
    `scripts`, `docs`, `.github`.
  - Modelo de ramas permanentes `experiment → testing → main`.
  - Documentos de gobernanza: README, CONTRIBUTING, CODE_OF_CONDUCT, SECURITY, SUPPORT,
    ARCHITECTURE, DEVELOPMENT, DATABASE, BACKUP, RELEASE, ROADMAP, THIRD_PARTY_LICENSES,
    TRADEMARK_POLICY.
  - ADR 0001–0012 (Java 25, JavaFX, SQLite, Gradle, arquitectura modular, motores de servicio /
    workflow / tasas, backup en Google Drive, AGPL-3.0-or-later, monorepo, flujo de tres ramas).
  - Documentación de dominio: glosario, mapa de dominio, ERD inicial, mapa de módulos, mapa de UX,
    plan de build.
  - Esqueleto del escritorio: proyecto Gradle multi-módulo (`sirmax-shared`, `sirmax-domain`,
    `sirmax-application`, `sirmax-infrastructure`, `sirmax-ui`, `sirmax-app`) con catálogo de
    versiones.
  - Esqueleto de la landing (Next.js + TypeScript) con navegación y CTAs obligatorios
    (_VER PROYECTO EN GITHUB_, _Descargar SIRMAX_).
  - Esqueleto de documentación (Docusaurus) con la estructura de secciones prevista.
  - GitHub Actions: `desktop`, `landing`, `docs`, `security`; plantillas de issues y PR.
  - `database/migrations/V0001__baseline.sql` (línea base mínima del esquema).
  - `package-lock.json` comiteado; los workflows usan `npm ci` con caché de npm.

### Changed
  - Gradle **8.12 → 9.7.1** (Gradle 8.x no arranca sobre JDK 25). El wrapper queda comiteado con
    verificación de checksum.
  - El cableado de dependencias se movió del bloque `subprojects {}` al `build.gradle.kts` de cada
    módulo (el accesor `libs` no está disponible en `subprojects {}`).
  - ArchUnit **1.3.0 → 1.5.0** (ASM de 1.3.0 no lee bytecode de Java 25).
  - Docusaurus **3.7.0 → 3.10.2** (3.7 falla con el webpack actual y con Node ≥ 24). Se pospone el
    tema de Mermaid a la Fase 12.

### Verified
  - Los cuatro workflows de CI (Desktop, Docs, Landing, Security) **en verde** en `experiment`
    (commit `2b24c71`). Build local reproducido: `./gradlew build`, `docs build`, landing
    `lint`/`typecheck`/`build`.

[Unreleased]: https://github.com/mrnoirhat/sirmax/commits/experiment
