# Mapa de módulos de SIRMAX

Relaciona los **módulos Gradle** del escritorio (capas, ver [ADR 0005](../adr/0005-modular-domain-architecture.md))
con las **áreas funcionales** del producto y con las fases del [`ROADMAP.md`](../../ROADMAP.md).

## 1. Módulos Gradle (`apps/desktop/`)

```text
sirmax-shared          Money · Result · IDs · errores base · claves i18n · utilidades sin estado
sirmax-domain          entidades · agregados · invariantes · servicios de dominio (Java puro)
sirmax-application     casos de uso · orquestación de transacciones · PUERTOS (interfaces)
sirmax-infrastructure  adaptadores: SQLite/JDBC · migraciones · ficheros · PDF · impresión Windows · Google Drive · cifrado · secretos
sirmax-ui              JavaFX: shell · navegación · Design System · vistas · view-models
sirmax-app             composition root · main · wiring manual · configuración de jpackage
```

Dependencias: `ui → application → domain → shared`; `infrastructure → {application, domain, shared}`;
`app → todos`. Verificado con ArchUnit.

## 2. Áreas funcionales → dónde vive cada pieza

| Área funcional | `domain` | `application` (casos de uso + puertos) | `infrastructure` (adaptadores) | `ui` |
| --- | --- | --- | --- | --- |
| **Identidad** (personas, organizaciones, direcciones) | `Person`, `Organization`, `Address`, `Identification` | `RegisterPerson`, `FindCitizen`, `DetectDuplicates` · `PersonRepository` | `SqlitePersonRepository` | Búsqueda de ciudadano, ficha maestra |
| **Organización** (institución, departamentos, usuarios, roles) | `OrganizationUnit`, `Department`, `Role`, `Permission` | `Authenticate`, `Authorize`, `ManageUsers` · `UserRepository`, `PasswordHasher`, `SessionStore` | `SqliteUserRepository`, `Argon2PasswordHasher` | Configuración > Usuarios/Roles |
| **Servicios** (catálogo, definiciones, requisitos, workflow, tasas) | `ServiceDefinition`, `RequirementDef`, `WorkflowStepDef`, `FeeRule` | `PublishServiceVersion`, `EvaluateRequirements`, `AdvanceWorkflow` · `ServiceRepository` | `SqliteServiceRepository`, `JsonSchemaValidator` | Catálogo de servicios, editor de servicio |
| **Trámites** (expedientes, tareas, decisiones, inspecciones, SLA) | `Procedure`, `Decision`, `Inspection`, `Sla` | `OpenProcedure`, `RecordDecision`, `ScheduleInspection` · `ProcedureRepository`, `Clock` | `SqliteProcedureRepository` | Asistente de trámite, detalle de caso, colas |
| **Documentos** (adjuntos, registrados, plantillas, numeración) | `AttachedDocument`, `RegisteredDocument`, `NumberingSequence`, `DocumentTemplate` | `RegisterDocument`, `AllocateNumber`, `RenderDocument` · `TemplateEngine`, `NumberAllocator`, `FileStore` | `HandlebarsTemplateEngine`, `FilesystemFileStore`, `SqliteNumberAllocator` | Registro de documentos, plantillas |
| **Finanzas** (cargos, facturas, pagos, caja, reembolsos) | `Charge`, `Invoice`, `Payment`, `CashSession`, `Refund`, `Money` | `IssueInvoice`, `RegisterPayment`, `VoidInvoice`, `OpenCashSession`, `Reconcile` · `InvoiceRepository`, `SequencePort` | `SqliteInvoiceRepository` | Mostrador de cobro, caja, recibo |
| **Impresión y PDF** (Letter, angosta, perfiles, reimpresión) | `PrintModel`, `PrinterProfile`, `PrintJob` | `PrintInvoice`, `Reprint`, `ExportPdf` · `PrinterPort`, `PdfRenderer` | `WindowsPrinterAdapter`, `PdfBoxRenderer` (o lib compatible) | Preview de impresión, selector de impresora |
| **Configuración / Marca** (institución, reglas versionadas, país) | `InstitutionProfile`, `BusinessRuleVersion`, `CountryAdapter` | `UpdateBranding`, `VersionRule` · `SettingsRepository` | `SqliteSettingsRepository`, `dominican-republic` adapter | Configuración > Institución |
| **Seguridad** (sesión, permisos, secretos) | `Session`, `AccessPolicy` | `Authorize`, `LockSession` · `SecretStore` | `DpapiSecretStore` / `CredentialManagerSecretStore` | Login, bloqueo, timeout |
| **Auditoría** | `AuditEvent` (inmutable) | `RecordAuditEvent` · `AuditSink` | `SqliteAuditSink` (append-only) | Visor de auditoría (solo lectura) |
| **Backup / Restauración** | `BackupArtifact`, `IntegrityHash` | `CreateBackup`, `RestoreBackup`, `ScheduleBackup` · `BackupTarget`, `CloudBackupPort` | `SqliteSnapshotter`, `GoogleDriveBackupTarget`, `AesGcmCipher` | Backup: crear/historial/restaurar |
| **Reportes** | (vistas de lectura) | `RunReport` · `ReportQueryPort` | `SqliteReportQueries` | Reportes financieros/servicio/operativos |
| **Búsqueda global** | — | `GlobalSearch` · `SearchIndexPort` | `SqliteFtsSearchIndex` (FTS5) | Barra de búsqueda global |
| **i18n** | claves en `shared` | `MessageResolver` | `ResourceBundleMessages` (es → en/fr) | Todos los textos de UI |

## 3. Módulos especializados (Fase 7) — sobre el mismo núcleo

Cada uno aporta sus entidades en `domain` (subpaquete propio), sus casos de uso en `application`, sus
adaptadores en `infrastructure` y sus vistas en `ui`, **reutilizando** trámite, documentos, finanzas
y auditoría del núcleo:

```text
domain/
  org.sirmax.domain.registry      Registro de Documentos / Conservaduría
  org.sirmax.domain.certificate   Certificaciones y cartas
  org.sirmax.domain.urban         Planeamiento Urbano / Construcción
  org.sirmax.domain.cadastre      Propiedad / Catastro
  org.sirmax.domain.cemetery      Cementerios
  org.sirmax.domain.market        Mercados y espacios comerciales
  org.sirmax.domain.permit        Negocios / Publicidad / Permisos
  org.sirmax.domain.mobility      Espacio público / Movilidad
  org.sirmax.domain.ops           Solicitudes / Quejas / Residuos / Órdenes de trabajo
  org.sirmax.domain.community     Casos comunitarios / sociales
```

## 4. Web y otros

| Ruta | Contenido | Toolchain | Fase |
| --- | --- | --- | --- |
| `apps/landing` | Sitio público (Next.js/React/TS), CTAs de GitHub/descarga/docs | Node · npm workspace | 1, 12 |
| `apps/docs` | Documentación (Docusaurus): operadoras + desarrollo | Node · npm workspace | 1, 6, 12 |
| `backend/` | Frontera de futura API/nube; **sin servicio** en 1.0 | Java (reutilizaría `domain`+`application`) | post-1.0 |
| `database/` | Migraciones SQL (`V####__*.sql`) y recursos | SQL | 3+ |
| `scripts/` | Utilidades de build/release/mantenimiento | Bash/PowerShell | 1+ |

## 5. Prueba de arquitectura (obligatoria)

Un módulo/paquete de pruebas comprueba con ArchUnit:

- `domain` no importa `javafx.*`, `java.sql.*`, `org.sirmax.infrastructure.*`, ni clases de red.
- `application` no importa `javafx.*` ni JDBC.
- `ui` no importa `java.sql.*`.
- Los puertos se declaran en `application`; sus implementaciones solo en `infrastructure`.
- Ningún texto de usuario literal en `domain`/`application` (se usan claves i18n).
