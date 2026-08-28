# 0011 — Monorepo único (escritorio + landing + docs)

- **Estado:** Aceptado
- **Fecha:** 2026-08-27
- **Decisores:** Ingeniería principal SIRMAX
- **Fase relacionada:** 1

## Contexto

El master prompt exige **un solo repositorio GitHub** que contenga la aplicación de escritorio, la
landing, la documentación, el código de backend/dominio, los recursos de base de datos, CI/CD y la
documentación de proyecto.

## Decisión

Un **monorepo** con esta forma:

```text
sirmax/
├── apps/
│   ├── desktop/   (Gradle · Java · JavaFX)
│   ├── landing/   (Next.js · workspace npm)
│   └── docs/      (Docusaurus · workspace npm)
├── backend/       (frontera de futura API; sin servicio en 1.0)
├── database/      (migraciones SQL y recursos)
├── scripts/       (build/release/mantenimiento)
├── docs/          (adr/, domain/, ux/, build-plan)
└── .github/       (workflows, ISSUE_TEMPLATE, PULL_REQUEST_TEMPLATE)
```

- La parte web usa **npm workspaces** (`apps/landing`, `apps/docs`) con `package-lock.json` en la
  raíz.
- La parte Java usa Gradle en `apps/desktop/` (build independiente).
- CI con **workflows separados y con filtros de ruta** (`desktop.yml`, `landing.yml`, `docs.yml`,
  `security.yml`): un cambio en la landing no dispara el build de Java y viceversa.
- Vercel despliega la landing desde este monorepo (`apps/landing` como _root directory_).

## Consecuencias

**Positivas**
- Una sola fuente de verdad; issues, PRs, versiones y roadmap unificados.
- Cambios transversales (p. ej. un término de dominio) en un solo PR.
- Onboarding simple: un `git clone`.

**Negativas / costes**
- Toolchains mixtas (JDK + Node) en el repo y en CI.
- Hay que cuidar los filtros de ruta para no ejecutar CI innecesario.
- El historial de Git mezcla áreas (se mitiga con ámbitos en Conventional Commits).

## Alternativas consideradas

- **Multi-repo** (uno por app) — contrario al master prompt; fricción de sincronización y releases.
- **Monorepo con Nx/Turborepo global** — innecesario: las dos mitades (JVM y Node) tienen
  herramientas nativas propias; añadir un orquestador global no aporta lo suficiente en 1.0.

## Referencias

- Master prompt §3, §68, §69.
