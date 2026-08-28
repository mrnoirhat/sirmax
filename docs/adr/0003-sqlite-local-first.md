# 0003 — SQLite como base de datos embebida local-first

- **Estado:** Aceptado
- **Fecha:** 2026-08-27
- **Decisores:** Ingeniería principal SIRMAX
- **Fase relacionada:** 1, 3

## Contexto

SIRMAX es local-first: cada instalación municipal corre en un equipo Windows, debe funcionar sin
internet y debe sentirse inmediata. El master prompt fija SQLite salvo que una decisión de
arquitectura documente una opción embebida mejor. Requisitos: migraciones, integridad transaccional,
claves foráneas, índices, representación monetaria segura, consistencia de backup, versión de esquema
y pruebas de migración.

## Decisión

La base de datos por defecto es **SQLite**, un único fichero por instalación, ubicado en la ruta de
datos del usuario (`%LOCALAPPDATA%\SIRMAX\data\sirmax.sqlite`), **fuera** de los binarios para
sobrevivir a actualizaciones.

Configuración: `PRAGMA foreign_keys = ON`, modo `WAL`, `synchronous = NORMAL` en operación normal.
Acceso vía JDBC (`org.xerial:sqlite-jdbc`). Backups con `VACUUM INTO` o la API de backup de SQLite,
nunca copia en caliente. Dinero en columnas `INTEGER` (unidad mínima) + `currency TEXT(3)`; nunca
`REAL`.

Migraciones versionadas en `database/migrations/` (`V<NNNN>__<desc>.sql`), aplicadas por un runner de
`sirmax-infrastructure` y registradas en `schema_migrations`. Se evalúa Flyway Community (soporta
SQLite) frente a un runner propio mínimo; la elección definitiva se registrará si difiere.

## Consecuencias

**Positivas**
- Cero administración, cero servidor, arranque instantáneo, backup = copiar un artefacto validado.
- Transacciones ACID, claves foráneas, índices, `integrity_check`.
- Portable y con dominio público (sin fricción de licencia).

**Negativas / costes**
- Concurrencia de escritura limitada (un escritor). Aceptable para un puesto/varios puestos de
  mostrador locales; se mitiga con transacciones cortas y colas de trabajo.
- Sin tipos ricos (fechas, decimales) — se maneja con convenciones (`DATABASE.md`).
- Escalar a multi-sede requerirá la futura API/servidor (fuera de 1.0).

**A vigilar**
- Si un municipio grande necesita concurrencia alta antes de la API, revisar (p. ej. servidor
  opcional Postgres detrás del mismo `domain`/`application`).

## Alternativas consideradas

- **H2 / HSQLDB embebidas** — buenas en JVM, pero SQLite tiene mejor tooling de inspección para
  operadoras/soporte y formato de fichero universal.
- **DuckDB** — orientada a analítica, no a OLTP transaccional de mostrador.
- **Postgres embebido** — demasiado pesado para local-first en 1.0.

## Referencias

- Master prompt §2.3 "Database", §41–42 "Backup", §45 "Performance".
