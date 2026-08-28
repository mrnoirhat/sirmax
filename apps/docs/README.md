# SIRMAX Documentation

Documentation site for SIRMAX, built with **Docusaurus 3** (TypeScript, Mermaid enabled).

## Commands

From the repo root (npm workspaces) or this directory:

```bash
npm run docs:dev      # http://localhost:3001
npm run docs:build    # static build to apps/docs/build
```

Direct:

```bash
cd apps/docs
npm run start
npm run build
npm run typecheck
```

## Structure

Two audiences (master prompt §6 / §71):

- **Operadoras** — `guia-usuario/`, `administracion/`: lenguaje claro, pasos, ejemplos.
- **Desarrollo** — `desarrollo/`: arquitectura, dominio, base de datos, motores, pruebas, releases.

`docs/` pages are Phase 1 stubs that link to the canonical root docs (`ARCHITECTURE.md`, `DATABASE.md`,
`BACKUP.md`, `RELEASE.md`, `docs/adr/`, `docs/domain/`, `docs/ux/`). Real content is written
alongside features in Phases 6 and 12. `onBrokenLinks` and `onBrokenMarkdownLinks` are set to
`throw`, so keep internal links valid.

## Deploy

`main` → production docs, `testing` → preview. Build output: `apps/docs/build`.
