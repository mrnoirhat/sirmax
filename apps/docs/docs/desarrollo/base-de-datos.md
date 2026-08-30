---
title: "Base de datos"
sidebar_position: 3
description: "SQLite, 47 tablas, diez migraciones y un guardia contra la deriva."
---

# Base de datos

**SQLite**, un archivo en `%LOCALAPPDATA%\SIRMAX\data\sirmax.sqlite`. Es
[ADR 0003](https://github.com/mrnoirhat/sirmax/blob/main/docs/adr/0003-sqlite-local-first.md): sin
servidor que administrar, y la copia de seguridad es copiar un archivo.

## Migraciones

En [`database/migrations`](https://github.com/mrnoirhat/sirmax/blob/main/database/migrations),
numeradas y aplicadas en orden al arrancar:

| | |
| --- | --- |
| `V0001` | baseline y tabla de migraciones |
| `V0002` | esquema base: personas, organización, usuarios |
| `V0003` | motor de servicios |
| `V0004` | trámites |
| `V0005` | facturación |
| `V0006` | módulos municipales |
| `V0007` | documentos |
| `V0008` | copias de seguridad |
| `V0009` | endurecimiento de seguridad |
| `V0010` | índices de claves foráneas |

47 tablas en total.

### Nunca se edita una migración publicada

Cada una se registra con el SHA-256 de su contenido. Al arrancar se comprueban
las ya aplicadas, y si una cambió el programa **se niega a continuar**:

```text
Migration V0008 has changed since it was applied (checksum mismatch).
```

Suena drástico y es lo correcto: aplicar media migración modificada sobre una base
existente deja un esquema que no corresponde a ninguna versión del código. Para
cambiar algo se añade `V0011`.

## Convenciones

- Identificadores **UUIDv7** en `TEXT`: ordenables por tiempo, sin coordinación.
- Dinero en dos columnas: `*_minor INTEGER` y `currency TEXT(3)`.
- Fechas en `TEXT` ISO-8601 UTC.
- Nombres en `snake_case`, tablas en singular.
- `archive_status` en lugar de borrar filas.

## Índices

`V0010` indexa las claves foráneas que la aplicación recorre. No todas: las
columnas de autoría (`created_by`) no se consultan por sí solas, y un índice que
nadie usa solo ralentiza las escrituras.

`MigrationAuditTest` comprueba que no queden foráneas sin índice **salvo** las
listadas explícitamente como excepción, para que la decisión sea consciente.

## Búsqueda sin tildes

`person.search_name` guarda el nombre normalizado —sin tildes, en minúsculas— y
la búsqueda va contra esa columna. El `LIKE` de SQLite solo ignora mayúsculas en
ASCII, así que sin ella `pena` nunca encontraría `Peña`.

La normalización está en `shared.text.Normalization`, y la usan tanto el
adaptador SQLite como el doble en memoria: si divergieran, los tests pasarían
mientras la aplicación falla.
