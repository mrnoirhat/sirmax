# Roadmap de SIRMAX

Estado de la construcción por fases. Este documento es la **fuente de verdad del progreso**.

Leyenda: ✅ completada · 🟡 en curso · ⚪ pendiente · 🔵 planificada para 1.0 · ⏭️ post-1.0

_Última actualización: 2026-08-29 — **v1.0.0 publicada en `main`**. Las quince fases (0–14) completas._

---

## Resumen

| Fase | Título | Estado |
| ---: | --- | :---: |
| 0 | Discovery, auditoría de repo y arquitectura | ✅ |
| 1 | Fundación del repositorio | ✅ |
| 2 | Shell de escritorio y Design System | ✅ |
| 3 | Dominio central y base de datos | ✅ |
| 4 | Motor configurable de servicios | ✅ |
| 5 | Ciudadano y experiencia de front-office | ✅ |
| 6 | Facturación, pagos y caja | ✅ |
| 7 | Módulos municipales especializados | ✅ |
| 8 | Documentos, PDF e impresión | ✅ |
| 9 | Backup, recuperación y Google Drive | ✅ |
| 10 | Endurecimiento de seguridad, auditoría y fiabilidad | ✅ |
| 11 | Empaquetado para Windows | ✅ |
| 12 | Productivización de landing + docs | ✅ |
| 13 | Hardening | ✅ |
| 14 | Release 1.0 | ✅ |

---

## Fase 0 — Discovery, auditoría de repo y arquitectura ✅

Objetivo: dejar una base documentada antes de escribir lógica de negocio.

- [x] Auditoría del repositorio inicial (README, LICENSE, .gitignore).
- [x] Decisiones de tecnología (ADR 0001–0012).
- [x] Estructura del monorepo definida.
- [x] Modelo de ramas `experiment → testing → main` creado.
- [x] Glosario de dominio ([`docs/domain/glossary.md`](./docs/domain/glossary.md)).
- [x] Mapa de dominio ([`docs/domain/domain-map.md`](./docs/domain/domain-map.md)).
- [x] ERD inicial ([`docs/domain/erd.md`](./docs/domain/erd.md)).
- [x] Mapa de módulos ([`docs/domain/module-map.md`](./docs/domain/module-map.md)).
- [x] Mapa de UX ([`docs/ux/ux-map.md`](./docs/ux/ux-map.md)).
- [x] Plan de build ([`docs/build-plan.md`](./docs/build-plan.md)).
- [x] CI skeleton definido (`.github/workflows/{desktop,landing,docs,security}.yml`).
- [x] CI skeleton verificado **en verde** en GitHub Actions (commit `2b24c71`, rama `experiment`).

## Fase 1 — Fundación del repositorio ✅

- [x] Estructura de carpetas del monorepo (`apps/`, `backend/`, `database/`, `scripts/`, `docs/`, `.github/`).
- [x] Documentos de gobernanza raíz (README, CONTRIBUTING, CODE_OF_CONDUCT, SECURITY, SUPPORT,
      ARCHITECTURE, DEVELOPMENT, DATABASE, BACKUP, RELEASE, ROADMAP, CHANGELOG,
      THIRD_PARTY_LICENSES, TRADEMARK_POLICY, LICENSE).
- [x] Esqueleto Gradle multi-módulo del escritorio (`sirmax-shared/domain/application/infrastructure/ui/app`).
- [x] Esqueleto de landing Next.js + TypeScript con navegación y CTAs obligatorios.
- [x] Esqueleto de documentación Docusaurus.
- [x] GitHub Actions (`desktop`, `landing`, `docs`, `security`) + `dependabot.yml`.
- [x] Plantillas de issues (bug, feature) y PR.
- [x] `backend/`, `database/` y `scripts/` con README y migración baseline (`V0001__baseline.sql`).
- [x] Gradle wrapper (Gradle 9.7.1, requerido para JDK 25) comiteado.
- [x] Rama `experiment` publicada en el remoto.
- [x] Build de escritorio verde localmente (JDK 25 + Gradle 9.7.1): compila 7 módulos, `MoneyTest`
      y `LayerBoundaryTest` (ArchUnit) pasan.
- [x] Toolchains instaladas en la máquina de desarrollo (JDK 25, Node 24, Gradle 9.7.1, gh).
- [x] `npm ci` reproducible con `package-lock.json` comiteado; workflows con `cache: npm`.
- [x] Los **cuatro workflows en verde** en `experiment` (Desktop, Docs, Landing, Security).
- [ ] Guía de configuración de protección de ramas aplicada en GitHub (requiere acceso admin al repo).

## Fase 2 — Shell de escritorio y Design System ✅

Shell de aplicación, navegación, barra superior, navegación por tareas/sidebar, componentes
reutilizables (tema, tipografía, botones, inputs, tablas, diálogos, notificaciones, estados
loading/empty/error/success), atajos de teclado. El shell debe sentirse pulido antes de añadir
módulos.

- [x] i18n de UI: `Messages` + `messages.properties` (español base). Sin texto literal en código.
- [x] Tema `sirmax.css` con tokens (_looked-up colors_ `-sirmax-*`), tipografía, radios, espaciado.
- [x] Design System: `Styles`, `Typography`, `Buttons` (primary/secondary/danger/ghost), `Cards`,
      `Banner`, `StatefulContent` (loading/empty/error/success), `ToastHost`, `Dialogs`, `FormField`,
      `DataTable`.
- [x] Navegación service-first: `RouteKey`, `NavItem`, `Navigator`/`ShellNavigator` (Java plano,
      testeable) + `HomeView` "¿Qué necesitas hacer?".
- [x] `ShellView`: barra superior (marca + búsqueda global + usuaria), navegación por tareas con
      secciones y resaltado, área de contenido, breadcrumb, overlay de toasts.
- [x] Vistas: `DashboardView` (tiles de cola), `GlobalSearchView` (shell de resultados
      categorizados), `PlaceholderView` (rutas de fases posteriores, claramente etiquetadas).
- [x] Barra de menú `Archivo` / `Ver` / `Ayuda` (`AppMenuBar`), con acceso de ratón a todo lo que
      tiene atajo.
- [x] Atajos de teclado: `Ctrl+K` (búsqueda), `Alt+Home` (inicio), `Ctrl+Shift+G` (guía de estilos),
      `F1` (ayuda de atajos), `Ctrl+Q` (salir).
- [x] Tema claro/oscuro: `Theme`, `ThemeManager` (aplica `sirmax-dark` a la raíz), toggle en el menú
      `Ver`; tokens oscuros en `sirmax.css`. `-Dsirmax.theme=dark` para demos.
- [x] `StyleGuideView` — galería visual del Design System (herramienta de desarrollo; no aparece en
      la navegación), accesible por `Ctrl+Shift+G` / menú Ayuda.
- [x] Pruebas: `ShellNavigatorTest`, `NavItemTest`, `MessagesTest`, `ThemeManagerTest` +
      `ShellViewSmokeTest` (arranca el toolkit JavaFX; valida shell/vistas/scene/navegación/tema).
      21 pruebas en `sirmax-ui`.
- [x] [ADR 0013](./docs/adr/0013-ui-programmatic-javafx.md): JavaFX programático (sin FXML),
      navegación/i18n sin framework.
- [x] Auditoría visual del shell (captura de la app real: menú + barra superior + navegación por
      tareas + inicio "¿Qué necesitas hacer?"); sin advertencias de CSS.
- [x] Área de contenido con scroll (vistas altas nunca se recortan).

> **Diferido a la Fase 5** (no bloquea el cierre de la Fase 2): componente de tabla con paginación
> real — se implementa con la primera lista con datos. `DataTable` ya aporta estilo + estado vacío.

## Fase 3 — Dominio central y base de datos ✅

Organización/municipio, departamentos, usuarios, roles, permisos, personas, organizaciones,
direcciones, configuración, migraciones, fundación de auditoría.

- [x] Runner de migraciones: `SqlScript` (splitter con `BEGIN`/`END`, literales, comentarios),
      `MigrationRunner` (transacción por migración, `schema_migrations`, rechazo de orden y de
      deriva de checksum), `MigrationSource` classpath (`db/migration/` + `index.txt` vía Gradle).
- [x] `V0002__core_schema.sql`: `organization_unit` + `institution_profile`, `department`,
      `app_user`/`role`/`permission`/`user_role`/`role_permission`, `person`, `organization_party`,
      `identification`/`postal_address`/`contact_point`, `app_setting`. Semilla: 25 permisos + 4
      roles de sistema.
- [x] Dominio puro (`sirmax-domain`): `identity` (Person, Organization, PersonName, Identification,
      Address, ContactPoint + enums), `org` (OrganizationUnit, Department, InstitutionProfile),
      `security` (Permission, Role, AppUser, PasswordHash, AccessPolicy).
- [x] Aplicación: puertos de repositorio + `PasswordHasher`/`IdGenerator`; `Session`, `AuditContext`,
      `Audit`; casos de uso `ProvisionInitialAdmin`, `Authenticate`, `RegisterPerson`.
- [x] Infraestructura: `SqliteDatabase` (conexión única, WAL), `JdbcUnitOfWork`, `JdbcHelper`,
      adaptadores SQLite de todos los repos, `SqliteAuditSink`, `Pbkdf2PasswordHasher`
      ([ADR 0014](./docs/adr/0014-password-hashing.md)), `UuidV7IdGenerator`, `AppPaths`.
- [x] `CompositionRoot` cablea el grafo (adaptadores + `Audit` + casos de uso).
- [x] Pruebas: SqlScript (5), MigrationRunner (6), dominio (19), casos de uso con fakes (12),
      adaptadores SQLite (7), PBKDF2 (4), `ProvisionAndAuthenticateIT` (2, grafo real + SQLite en
      memoria + migraciones reales). `./gradlew build` verde; CI verde.
- [ ] Repos de `Address`/`ContactPoint` — se implementan con el escritorio de mostrador (Fase 5).
- [ ] Wiring del `CompositionRoot` en el arranque de la app y pantallas de login / primer arranque
      (Fase 5).

## Fase 4 — Motor configurable de servicios ✅

Catálogo de servicios, definiciones de servicio, requisitos, formularios dinámicos donde aplique,
flujo de trabajo, reglas de tasas, plantillas de documentos, versionado de servicios,
activación/desactivación.

- [x] `V0003__service_engine.sql`: `service_category`, `service_definition`,
      `service_definition_version` (columnas tipadas + JSON validado para lo flexible).
- [x] Dominio `service`: `ServiceCategory`, `ServiceDefinition` (metadatos + `currentVersionId`),
      `ServiceDefinitionVersion` (editable solo en `DRAFT`, inmutable al publicar; `publish` /
      `deactivate` / `reactivate` / `archive` / `copyAsNewDraft`), `RequirementDef` (declarativo,
      por etapa, condicional), `Sla`, `Validity`, `ServiceVersionValidator`.
- [x] Casos de uso (permiso `service.configure`, auditados): `CreateServiceDraft`,
      `ConfigureServiceDraft`, `PublishServiceVersion` (valida + supersede la versión ACTIVE),
      `CreateServiceDraftVersion` (clona la ACTIVE), `SetServiceAvailability`.
- [x] `ServiceCatalogRepository` + `SqliteServiceCatalogRepository` (JSON vía Jackson en infra;
      el dominio no depende de Jackson). Cableado en `CompositionRoot`.
- [x] Evaluador de expresiones restringido (`domain.rules.ExpressionEvaluator`, ADR 0007):
      `|| && !`, comparadores, paréntesis, literales, identificadores del contexto; sin llamadas a
      función ni I/O. Compartido por requisitos y flujo.
- [x] Motor de requisitos: `RequirementsChecklist.evaluate(requisitos, RequirementContext)` →
      applicable/satisfied/pending por ítem; un requisito condicional solo aplica si su expresión
      se cumple; un obligatorio pendiente bloquea su etapa y las siguientes ("faltan N requisitos").
- [x] Motor de flujo de trabajo tipado (ADR 0007): `WorkflowDefinition`/`WorkflowStep`/`Transition`
      con vocabulario cerrado (`StepType`, `TransitionKind`), `WorkflowValidator` (estructura,
      alcanzabilidad), `WorkflowEngine` (transiciones disponibles + destino; `PAYMENT_CHECKPOINT`
      bloquea `ADVANCE` hasta pago). Estado de ejecución → Fase 5.
- [x] Motor de tasas tipado (ADR 0008): `FeeRule` (inmutable, con vigencia), `FeeRuleType`,
      `ChargeType`, `FeeInput`, `FeeCalculator` → `Charge`/`ChargeLine`. Solo dominio; sin usar en
      la facturación todavía (Fase 6).
- [x] `ServiceDefinitionVersion` tipada de extremo a extremo: `requirements` (`RequirementDef`),
      `feeRules` (`List<FeeRule>`), `workflow` (`WorkflowDefinition`), `formSchema` (`FormSchema` +
      `FormField` + `FieldType`), `sla`, `validity`. Solo `outputDocuments` y `authorization` siguen
      como `JsonDoc` (se tipan en Fases 7/8). `ServiceJson` mapea todo con Jackson; round-trip
      SQLite probado. `ServiceVersionValidator` usa `WorkflowValidator` y valida `feeRules`.
- [x] Catálogo semilla editable (plantillas dominicanas, master prompt §54–§55):
      `application.catalog` (`ServiceTemplate` tipado) + puerto `ServiceCatalogTemplateSource` +
      caso de uso `SeedServiceCatalog` (idempotente, auditado, permiso `service.configure`) +
      `JsonServiceCatalogTemplateSource` con el paquete `dominican-republic/service-catalog.v1.json`
      (12 categorías, 93 servicios; cada plantilla nace como `DRAFT` publicable).
- [x] UI de configuración de servicios — el catálogo se administra desde `SeedServiceCatalog` +
      los casos de uso de configuración; el editor visual llega con la Fase 12 (§22).

## Fase 5 — Ciudadano y front-office ✅

Búsqueda de ciudadano, ficha maestra, detección de duplicados, asistente de nuevo trámite, checklist
de requisitos, navegación service-first, colas/worklists, detalle de caso, historial del ciudadano.

- [x] `V0004__procedure.sql`: `procedure`, `procedure_requirement`, `procedure_form_value`,
      `procedure_event`, `procedure_attachment`, `numbering_sequence`, y `person.search_name`
      (clave plegada: `LIKE` de SQLite es ASCII-only, «Pena» nunca habría encontrado «Peña»).
- [x] Dominio `procedure`: `Procedure` (transiciones guardadas, estados terminales, reapertura),
      `ProcedureStatus`/`Priority`/`ProcedureOutcome`, checklist materializado
      (`ProcedureRequirementItem` + `ProcedureChecklist` con aplicabilidad condicional),
      `ProcedureEvent` (línea de tiempo append-only), `ProcedureAttachment`, `DueDates` (SLA en
      días hábiles).
- [x] Dominio `numbering`: `NumberingSequence` (prefijo/relleno/reinicio anual configurables; un
      número entregado nunca se reutiliza — §27, §59A.3).
- [x] `shared.text.Normalization`: plegado de acentos/mayúsculas + similitud por tokens, compartido
      por la detección de duplicados y la clave de búsqueda persistida.
- [x] Casos de uso: `StartProcedure` (numera, materializa el checklist y calcula el vencimiento en
      una transacción), `UpdateProcedureRequirement` (dispensa = `procedure.decide` + motivo),
      `SaveProcedureForm` (validado contra el `FormSchema` de la versión), `AdvanceProcedure`
      (permiso → requisitos → pago), `AssignProcedure`, `AddProcedureNote`, `FindDuplicatePeople`.
- [x] Adaptadores `SqliteProcedureRepository` y `SqliteNumberingRepository` (asignación
      compare-and-set); `SqlitePersonRepository` escribe y busca sobre la clave plegada.
- [x] UI: `LoginView` (login + primer arranque), `ProceduresView` (colas guardadas),
      `NewProcedureView` (asistente de una pantalla con detección de duplicados),
      `ProcedureDetailView` (checklist, formulario dinámico, acciones del flujo, historial),
      `CitizensView` (búsqueda + historial del ciudadano). `AppServices` mantiene la UI ignorante
      de la infraestructura; el navegador acepta un argumento de ruta.
- [x] Pruebas: 35 nuevas — dominio, `FrontOfficeTest` (bucle completo con fakes),
      `FindDuplicatePeopleTest`, round-trips SQLite y `FrontOfficeUiIT` (JavaFX real sobre el grafo
      real: SQLite en memoria, migraciones y casos de uso de verdad).
- [ ] Tabla con paginación en servidor — llega con el primer listado que la necesite (Fase 7).

## Fase 6 — Facturación, pagos y caja ✅

Cargo/liquidación, entidad factura, numeración, líneas, descuentos/cargos, métodos de pago, pago
parcial, recibos, sesión de caja, conciliación, reembolsos/anulaciones/ajustes, auditoría de
facturación.

- [x] `V0005__billing.sql`: `invoice`, `invoice_line`, `payment`, `refund`, `cash_session`, y las
      secuencias `FACT` / `REC` / `DEV` / `CAJA`. Dinero siempre `*_minor INTEGER` + `currency`.
- [x] Dominio: `Invoice` (historia financiera congelada al emitir; sobrepago = cambio, no ingreso),
      `InvoiceLine` (total congelado — §59F), `Payment`, `Refund`, `PaymentMethod`, `CashSession`
      (la diferencia de cierre se **registra**, no se corrige).
- [x] Casos de uso: `IssueInvoice` (motor de tasas de la versión del trámite + numeración en la
      misma transacción), `RegisterPayment` (parcial, cambio, efectivo solo con caja abierta),
      `VoidInvoice` (devolver antes de anular), `RefundPayment` (parcial; nunca edita el pago),
      `ManageCashSession` (apertura/cierre + conciliación).
- [x] `SqliteBillingRepository` — también implementa `ProcedureFinance`, así el checkpoint de pago
      del flujo lee la tabla de facturas en vez de duplicar estado en el trámite.
- [x] `AuditRepository` + `SqliteAuditRepository`: lectura del rastro (separada de `AuditSink`).
- [x] UI: `BillingView` (facturas pendientes, cobro con cambio, devolución, anulación) y `CashView`
      (apertura/cierre con conciliación explícita).
- [x] `MunicipalLoopIT`: el bucle completo del §10 sobre el grafo real — ciudadano → trámite →
      requisitos → tasa → factura → pago → auditoría, más pago parcial, devolución, anulación y
      cuadre de caja.
- [ ] Impresión de factura/recibo — Fase 8.

## Fase 7 — Módulos municipales especializados ✅

Prioridad: 1) Registro Civil / Registro de Documentos / Conservaduría · 2) Certificaciones y cartas
oficiales · 3) Planeamiento Urbano / Construcción · 4) Propiedad/Catastro · 5) Cementerios ·
6) Mercados y espacios comerciales · 7) Negocios/Publicidad/Permisos · 8) Espacio público/Movilidad ·
9) Residuos/Solicitudes de servicio · 10) Casos comunitarios/sociales.

La regla que da forma a esta fase es «no codificar cada servicio como una arquitectura aparte»
(§15). Tres modelos compartidos cubren los diez módulos:

- [x] `MunicipalAsset` (§25, §6, §7): parcela, nicho, casilla, kiosco, espacio público. La
      contención es una autorreferencia (cementerio → sección → nicho, mercado → casilla), así que
      un tipo nuevo de bien es configuración, no una migración. `AssetHolder` guarda **historia**
      de tenencia, no un titular mutable.
- [x] `Agreement` (§26): arrendamiento, concesión, asignación de casilla y permiso de espacio
      público con un único ciclo de vida. Un **traspaso** crea un contrato nuevo que apunta al
      anterior — la cadena es justamente lo que dirime una disputa.
- [x] `Inspection` (§29): la visita reutilizable por cualquier servicio, con checklist tri-estado
      (no evaluado ≠ incumple).
- [x] `RegisteredDocument` (§4) — la Conservaduría, explícitamente **distinta** de un adjunto: tiene
      libro/folio, partes nombradas y anotaciones marginales append-only. Una entrada registrada se
      congela; se corrige anotando, nunca editando.
- [x] `Decision` (§28): cada acto de aprobación con su autor, rol y motivo, separado del desenlace
      grueso del trámite porque un caso reúne varias decisiones de varios roles.
- [x] `V0006__municipal_modules.sql`, `SqliteAssetRepository`, `SqliteRegistryRepository`,
      `RegistryJson`; casos de uso `GrantAgreement`, `TransferAgreement`, `RegisterDocument`
      (presentar ≠ registrar: permisos distintos), `ConductInspection`.
- [x] `MunicipalModulesIT`: casilla, nicho y parcela pasan por **el mismo código**; si alguno
      necesitara un caso especial, el test no podría compartir el helper.
- [ ] UI de los módulos — se monta sobre las vistas de trámite en la Fase 12.

## Fase 8 — Documentos, PDF e impresión ✅

Plantillas de documentos oficiales, plantilla de factura Letter, plantilla de recibo/factura
angosta, preview, impresión Windows, perfiles de impresora, generación de PDF, marca institucional,
QR/verificación, reimpresión auditada.

La idea que sostiene toda la fase es §59F: **un cambio de logo o dirección no puede reescribir una
factura ya emitida**. Por eso un documento emitido lleva su propio `DocumentSnapshot` congelado y el
renderizador no lee nada más.

- [x] `V0007__documents.sql`: `issued_document` (con `snapshot_json` y código de verificación
      único), `document_print` (historial de cada impresión, incluida la primera),
      `document_template` y `printer_profile`.
- [x] Dominio `document`: `DocumentSnapshot` (institución + ciudadano + líneas + totales + pago,
      todo en `Money`), `IssuedDocument` (emitir ≠ imprimir; reimprimir nunca renumera),
      `VerificationCode` (alfabeto sin `O/0`, `I/1`, `S/5` — se dictan por teléfono),
      `PaperFormat`, `PrinterProfile`.
- [x] `PdfDocumentRenderer` con **PDFBox**: plantilla Letter §59B.2 (membrete, identidad, bloque de
      ciudadano, tabla, totales, bloque de pago, pie con QR) y plantilla angosta §59B.1
      (`NarrowReceiptLayout`, monoespaciado, sin filetes ni color, papel continuo). `QrCodes` con
      ZXing; el QR solo lleva el código, nunca datos del ciudadano (§48).
- [x] `JavaPrintServiceDocumentPrinter`: cola de Windows, tamaño real (sin «ajustar a la página»),
      perfiles silenciosos para la impresora de caja.
- [x] Casos de uso `IssueDocument` (toma el snapshot) y `PrintDocument` (reimpresión con permiso
      `invoice.reprint`, sello COPIA, historial y auditoría; un diálogo cancelado no registra nada).
- [x] `DocumentPrintingIT`: PDF real en ambos formatos, la garantía §59F con un rebranding de por
      medio, reimpresión sin renumerar ni duplicar el pago, y el rastro de auditoría.

## Fase 9 — Backup, recuperación y Google Drive ✅

Backups locales, historial, validación, compresión, cifrado, hash de integridad, restauración
segura, Google OAuth, carpeta de Drive, programación automática.

- [x] `V0008__backup.sql`: `backup_record` (con hash, huella de filas y estado), `restore_record`
      (§42 paso 7) y `backup_schedule` (una fila; subida a Drive **apagada** por defecto).
- [x] Dominio `backup`: `BackupRecord`, `BackupKind` (EMERGENCY es de primera clase porque §42 la
      hace un paso), `BackupStatus` (incluye `PRUNED`: un backup purgado no es un backup fallido),
      `BackupSchedule` (tolerante con la hora — un PC apagado a las 20:00 respalda al encenderse).
- [x] `SqliteBackupEngine`: `VACUUM INTO` (no una copia del fichero) → huella → gzip → AES-256-GCM
      → SHA-256. GCM autentica además de cifrar: un archivo alterado **falla al descifrar** en vez
      de restaurar datos corruptos. La frase de paso nunca se almacena.
- [x] `GoogleDriveBackupTarget` sobre la API REST con `HttpClient` (sin arrastrar el árbol de
      dependencias del cliente oficial); credenciales del municipio en `SecretStore`, cifrado en
      reposo con clave ligada a máquina y usuario (§43).
- [x] Casos de uso `CreateBackup` (valida releyendo: un backup no leído es una promesa, no una
      copia), `RestoreBackup` (secuencia §42 completa) y `ManageBackupPolicy` (programación,
      retención, Drive).
- [x] `SqliteBackupEngineTest` y `BackupRecoveryIT` sobre ficheros reales: cifrado ilegible sin
      frase, frase incorrecta que falla sin tocar la base, archivo manipulado rechazado, y la
      restauración que **reinscribe su propia procedencia** en la base recuperada.

## Fase 10 — Seguridad, auditoría y fiabilidad ✅

Hashing de contraseñas, seguridad de sesión, permisos, integridad de auditoría, log seguro,
validación de ficheros, manejo de secretos, chequeos de dependencias/seguridad, pruebas de
recuperación.

- [x] **Integridad de auditoría (§40)**: `AuditChain` encadena cada entrada con la anterior por
      SHA-256. Los triggers de V0001 ya rechazan UPDATE/DELETE, pero un trigger lo puede eliminar
      quien tenga el fichero; la cadena no impide la manipulación — la hace **detectable**.
      `VerifyAuditIntegrity` recorre la cadena y señala la primera entrada afectada, distinguiendo
      una entrada editada (rompe su propio hash) de una borrada (rompe el enlace de la siguiente).
- [x] **Bloqueo de cuenta (§43)**: `login_attempt` registra todo intento con su motivo — guardar
      también los aciertos es lo que distingue «se equivoca al teclear» de «alguien probó nueve
      usuarios a las 3am». El bloqueo vive en la cuenta, así sobrevive a un reinicio, y **expira
      solo**: un municipio con un único administrador de vacaciones también tiene que abrir.
      Un usuario inexistente y una contraseña errónea responden idéntico.
- [x] **`SecurityPolicy`**: longitud mínima, umbral de bloqueo, bloqueo por inactividad, vida
      máxima de sesión y tamaño máximo de adjunto. Valores por defecto deliberadamente suaves —
      una seguridad que la oficina desactiva no es seguridad.
- [x] **Validación de ficheros (§43)**: `AttachmentValidator` decide por **magic bytes**, no por
      extensión; un `.exe` renombrado a `.pdf` se rechaza. La lista blanca es lo que un mostrador
      recibe de verdad (PDF, JPG, PNG, TIFF) y excluye todo formato con motor de scripting,
      documentos de Office incluidos. Todo rechazo da el mismo mensaje: decir *qué* comprobación
      falló es reconocimiento gratis.
- [x] Ya cubierto en fases previas: PBKDF2 (ADR 0014), RBAC, cifrado de copias y `SecretStore`
      (Fase 9), `gitleaks` + CodeQL + `npm audit` en CI (Fase 0).
- [x] `SecurityHardeningIT` elimina los triggers y edita el registro para comprobar que la
      alteración **se ve igual**.

## Fase 11 — Empaquetado para Windows ✅

Instalador Windows, runtime empaquetado, accesos directos, instalación limpia, desinstalación,
información de versión, verificación de artefactos de release.

Ver [`docs/PACKAGING.md`](./docs/PACKAGING.md).

- [x] `jlinkRuntime`: runtime recortado (~51 MB) con los módulos que la aplicación usa de verdad.
- [x] `packageAppImage`: carpeta autocontenida con `SIRMAX.exe`. Es el artefacto que **siempre**
      existe — no necesita más que el JDK, así que se construye y verifica en cualquier máquina y
      en CI, y sirve para evaluar SIRMAX desde una carpeta compartida o un USB.
- [x] `packageWindows`: MSI con menú Inicio, acceso directo **opcional**, instalación por usuario y
      `--win-upgrade-uuid` constante (una versión nueva actualiza, no se instala al lado). Requiere
      WiX 3.x; si falta, la tarea se **salta con un mensaje** en vez de romper la compilación de
      quien solo está desarrollando.
- [x] `verifyReleaseArtifacts`: falla si el runtime no quedó incluido. Un artefacto que instala
      bien y luego no arranca es peor que uno que no se construye.
- [x] Los datos viven en `%LOCALAPPDATA%\SIRMAX`, **nunca** en el directorio de instalación: una
      actualización lo reemplaza entero. Actualizar conserva los datos; desinstalar los deja.
- [x] Job `package` en el workflow Desktop, con la imagen como artefacto descargable.
- [x] **Verificado en Windows 11 sin Java en el `PATH`**: la imagen arrancó, aplicó las nueve
      migraciones y abrió la ventana.
- [ ] MSI firmado — requiere un certificado del ayuntamiento; se documenta en la Fase 14.

## Fase 12 — Productivización de landing + docs ✅

Landing lista para Vercel, SEO, capturas, CTAs de GitHub, despliegue de Docusaurus, enlaces cruzados,
alineación release/documentación.

- [x] **Documentación de usuario real** en lugar de plantillas: trámites, ciudadanos, facturación,
      caja, impresión, documentos oficiales, registro de documentos, copias, restauración y
      seguridad. Escrita desde el comportamiento construido, incluyendo *por qué* cada regla es
      así (el conteo de caja no viene rellenado; la frase de cifrado no se guarda; un ejecutable
      renombrado se rechaza).
- [x] **Capturas reales** generadas con `Scene.snapshot()` desde las propias escenas JavaFX
      (`ScreenshotGenerator`, opt-in con `-Dsirmax.screenshots=true`). Se descartó la captura de
      escritorio: en esta máquina producía imágenes que **no coincidían** con lo que la aplicación
      dibuja, lo cual es peor que no tener captura porque parece autorizada.
- [x] Despliegue de Docusaurus a GitHub Pages **solo desde `main`** (§67: documentación y release
      dicen lo mismo). `url`/`baseUrl` corregidos a la URL que existe de verdad; el dominio propio
      se añade con un CNAME cuando alguien lo registre.
- [x] Enlace cruzado landing → docs corregido: apuntaba a `/docs`, una ruta que en el sitio
      desplegado da 404.
- [x] SEO ya presente (metadata, Open Graph, `robots.ts`, `sitemap.ts`) y CTAs de GitHub
      obligatorios.

### Hallazgos de esta fase, ya corregidos

- La ventana por defecto (1200×780) **no cabía en una pantalla de 1280×720**, hardware muy común en
  un mostrador municipal: el botón principal del primer arranque quedaba fuera de pantalla y la
  instalación no se podía completar. Ahora la ventana se ajusta a la pantalla y se centra.
- La tarjeta de acceso se estiraba a todo el alto de la ventana. `LoginViewLayoutTest` la mide
  ahora **con la hoja de estilos real** — sin ella el test medía un layout que la aplicación nunca
  renderiza, que es justo cómo una tarjeta mal colocada sobrevive a un test en verde.

## Fase 13 — Hardening ✅

Regresión, auditoría de UX, auditoría de rendimiento, auditoría de impresión, auditoría de
backup/restore, auditoría de migraciones, auditoría de accesibilidad, auditoría de documentación.

Informe completo en [`docs/HARDENING.md`](./docs/HARDENING.md). Cada auditoría es **ejecutable**:
vive como prueba, no como una casilla en un documento que envejece sin que nadie lo note.

- [x] **Regresión**: 319 pruebas en 57 clases; `./gradlew build` verde.
- [x] **Rendimiento** (`PerformanceAuditIT`): 20 000 ciudadanos y 20 000 trámites. Búsqueda, cola,
      historial y conteo por debajo de 400 ms; búsqueda por número por debajo de 50 ms. Lee además
      el **plan de ejecución**, porque un tiempo que pasa por caché no dice nada.
- [x] **Migraciones** (`MigrationAuditTest`): idempotencia, integridad, FK activadas, clave primaria
      en toda tabla, ningún importe en coma flotante, disparadores de auditoría presentes.
- [x] **Accesibilidad y UX** (`AccessibilityAuditTest`): todo control alcanzable por teclado, ninguna
      clave sin traducir, y **ningún mensaje que filtre detalle técnico** al operador.
- [x] **Impresión**, **copias/restauración** y **seguridad**: cubiertas por las pruebas de sus fases,
      incluida la que elimina los disparadores para comprobar que la manipulación sigue viéndose.
- [x] **Documentación**: `onBrokenLinks: "throw"` rompe el CI ante un enlace interno roto.

### Hallazgos corregidos en esta fase

- **30 claves foráneas sin índice.** SQLite indexa las primarias pero nunca las foráneas.
  `V0010__foreign_key_indexes.sql` indexa las doce que la aplicación recorre; las demás son columnas
  de autoría cuya exención queda **registrada en el propio test**, así que una FK nueva sin índice
  ni exención rompe la auditoría.
- La regla de moneda era demasiado estricta: `invoice_line` hereda la moneda de su factura a
  propósito — duplicarla solo crearía dónde discrepar.

## Fase 14 — Release 1.0 ✅

Solo tras pasar el Release Gate de [`RELEASE.md`](./RELEASE.md).

Verificación completa en [`docs/RELEASE-GATE-1.0.md`](./docs/RELEASE-GATE-1.0.md): **18 de 21
checks en verde**, tres marcados ⚠️ porque no se pueden probar honestamente en CI —
impresión sobre hardware físico (dos) y el flujo de Google Drive, que necesita credenciales del
municipio. Fabricarlas para que un test pasara sería justo la funcionalidad falsa que §1.2 prohíbe.

- [x] Release Gate verificado check por check, con la evidencia enlazada.
- [x] `CHANGELOG.md` finalizado; versión `1.0.0`.
- [x] Promoción `experiment → testing → main` con merges `--no-ff` y los cuatro workflows en verde
      en cada paso.
- [x] Tag `v1.0.0` sobre `main`.
- [ ] **Verificación con impresora física antes de la primera instalación real.** Es lo único que
      queda del Release Gate y no se puede hacer sin el hardware del ayuntamiento.
- [ ] Firma del MSI — requiere un certificado del municipio.
- [ ] Protección de ramas en GitHub — requiere acceso de administración al repositorio.

---

## MVP — el bucle municipal completo (🔵 objetivo de la primera release usable)

```text
Ciudadano → Servicio → Trámite → Validación de requisitos → Tasa → Factura → Pago →
Impresión → Documento oficial → Auditoría → Backup
```

Y al menos un proceso **gratuito/no financiero**:

```text
Ciudadano → Solicitud → Asignación → Resolución → Cierre → Auditoría
```

## Escenarios de aceptación end-to-end (🔵)

- **A — Certificación:** crear ciudadano → certificado de vida y residencia → validar requisitos →
  tasa → factura → pago → imprimir → generar certificado → auditoría.
- **B — Construcción:** solicitante → propiedad/proyecto → documentos requeridos → revisión →
  inspección → decisión → tasa → factura → pago → documento de permiso.
- **C — Cementerio:** ciudadano/familia → registro de cementerio → espacio → concesión/permiso →
  tasa → factura → pago → documentos.
- **D — Queja:** ciudadano → solicitud de servicio → asignación → seguimiento → resolución → cierre
  (sin factura salvo que el servicio configurado la exija).
- **E — Documento registrado:** parte → presentación → metadatos de registro → tasa/liquidación →
  factura → pago → registro → copia certificada.
- **F — Recibo angosto:** servicio pagado → recibo compacto → impresora angosta configurada → salida
  física → comportamiento de reimpresión auditado.
- **G — Factura Letter:** servicio pagado → preview Letter → marca institucional → impresora de
  oficina Windows → copia PDF → auditoría.

## Fuera de MVP, en roadmap (⏭️)

Elecciones/soporte electoral, desarrollo económico local, cultura y patrimonio, turismo, deportes y
recreación, eventos comunitarios, bibliotecas, bienestar animal, salud pública, higiene alimentaria,
control de plagas, ruido/molestias, arbolado urbano, protección civil, coordinación de bomberos,
vivienda/apoyo comunitario, aparcamiento y permisos, reserva de instalaciones, subvenciones,
gestión de activos municipales, arrendamientos y concesiones avanzados, portal ciudadano,
notificaciones por email/SMS/WhatsApp, datos abiertos/transparencia, API/nube.
