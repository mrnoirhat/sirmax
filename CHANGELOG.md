# Changelog

Todos los cambios notables de SIRMAX se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa
[Versionado Semántico](https://semver.org/lang/es/).

## [Unreleased]

### Added
- **Fase 9 ✅ — Backup, recuperación y Google Drive.**
  - `SqliteBackupEngine`: `VACUUM INTO` → gzip → AES-256-GCM → SHA-256. Un archivo alterado falla
    al descifrar en vez de restaurar basura; la frase de paso nunca se guarda.
  - `RestoreBackup` sigue la secuencia §42 al pie de la letra y reinscribe su procedencia en la base
    recuperada, para que la copia de emergencia siga siendo localizable.
  - `GoogleDriveBackupTarget` (REST + `HttpClient`) y `SecretStore` cifrado en reposo. La subida
    fuera de sede está **apagada** por defecto (§41).
  - `V0008__backup.sql`, `SqliteBackupRepository`, `CreateBackup`, `ManageBackupPolicy`.
  - `SqliteDatabase.reopen()`: una restauración cambia el fichero bajo la aplicación.
- **Fase 8 ✅ — Documentos, PDF e impresión.**
  - `DocumentSnapshot`: una factura emitida guarda la marca institucional, el ciudadano, las líneas
    y los totales congelados. Un rebranding en 2029 no reescribe un documento de 2026 (§59F).
  - `PdfDocumentRenderer` (PDFBox): plantilla Letter y plantilla de recibo angosto real, no capturas.
    `VerificationCode` + QR (ZXing) sin datos privados.
  - `JavaPrintServiceDocumentPrinter` (cola de Windows, tamaño real, perfiles silenciosos),
    `IssueDocument`, `PrintDocument` (reimpresión auditada que nunca renumera).
  - `V0007__documents.sql`, `SqliteDocumentRepository`, `DocumentJson`.
- **Fase 7 ✅ — Módulos municipales especializados.**
  - Tres modelos compartidos en lugar de diez arquitecturas: `MunicipalAsset` (+ `AssetHolder` como
    historia de tenencia), `Agreement` (arrendamiento/concesión/casilla/permiso con un solo ciclo de
    vida y traspaso encadenado) e `Inspection`.
  - `RegisteredDocument`: la Conservaduría, distinta de un adjunto — libro/folio, partes y
    anotaciones marginales; una entrada registrada se congela. `Decision` (§28).
  - `V0006__municipal_modules.sql`, `SqliteAssetRepository`, `SqliteRegistryRepository`;
    `GrantAgreement`, `TransferAgreement`, `RegisterDocument`, `ConductInspection`.
- **Fase 6 ✅ — Facturación, pagos y caja.**
  - `V0005__billing.sql`: facturas, líneas, pagos, devoluciones y sesiones de caja; dinero como
    unidades menores enteras + ISO-4217, nunca coma flotante.
  - Dominio `Invoice` / `InvoiceLine` / `Payment` / `Refund` / `CashSession`: la historia financiera
    de una factura emitida no cambia en silencio; el sobrepago es **cambio**, no ingreso; la
    diferencia de caja se registra en lugar de corregirse.
  - Casos de uso `IssueInvoice`, `RegisterPayment`, `VoidInvoice`, `RefundPayment`,
    `ManageCashSession`.
  - `SqliteBillingRepository` (que además implementa `ProcedureFinance`) y `SqliteAuditRepository`.
  - UI `BillingView` y `CashView`; `MunicipalLoopIT` cubre el bucle municipal completo.
- **Fase 5 ✅ — Ciudadano y front-office.**
  - `V0004__procedure.sql`: trámites, checklist materializado, valores de formulario, línea de
    tiempo, adjuntos, `numbering_sequence` y `person.search_name` (clave plegada).
  - Dominio `procedure` (`Procedure`, `ProcedureChecklist`, `ProcedureEvent`, `DueDates`) y
    `numbering` (`NumberingSequence`). `shared.text.Normalization` (plegado + similitud).
  - Casos de uso `StartProcedure`, `UpdateProcedureRequirement`, `SaveProcedureForm`,
    `AdvanceProcedure`, `AssignProcedure`, `AddProcedureNote`, `FindDuplicatePeople`.
  - Adaptadores `SqliteProcedureRepository` / `SqliteNumberingRepository`.
  - UI real: login/primer arranque, worklist con colas guardadas, asistente de nuevo trámite con
    detección de duplicados, detalle de trámite (checklist + formulario + acciones del flujo +
    historial) y escritorio de ciudadano. `AppServices` desacopla la UI de la infraestructura.
  - 35 pruebas nuevas, incluida `FrontOfficeUiIT` (JavaFX sobre el grafo real).
- **Fase 4 — Catálogo semilla editable (master prompt §54–§55).**
  - `application.catalog`: `ServiceTemplate` / `ServiceCategoryTemplate` / `ServiceCatalogTemplates`
    (tipados: reutilizan `RequirementDef`, `WorkflowDefinition`, `FeeRule`, `Sla`, `Validity`) y el
    puerto `ServiceCatalogTemplateSource`.
  - Caso de uso `SeedServiceCatalog` (permiso `service.configure`, auditado, idempotente):
    materializa el paquete en categorías + servicios con una versión **v1 `DRAFT`** que el municipio
    revisa (montos, requisitos, flujo) y publica. Vuelve a ejecutarse sin duplicar.
  - `infrastructure`: `JsonServiceCatalogTemplateSource` lee el paquete de la República Dominicana
    (`catalog/dominican-republic/service-catalog.v1.json`, 12 categorías y 93 servicios) reutilizando
    los parsers de `ServiceJson`. Cada plantilla produce un borrador que pasa `ServiceVersionValidator`.
  - Cableado en `CompositionRoot` (`seedServiceCatalog()`).

## [0.1.1] - 2026-08-28

Checkpoint de mantenimiento: desbloquea el despliegue en Vercel (rechazaba la
landing por una versión vulnerable de Next.js) y estabiliza el workflow de
seguridad en CI. Incluye el avance interno de la Fase 4 (evaluador de
expresiones, motor de requisitos y motor de flujo de trabajo tipado), todavía
sin superficie de usuario. Sigue **sin ser la 1.0**.

### Fixed
- **Landing / Vercel:** `next` y `eslint-config-next` `15.1.6 → 15.5.24`. La 15.1.6
  es anterior a CVE-2025-29927 (bypass de autorización en middleware, corregido en
  15.2.3) y a avisos posteriores de 2025; Vercel rechaza el despliegue con esa
  versión. Sin cambios de código en la landing; `lint` / `typecheck` / `build`
  (export estático, 6 rutas) verdes. `package-lock.json` regenerado.
- **CI (Security):** se reemplaza la acción `gitleaks/gitleaks-action` por la CLI
  de gitleaks (el wrapper exige `GITLEAKS_LICENSE` en algunas cuentas; el binario
  MIT no) y se corrige el paso de CodeQL — input `languages:` (plural) y
  `build-mode` explícito por lenguaje; el singular `language:` había dejado de ser
  válido y disparaba autodetección de todos los lenguajes.

### Added
- **Fase 4 — Motor configurable de servicios (continuación).**
  - `domain.rules.ExpressionEvaluator` (ADR 0007): evaluador booleano restringido (`|| && !`,
    comparadores, paréntesis, literales, identificadores del contexto; sin funciones ni I/O).
  - Motor de requisitos: `RequirementContext`, `RequirementsChecklist` (applicable/satisfied/
    pending por ítem; condicionales; bloqueo por etapa — "faltan N requisitos").
  - Motor de flujo de trabajo tipado (`domain.workflow`, ADR 0007): `WorkflowDefinition`,
    `WorkflowStep`, `Transition`, `StepType`, `TransitionKind`, `WorkflowValidator`,
    `WorkflowEngine` (transiciones disponibles + destino; guarda de `PAYMENT_CHECKPOINT`).
  - Form schema tipado (`FormSchema` + `FormField` + `FieldType`).

### Changed
  - `ServiceDefinitionVersion` ahora es **tipada de extremo a extremo**: `feeRules` es
    `List<FeeRule>`, `workflow` es `WorkflowDefinition`, `formSchema` es `FormSchema` (antes
    `JsonDoc` opacos). Solo `outputDocuments`/`authorization` siguen como `JsonDoc`. `ServiceJson`
    (infra) los (de)serializa con Jackson; `ServiceVersionValidator` usa `WorkflowValidator`.
    Tests de servicio y round-trip SQLite actualizados.

## [0.1.0] - 2026-08-28

Primer checkpoint estable promovido a `main` a través de `testing` (`experiment → testing → main`).
Cubre las Fases 0–3 completas y el inicio de la Fase 4. **No es la 1.0**: facturación, impresión,
copias de seguridad y empaquetado llegan en fases posteriores (ver `ROADMAP.md`).

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
  - Motor de tasas tipado (`domain/finance`, ADR 0008): `FeeRule` (inmutable, con vigencia),
    `FeeRuleType`, `ChargeType`, `FeeTier`, `FeeInput`, `FeeCalculator` → `Charge` / `ChargeLine`
    (dinero entero, nunca coma flotante). Aún sin usar en facturación (Fase 6).
  - Tests: `ServiceDefinitionVersionTest`, `ServiceEngineTest`, `SqliteServiceCatalogRepositoryTest`,
    `ServiceVersionValidatorTest`, `FeeCalculatorTest`.
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

[Unreleased]: https://github.com/mrnoirhat/sirmax/compare/v0.1.1...experiment
[0.1.1]: https://github.com/mrnoirhat/sirmax/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/mrnoirhat/sirmax/releases/tag/v0.1.0
