# Base de datos de SIRMAX

Motor por defecto: **SQLite** embebido (ver [ADR 0003](./docs/adr/0003-sqlite-local-first.md)).
Diagrama entidad-relación inicial: [`docs/domain/erd.md`](./docs/domain/erd.md).

## 1. Principios

- **Local-first.** Una sola base por instalación municipal, en la ruta de datos del usuario.
- **Integridad transaccional.** Claves foráneas siempre activas (`PRAGMA foreign_keys = ON`).
- **Dinero exacto.** Nunca `REAL`/coma flotante para importes. Ver §4.
- **Nada se borra de verdad.** Estados de archivo (`ACTIVE`, `COMPLETED`, `CLOSED`, `ARCHIVED`,
  `VOID`, `CANCELLED`), no `DELETE` sobre registros con valor legal/financiero.
- **Histórico inmutable.** Facturas emitidas/pagadas y eventos de auditoría no se editan desde la UI.
- **Reglas versionadas.** Tasas, requisitos, flujos y roles de aprobación se versionan; los trámites
  antiguos se interpretan con su versión vigente.

## 2. Ubicación del fichero

| Entorno | Ruta |
| --- | --- |
| Desarrollo | `apps/desktop/local-run/sirmax-dev.sqlite` (ignorado por Git) |
| Windows (producción) | `%LOCALAPPDATA%\SIRMAX\data\sirmax.sqlite` (fuera de los binarios; sobrevive a actualizaciones) |

Archivos asociados de SQLite en modo WAL: `*.sqlite-wal`, `*.sqlite-shm` (no comitear).

## 3. Migraciones

- SQL versionado en [`database/migrations/`](./database/migrations/).
- Convención de nombre: `V<NNNN>__<descripcion_snake_case>.sql` (estilo Flyway).
  Ejemplo: `V0001__baseline.sql`.
- Cada migración es **idempotente en intención** y se aplica en una transacción.
- La versión aplicada se registra en la tabla `schema_migrations` (o `flyway_schema_history` si se
  adopta Flyway Community, que soporta SQLite).
- **Pruebas obligatorias:** aplicar toda la cadena sobre (a) base nueva y (b) base de una versión
  anterior con datos de ejemplo; verificar que no hay pérdida de datos ni violaciones de FK.
- **Nunca** editar una migración ya publicada en `testing`/`main`: se añade una nueva.

Runner: un componente de `sirmax-infrastructure` que lee los ficheros ordenados, compara con
`schema_migrations` y aplica los pendientes al arrancar (con opción de "solo validar").

## 4. Representación del dinero

- Tipo de aplicación: `Money` (`sirmax-shared`) = importe entero en **unidad mínima** (centavos) +
  código de moneda ISO 4217.
- Almacenamiento: columnas `*_minor` de tipo `INTEGER` (centavos) y `currency` de tipo `TEXT(3)`.
  Alternativa permitida: `TEXT` con decimal de precisión fija; nunca `REAL`.
- Todos los cálculos (subtotal, descuento, cargo, total, balance, cambio) se hacen en enteros o
  decimales exactos y se redondean con una política explícita y documentada por moneda.

## 5. Convenciones de esquema

- Claves primarias: `id` `TEXT` con UUIDv7 (ordenable) o `INTEGER` autoincremental según la tabla;
  documentar la elección por tabla.
- Marcas de tiempo: `TEXT` en ISO-8601 UTC (`created_at`, `updated_at`), más `*_at` de negocio.
- Enumeraciones: `TEXT` con `CHECK (col IN (...))`.
- Auditoría: tabla `audit_event` append-only (`who`, `when_at`, `action`, `entity_type`,
  `entity_id`, `before_json`, `after_json`, `reason`, `session_id`, `source`).
- Índices: en toda FK, en columnas de búsqueda (documento de identidad, número de factura, código de
  trámite) y en filtros de listas (estado + fecha).

## 6. Numeración de documentos

Secuencias independientes por tipo, seguras ante concurrencia (fila de secuencia bloqueada dentro de
la transacción de emisión). Ejemplos:

```text
FACT-2026-000001      TRM-2026-000001      CERT-RES-2026-000001
PER-URB-2026-000001   REG-2026-000001
```

Configurable: prefijo, serie, año, _padding_, ámbito y reinicio anual. Un número emitido **no** se
reutiliza tras anulación.

## 7. Copias de seguridad e integridad

- Snapshot consistente vía `VACUUM INTO` / API de backup de SQLite (no copiar el fichero en caliente).
- Validación (`PRAGMA integrity_check`), compresión, cifrado opcional y **hash de integridad**.
- Restauración segura: backup de emergencia previo → validar destino → confirmar → restaurar →
  chequeos → registrar en auditoría. Detalle en [`BACKUP.md`](./BACKUP.md).

## 8. Rendimiento

- Modo `WAL`, `PRAGMA synchronous = NORMAL` en operación normal.
- Paginación en todas las listas; sin `SELECT *` sobre tablas grandes en la UI.
- Consultas de reporte separadas y optimizadas (índices dedicados), ejecutadas en hilos de fondo.

## 9. Runner de migraciones

Implementado en `sirmax-infrastructure`:

- `SqlScript.splitStatements` divide un `.sql` en sentencias respetando literales de cadena
  (`''`), comentarios `--` y `/* */`, y el anidamiento `BEGIN`/`END` (cuerpos de trigger).
- Las migraciones autoras viven en `database/migrations/` (§3). Gradle (`stageMigrations`) las
  copia al classpath del módulo como `db/migration/*.sql` + un `index.txt` generado (fiable dentro
  de un jar).
- `MigrationRunner`: aplica cada migración pendiente en su propia transacción, la registra en
  `schema_migrations`, **rechaza versiones fuera de orden** (una versión por debajo de la más alta
  aplicada) y `validate()` **rechaza la deriva de checksum** de una migración ya aplicada.
- `SqliteDatabase` mantiene la única conexión de larga vida y llama a `migrate()` al arrancar.

## 10. Esquema del núcleo (`V0002__core_schema.sql`)

| Tabla | Contenido |
| --- | --- |
| `organization_unit` | La institución que opera SIRMAX (ayuntamiento); país ISO alpha-2 |
| `institution_profile` | 1:1 con `organization_unit`: marca (logo, colores, RNC, pie de factura) |
| `department` | Unidades internas; `UNIQUE(organization_unit_id, code)` |
| `app_user` | Cuentas de operación; `password_hash` + `password_algo`; `status` |
| `role`, `permission` | RBAC; catálogo de 25 permisos + 4 roles de sistema sembrados |
| `user_role`, `role_permission` | Asociaciones N:M |
| `person` | Ciudadanos; `full_name` denormalizado para búsqueda |
| `organization_party` | Partes no-persona (negocios, juntas de vecinos, instituciones) |
| `identification` | Documentos identificativos; `(party_type, party_id)` polimórfico; índice `(id_type, id_number)` |
| `postal_address`, `contact_point` | Direcciones y contactos; mismo dueño polimórfico |
| `app_setting` | Configuración clave/valor JSON + clasificación de datos |
| `audit_event` | Append-only (triggers bloquean UPDATE/DELETE) — creada en `V0001` |

Owner polimórfico `(party_type, party_id)`: SQLite no permite FK entre tablas por valor; la
integridad la garantiza la capa de aplicación y hay `CHECK` sobre `party_type`.
