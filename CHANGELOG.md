# Changelog

Todos los cambios notables de SIRMAX se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa
[Versionado Semántico](https://semver.org/lang/es/).

## [Unreleased]

## [1.0.3] - 2026-08-30

### Added
- **Firma de código con la SignPath Foundation.** El ejecutable y el instalador se
  firman desde el propio build. La fundación custodia el certificado en un HSM al
  que el proyecto no tiene acceso, y cada firma exige aprobación manual. El paso
  se salta sin fallar mientras no exista el secreto: una release sin firmar es
  peor que una firmada, pero una que no se puede cortar es peor que las dos.
  - [`docs/CODE-SIGNING-POLICY.md`](docs/CODE-SIGNING-POLICY.md) — el documento que
    SignPath exige publicado, y que permite a un ayuntamiento comprobar que el
    archivo descargado salió de este repositorio.
  - [`docs/SIGNPATH-APPLICATION.md`](docs/SIGNPATH-APPLICATION.md) — la solicitud,
    con el estado de cada requisito.
- **Ayuda → Acerca de**, con la versión leída del manifiesto del jar para que no
  pueda desviarse de lo compilado.
- Documentación completa: las 26 páginas que decían «en construcción» tienen
  contenido real. No queda ningún aviso de obra en el sitio.

### Changed
- **El panel muestra trámites reales.** Antes eran cuatro cifras fijas (2, 4, 7,
  12) heredadas de la demo del sistema de diseño, montadas en la aplicación real.
  Un número inventado en un panel no se distingue de uno real y alguien acaba
  planificando la mañana con él. Ahora cuenta vencidos, sin asignar, asignados a
  ti y abiertos, y cada tarjeta abre la lista.
- Pase de diseño con la paleta actual: profundidad en las tarjetas, estados al
  pasar por encima, más aire en las filas de tabla, y regla lateral además del
  tinte en la navegación seleccionada — el color no puede ser la única señal (§12).
- Dependencias al día: PDFBox 3.0.8, ZXing 3.5.4, sqlite-jdbc 3.53.4.0,
  TypeScript 6.0.3, CodeQL Action v4.

### Fixed
- **La columna Roles imprimía UUIDs** en lugar de nombres de rol.
- **El nombre del editor en la firma estaba mal escrito.**
- **TypeScript 6 estaba bloqueado por error.** Se había descartado junto al 7
  suponiendo la misma causa. El `baseUrl` que rompía estaba en dos sitios: el
  nuestro, que se quitó, y el que publica Docusaurus, para el que TypeScript 6
  trae `ignoreDeprecations: "6.0"`. El bloqueo se mantiene solo para el 7, donde
  la opción desaparece.
- **La acción de SignPath v2 habría roto la firma en silencio.** Declara
  `connector-url` como obligatorio; como el paso está desactivado hasta que exista
  el secreto, el fallo habría aparecido en la primera release firmada con CI en
  verde todo el tiempo.

### Documentation
- `docs/SIGNING.md` corrige dos afirmaciones que eran falsas, verificadas contra
  la documentación de Microsoft y la de SignPath:
  - **Sí existe firma de código gratuita** para proyectos libres que califiquen.
  - **Un certificado EV ya no evita el aviso de SmartScreen**; Microsoft retiró
    ese comportamiento y desaconseja pagar el sobreprecio por ese motivo.

  Consecuencia que el documento ahora deja clara: fuera de la Microsoft Store
  ninguna opción garantiza que no avise el primer día. Un certificado de una CA
  consigue que el aviso muestre un editor verificable y que desaparezca en
  semanas, no que no aparezca.

## [1.0.2] - 2026-08-30

### Added
- **Las cinco pantallas de administración**, que hasta ahora eran marcadores:
  - *Servicios* (§22, §54–§55): el catálogo del ayuntamiento y el alta de servicios.
    Una versión publicada no se edita, porque cada trámite fija la versión con la que
    se abrió (§39); cambiar un servicio vivo es una versión nueva, y la pantalla lo
    impone en vez de explicarlo.
  - *Documentos* (§46, §59D, §59F): búsqueda por número o código de verificación,
    reimpresión con motivo obligatorio y marca COPIA, e historial de impresión.
    También los perfiles de impresora.
  - *Configuración* (§41–§42, §59C): identidad del ayuntamiento, tema, programación
    e historial de copias, política de seguridad y verificación de la cadena de
    auditoría.
  - *Departamentos* (§21): alta y archivado — nunca borrado, porque los trámites ya
    enrutados siguen refiriéndose a ellos.
  - *Reportes* (§36): cobros por medio de pago y por servicio, trámites por estado y
    lo pendiente de cobro, todo sobre un mismo rango de fechas y exportable a CSV.
- **Emisión de facturas desde el mostrador.** `IssueInvoice` existía y no se llamaba
  desde ninguna parte, así que un trámite que requería pago podía abrirse pero nunca
  cobrarse. Facturación ahora empieza por los casos pendientes de facturar, y excluye
  los que ya tienen factura: reemitir le daría al ciudadano dos documentos por un
  mismo cargo.
- **Marca e icono** en la aplicación de escritorio, la landing y la documentación.
  Se dibujan desde una sola geometría en Java2D, de modo que se regeneran en una
  máquina que solo tenga un JDK.
- Auditoría automática de claves de traducción (`MessageKeyAuditTest`) y cobertura de
  las pantallas nuevas contra el grafo real (`BackOfficeUiIT`).

### Fixed
- **Modo oscuro.** No existía ninguna regla para la barra de menú, así que JavaFX la
  pintaba desde `modena.css`, que es clara sin condición: la única franja siempre
  visible se quedaba en blanco. Lo mismo ocurría con las barras de desplazamiento,
  los menús contextuales, los diálogos, los desplegables, los *spinners* y el
  selector de fecha — todos son ventanas aparte o controles compuestos a los que el
  sistema de tokens no llegaba.
- **La landing no se desplegaba desde 1.0.1.** `vercel.json` fijaba
  `outputDirectory` en `out` junto a `framework: nextjs`; Next con `output: "export"`
  lo resuelve el propio builder de Vercel, y decirle además dónde está la salida lo
  manda a buscar un `routes-manifest.json` que un export estático no genera. El build
  aparecía en verde y fallaba en la última línea, así que parecía un problema de
  caché.
- La landing arrastraba una segunda sección `id="capturas"` de la Fase 2 — tres
  recuadros punteados y un texto diciendo que el shell estaba en construcción — que
  además hacía que el ancla `#capturas` no fuera única.
- Tres constantes de enumeración (`charge.type.recargo`, `service.type.pago_externo`,
  `backup.kind.pre_migration`) no tenían traducción y se mostraban como su clave.
- Los checks obligatorios filtraban por rutas en `pull_request`. GitHub espera
  indefinidamente por un check que nunca reporta, así que cualquier PR que no tocara
  ese directorio habría quedado imposible de fusionar.

## [1.0.1] - 2026-08-29

### Added
- Descarga directa del instalador y del portable desde la landing, y workflow de
  release que construye el MSI en CI y publica los artefactos con sus sumas SHA-256.
- La landing, la documentación y GitHub enlazados entre sí con URLs que resuelven.

### Fixed
- **La documentación no cargaba sus assets.** Se publica en dos sitios con base
  paths distintos — Vercel en la raíz, GitHub Pages bajo `/sirmax/` — y un único
  `baseUrl` fijo rompe aquel para el que no fue escrito. Ahora lo elige el destino.
- El README enlazaba a `#` en «Sitio web» y «Documentación», y sus badges de CI
  apuntaban a `testing` en vez de a `main`.

### Changed
- Doce de las catorce actualizaciones de Dependabot aplicadas: JUnit 6, Jackson
  2.22, Next 16, React 19.2, AssertJ, SLF4J, Logback y las acciones de GitHub.
- **Retenidas con motivo**: TypeScript 7 (Docusaurus 3.10 publica un tsconfig con
  `baseUrl`, que la 7 eliminó) y ESLint 10 (`eslint-plugin-react`, que llega vía
  `eslint-config-next` 16, usa la API de contexto de la 9).
- `next lint` desapareció en Next 16: el lint llama a ESLint directamente y la
  configuración migró a flat config.

## [1.0.0] - 2026-08-29

### Added
- **Fase 13 ✅ — Hardening.** Auditorías ejecutables de rendimiento (`PerformanceAuditIT`, con
  20 000 ciudadanos y 20 000 trámites, leyendo el plan de ejecución), migraciones
  (`MigrationAuditTest`) y accesibilidad/UX (`AccessibilityAuditTest`). Informe en
  [`docs/HARDENING.md`](docs/HARDENING.md), incluida la lista de lo que **no** cubre.
  `V0010__foreign_key_indexes.sql` indexa las claves foráneas que la aplicación recorre.
- **Fase 12 ✅ — Landing y documentación a producción.**
  - Guía de usuario real (trámites, ciudadanos, facturación, caja, impresión, documentos, registro,
    copias, restauración, seguridad) en lugar de plantillas.
  - `ScreenshotGenerator`: capturas fieles desde `Scene.snapshot()`, no del escritorio.
  - Docusaurus se despliega a GitHub Pages desde `main`; `url`/`baseUrl` y el enlace landing → docs
    corregidos a rutas que existen.

### Fixed
- La ventana por defecto no cabía en pantallas de 1280×720 y dejaba el botón de primer arranque
  fuera de pantalla. Ahora se ajusta al tamaño real de la pantalla y se centra.
- La tarjeta de acceso se estiraba a todo el alto de la ventana.
- `LoginViewLayoutTest` mide el layout con la hoja de estilos real.

### Added
- **Fase 11 ✅ — Empaquetado para Windows.**
  - `jlinkRuntime` (runtime recortado), `packageAppImage` (carpeta autocontenida, siempre
    disponible) y `packageWindows` (MSI; se salta con un mensaje si falta WiX).
  - `verifyReleaseArtifacts` falla si el runtime no quedó dentro del artefacto.
  - Datos en `%LOCALAPPDATA%\SIRMAX`: actualizar conserva, desinstalar deja.
  - Job `package` en CI y [`docs/PACKAGING.md`](docs/PACKAGING.md).
- **Fase 10 ✅ — Seguridad, auditoría y fiabilidad.**
  - `AuditChain`: cada entrada de auditoría se encadena con la anterior por SHA-256, así una
    alteración o un borrado son detectables aunque se eliminen los triggers. `VerifyAuditIntegrity`.
  - Bloqueo de cuenta con expiración automática y registro de intentos (`login_attempt`);
    usuario inexistente y contraseña errónea responden idéntico.
  - `SecurityPolicy` (longitud mínima, umbral de bloqueo, inactividad, tamaño de adjunto) y
    `AttachmentValidator` por magic bytes — un ejecutable renombrado a `.pdf` se rechaza.
  - `V0009__security_hardening.sql`, `SqliteSecurityPolicyRepository`; `SqliteAuditSink` ahora
    escribe la cadena.
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
