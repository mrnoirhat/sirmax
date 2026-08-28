# Roadmap de SIRMAX

Estado de la construcción por fases. Este documento es la **fuente de verdad del progreso**.

Leyenda: ✅ completada · 🟡 en curso · ⚪ pendiente · 🔵 planificada para 1.0 · ⏭️ post-1.0

_Última actualización: 2026-08-28 — rama `experiment`. Fases 0–3 ✅; Fase 4 en curso._

---

## Resumen

| Fase | Título | Estado |
| ---: | --- | :---: |
| 0 | Discovery, auditoría de repo y arquitectura | ✅ |
| 1 | Fundación del repositorio | ✅ |
| 2 | Shell de escritorio y Design System | ✅ |
| 3 | Dominio central y base de datos | ✅ |
| 4 | Motor configurable de servicios | 🟡 |
| 5 | Ciudadano y experiencia de front-office | ⚪ |
| 6 | Facturación, pagos y caja | ⚪ |
| 7 | Módulos municipales especializados | ⚪ |
| 8 | Documentos, PDF e impresión | ⚪ |
| 9 | Backup, recuperación y Google Drive | ⚪ |
| 10 | Endurecimiento de seguridad, auditoría y fiabilidad | ⚪ |
| 11 | Empaquetado para Windows | ⚪ |
| 12 | Productivización de landing + docs | ⚪ |
| 13 | Hardening | ⚪ |
| 14 | Release 1.0 | ⚪ |

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

## Fase 4 — Motor configurable de servicios 🟡

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
- [ ] Motor de requisitos: evaluación contra un trámite (checklist "faltan N requisitos").
- [ ] Motor de flujo de trabajo tipado (ADR 0007): `WorkflowDefinition`/`Step`/transiciones,
      evaluador de expresiones restringido.
- [x] Motor de tasas tipado (ADR 0008): `FeeRule` (inmutable, con vigencia), `FeeRuleType`,
      `ChargeType`, `FeeInput`, `FeeCalculator` → `Charge`/`ChargeLine`. Solo dominio; sin usar en
      la facturación todavía (Fase 6).
- [ ] Form schema tipado (campos dinámicos).
- [ ] Catálogo semilla editable (plantillas dominicanas, master prompt §54).
- [ ] UI de configuración de servicios (puede solaparse con Fase 5).

## Fase 5 — Ciudadano y front-office ⚪

Búsqueda de ciudadano, ficha maestra, detección de duplicados, asistente de nuevo trámite, checklist
de requisitos, navegación service-first, colas/worklists, detalle de caso, historial del ciudadano.

## Fase 6 — Facturación, pagos y caja ⚪

Cargo/liquidación, entidad factura, numeración, líneas, descuentos/cargos, métodos de pago, pago
parcial, recibos, sesión de caja, conciliación, reembolsos/anulaciones/ajustes, auditoría de
facturación.

## Fase 7 — Módulos municipales especializados ⚪

Prioridad: 1) Registro Civil / Registro de Documentos / Conservaduría · 2) Certificaciones y cartas
oficiales · 3) Planeamiento Urbano / Construcción · 4) Propiedad/Catastro · 5) Cementerios ·
6) Mercados y espacios comerciales · 7) Negocios/Publicidad/Permisos · 8) Espacio público/Movilidad ·
9) Residuos/Solicitudes de servicio · 10) Casos comunitarios/sociales.

## Fase 8 — Documentos, PDF e impresión ⚪

Plantillas de documentos oficiales, plantilla de factura Letter, plantilla de recibo/factura
angosta, preview, impresión Windows, perfiles de impresora, generación de PDF, marca institucional,
QR/verificación, reimpresión auditada.

## Fase 9 — Backup, recuperación y Google Drive ⚪

Backups locales, historial, validación, compresión, cifrado, hash de integridad, restauración
segura, Google OAuth, carpeta de Drive, programación automática.

## Fase 10 — Seguridad, auditoría y fiabilidad ⚪

Hashing de contraseñas, seguridad de sesión, permisos, integridad de auditoría, log seguro,
validación de ficheros, manejo de secretos, chequeos de dependencias/seguridad, pruebas de
recuperación.

## Fase 11 — Empaquetado para Windows ⚪

Instalador Windows, runtime empaquetado, accesos directos, instalación limpia, desinstalación,
información de versión, verificación de artefactos de release.

## Fase 12 — Productivización de landing + docs ⚪

Landing lista para Vercel, SEO, capturas, CTAs de GitHub, despliegue de Docusaurus, enlaces cruzados,
alineación release/documentación.

## Fase 13 — Hardening ⚪

Regresión, auditoría de UX, auditoría de rendimiento, auditoría de impresión, auditoría de
backup/restore, auditoría de migraciones, auditoría de accesibilidad, auditoría de documentación.

## Fase 14 — Release 1.0 ⚪

Solo tras pasar el Release Gate de [`RELEASE.md`](./RELEASE.md).

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
