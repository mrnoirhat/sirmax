# database/

SQL migrations and database resources for the SIRMAX local database (**SQLite**, see
[ADR 0003](../docs/adr/0003-sqlite-local-first.md)). Conventions and the initial ERD:
[`DATABASE.md`](../DATABASE.md) · [`docs/domain/erd.md`](../docs/domain/erd.md).

## Layout

```text
database/
└── migrations/
    └── V0001__baseline.sql   # applied first; creates schema_migrations + core pragmas doc
```

## Migration rules

- File name: `V<NNNN>__<snake_case_description>.sql` (Flyway-style, zero-padded, monotonic).
- Each migration runs inside one transaction and is applied by the runner in
  `sirmax-infrastructure` at startup (with a "validate only" mode).
- **Never edit a migration already published to `testing`/`main`.** Add a new one.
- Applied versions are tracked in `schema_migrations` (or `flyway_schema_history` if Flyway
  Community is adopted in Phase 3).
- Every migration is covered by a test that applies the whole chain on (a) a fresh database and
  (b) a database one version behind with sample data.

## Money

Never `REAL`. Amounts are `INTEGER` minor units + a `currency TEXT(3)` column. See
[`DATABASE.md` §4](../DATABASE.md).
