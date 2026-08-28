# Arquitectura de SIRMAX

> Documento vivo. Las decisiones puntuales se registran como ADR en [`docs/adr/`](./docs/adr/).
> El modelo de dominio detallado vive en [`docs/domain/`](./docs/domain/).

## 1. Idea central

> **SIRMAX no es una colección de formularios. Es una plataforma configurable de servicios y
> registros municipales con una columna vertebral compartida de trámite, documento, finanzas,
> flujo de trabajo, auditoría y reportes.**

Cada módulo especializado (cementerios, mercados, planeamiento, catastro, movilidad, quejas…) se
conecta a esa misma columna en lugar de reimplementarla.

## 2. Forma del sistema

```text
┌─────────────────────────────────────────────────────────────────────┐
│  apps/desktop  (Windows · Java 25 · JavaFX)   ← producto principal   │
│                                                                     │
│   UI / Presentación (JavaFX)                                         │
│        │  (solo llama a casos de uso; nunca SQL)                     │
│   Aplicación / Casos de uso                                          │
│        │  (orquesta dominio + puertos)                              │
│   Dominio  (Java puro, sin frameworks)                               │
│        │                                                            │
│   Infraestructura (adaptadores: SQLite, ficheros, impresión, Drive)  │
│        │                                                            │
│   SQLite · Sistema de ficheros · Integraciones externas             │
└─────────────────────────────────────────────────────────────────────┘

apps/landing (Next.js/Vercel)      apps/docs (Docusaurus)      backend/ (futura API)
        └── independientes del escritorio; no requieren que la app esté online
```

## 3. Capas (apps/desktop)

| Capa | Módulo Gradle | Depende de | Reglas |
| --- | --- | --- | --- |
| Presentación | `sirmax-ui` | `application`, `shared` | JavaFX aquí y solo aquí. No accede a SQL. No contiene reglas de negocio. |
| Aplicación | `sirmax-application` | `domain`, `shared` | Casos de uso, orquestación de transacciones, definición de **puertos** (interfaces). Sin JavaFX. Sin JDBC. |
| Dominio | `sirmax-domain` | `shared` | Entidades, agregados, invariantes, servicios de dominio. **Java puro.** Sin frameworks, sin I/O, sin texto de usuario. |
| Infraestructura | `sirmax-infrastructure` | `application`, `domain`, `shared` | **Adaptadores** que implementan los puertos: SQLite/JDBC, migraciones, ficheros, PDF, impresión Windows, cliente Google Drive, cifrado. |
| Composición | `sirmax-app` | todos | _Composition root_: arranque, inyección manual de dependencias, `main`, configuración de `jpackage`. |
| Transversal | `sirmax-shared` | — | `Money`, `Result`, tipos de identidad, errores base, claves i18n, utilidades sin estado. |

Dependencias permitidas (flecha = "puede usar"):

```text
ui ──▶ application ──▶ domain ──▶ shared
             ▲            ▲          ▲
infrastructure ───────────┴──────────┘
app ──▶ (todos)
```

**Prohibido:** `domain → javafx`, `domain → jdbc`, `ui → jdbc`, `application → javafx`,
`domain → infrastructure`.

Estas reglas se verifican con una prueba de arquitectura (ArchUnit) en el módulo
`sirmax-architecture-tests`.

### 3.1 Capa de UI (`sirmax-ui`, Fase 2)

JavaFX **programático, sin FXML** (ver [ADR 0013](./docs/adr/0013-ui-programmatic-javafx.md)).

```text
org.sirmax.ui.i18n         Messages (ResourceBundle; español base) — nada de texto literal
org.sirmax.ui.designsystem Styles, Typography, Buttons, Cards, Banner, StatefulContent,
                           ToastHost, Dialogs, FormField, DataTable  +  theme/sirmax.css
org.sirmax.ui.nav          RouteKey, NavItem, Navigator (sin JavaFX), ShellNavigator
org.sirmax.ui.theme        Theme, ThemeManager (claro/oscuro; aplica `sirmax-dark` a la raíz)
org.sirmax.ui.view         SirmaxView, HomeView (service-first), DashboardView,
                           GlobalSearchView, PlaceholderView, StyleGuideView (galería, dev)
org.sirmax.ui.shell        ShellView (menú + top bar + task nav + content host + toasts),
                           AppMenuBar (Archivo/Ver/Ayuda), KeyboardShortcuts (Ctrl+K)
org.sirmax.ui              SirmaxApplication (Scene + tema + atajos)
```

- **Navegación** orientada a tareas (§35): la pantalla de inicio pregunta "¿Qué necesitas hacer?".
  `Navigator`/`ShellNavigator` son Java plano y testeables sin toolkit.
- **Estados** loading / empty / error / success como componente reutilizable (`StatefulContent`); el
  error muestra mensaje amable + reintento y esconde el detalle técnico.
- **Tema** con _looked-up colors_ (`-sirmax-*`); el color nunca es el único indicador semántico.
- **Pruebas:** lógica con JUnit normal; `ShellViewSmokeTest` arranca el toolkit (`Platform.startup`)
  y valida el shell en el hilo de JavaFX (runner Windows de CI).

## 4. Módulos transversales (conceptuales)

```text
security         autenticación, sesión, RBAC, hashing, secretos locales
logging          log estructurado, sin datos sensibles en claro
configuration    perfil de institución, ajustes, catálogos, versionado de reglas
reporting        consultas de lectura optimizadas, exportación
backup           snapshot SQLite, validación, compresión, cifrado, hash, Drive
printing         perfiles de impresora, preview, Windows printing, dos plantillas
i18n             español primero; inglés/francés/país preparados
audit            eventos inmutables (quién, cuándo, qué, antes/después, motivo, sesión)
```

## 5. La columna vertebral del dominio

```text
Ciudadano / Entidad
      ↓
Solicitud / Caso
      ↓
Trámite / Expediente ── Requisitos ── Documentos ── Revisión / Inspección
      ↓
Decisión / Aprobación
      ↓
Tasa / Liquidación  →  Factura  →  Pago  →  Recibo / Documento oficial
      ↓
Entrega ── Auditoría ── Archivo
```

Distinciones que la arquitectura mantiene explícitas:

- No todo trámite se paga (`GRATUITO`, `CON_TASA`, `TASA_CONDICIONAL`, `PAGO_EXTERNO`).
- **Tasa ≠ Factura ≠ Pago ≠ Movimiento de caja ≠ Recibo.**
- "Documento adjunto a un trámite" ≠ "documento oficialmente registrado" (Registro de Documentos).
- No todo servicio pertenece a un único departamento.

## 6. Motores configurables (pragmáticos, no genéricos)

| Motor | Qué hace | Anti-objetivo |
| --- | --- | --- |
| **Definición de servicios** | Catálogo + metadatos: requisitos, formulario, flujo, tasa, plantillas, SLA, numeración, validez | No una lista plana de servicios quemados |
| **Requisitos** | Declarativos por servicio y etapa; `required`, `conditional`, `validation` | — |
| **Flujo de trabajo** | Pasos secuenciales/ramificados, aprobaciones, devolución, reasignación, SLA, checkpoints de pago/documento | No un motor tan abstracto que nadie lo mantenga |
| **Tasas** | Fijo, cantidad × tarifa, por área, duración, categoría, ubicación, tramos, periódico; con fecha de vigencia y **versionado** | Nunca reescribir reglas históricas |
| **Numeración** | Secuencias independientes por tipo de documento; únicas, seguras ante concurrencia, sin reutilización | — |
| **Plantillas/Documentos** | Marca institucional, variables, numeración, QR/verificación | Nunca quemar el nombre/logo de un municipio |

Cualquier regla que afecte dinero, elegibilidad o flujo legal/administrativo es **versionable**: los
trámites antiguos se interpretan con la versión de regla vigente en su momento.

## 7. Facturación e impresión (núcleo de primera clase)

Flujo de mostrador sin salir de la aplicación:

```text
Servicio → Cargo/Tasa → Factura → Pago → Recibo/Factura pagada → Imprimir / Guardar PDF → Trámite + Auditoría
```

- Entidad `Factura` dedicada con ciclo `DRAFT → ISSUED → PARTIALLY_PAID → PAID → VOIDED → REFUNDED`.
- **Dinero:** representación decimal precisa (`Money` en `sirmax-shared`). **Nunca coma flotante.**
- **Snapshot histórico:** al emitir se preserva identidad de institución y cliente, líneas, precios,
  totales y moneda. Un cambio futuro de logo/dirección no reescribe facturas antiguas.
- **Dos plantillas de impresión independientes:**
  - **Modelo A** — angosta / mostrador (58 mm, 80 mm, ancho configurable), legible en impresora de
    impacto monocroma; **no** es una Letter encogida.
  - **Modelo B** — oficina, **US Letter 8.5 × 11"**; arquitectura de plantillas lista para A4 sin
    tocar el dominio de facturación.
- **Reimpresión** autorizada: no crea número nuevo ni duplica el pago; se audita; se puede marcar
  `COPIA/REIMPRESIÓN`.
- Integración con impresión de Windows: perfiles de impresora, selección, preview (Letter), PDF real
  (no captura de pantalla).

## 8. Persistencia

- **SQLite** embebido por defecto (ver [ADR 0003](./docs/adr/0003-sqlite-local-first.md)).
- Claves foráneas activas, transacciones, índices apropiados, `WAL` para lectura concurrente.
- **Migraciones** versionadas y probadas sobre base nueva y actualizada. SQL en
  [`database/migrations/`](./database/migrations/). Detalle en [`DATABASE.md`](./DATABASE.md).
- La base de datos del operador se guarda **fuera** del directorio de binarios de la aplicación, en
  una ruta de datos de usuario que sobrevive a las actualizaciones.

## 9. Seguridad (resumen)

Autenticación de usuario · hashing con sal · expiración/bloqueo de sesión · RBAC · protección de
datos locales · log seguro · cifrado de backups · almacenamiento seguro de secretos (evaluando
Credential Manager / DPAPI) · sin secretos en Git · validación de ficheros. Detalle en
[`SECURITY.md`](./SECURITY.md).

## 10. Rendimiento

Local-first ⇒ debe sentirse inmediato. Sin cargar tablas completas en memoria; paginación, índices,
_lazy loading_, cachés acotadas; trabajo pesado (backup, sync Drive, PDF, exportes, chequeos de
integridad) en hilos de fondo, nunca en el hilo de UI.

## 11. Preparación para nube/API futura

`domain` y `application` no dependen de JavaFX ni de SQLite directamente. Una futura API HTTP
(`backend/`) podría reutilizar `domain` + `application` sustituyendo adaptadores de infraestructura,
sin reescribir las reglas de negocio. La primera release **no** requiere servidor.

## 12. Frontera por país

```text
core/     persona, trámite, servicio, documento, facturación, pago, flujo   (neutral)
country/  dominican-republic/  identidad, moneda, reglas municipales/documentales/tasas
future/   otros países
```

Conceptos internos neutrales: _Local Government Organization, Municipality, Department, Service,
Procedure_. La UI en español puede decir "Ayuntamiento" cuando el país configurado es RD.

## 13. Web y documentación

- **`apps/landing`** — Next.js + TypeScript, desplegable en Vercel desde el monorepo. `main` →
  producción; `testing` → preview. No depende del escritorio.
- **`apps/docs`** — Docusaurus; dos audiencias (operadoras y personas desarrolladoras); diagramas
  Mermaid donde aporten.
- **`backend/`** — por ahora solo una frontera documentada; sin servicio en ejecución para 1.0.
