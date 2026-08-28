# 0012 — Flujo de release con tres ramas permanentes

- **Estado:** Aceptado
- **Fecha:** 2026-08-27
- **Decisores:** Ingeniería principal SIRMAX
- **Fase relacionada:** todas

## Contexto

El master prompt define un modelo de Git **no negociable**: exactamente tres ramas permanentes
compartidas y una promoción estricta hacia producción.

## Decisión

Tres ramas permanentes:

```text
feature/*  ─▶  experiment  ─▶  testing  ─▶  main
```

- **`experiment`** — desarrollo activo y experimentación controlada. Puede ser inestable. Toda rama
  de trabajo (`feature/*`, `fix/*`, `refactor/*`, `ux/*`, `docs/*`, `chore/*`, `perf/*`, `ci/*`)
  parte de aquí.
- **`testing`** — integración y QA: build, pruebas, migraciones, empaquetado Windows, pruebas de
  impresión, backup/restore, seguridad, build de landing y docs. Release candidate.
- **`main`** — solo producción estable. Sin desarrollo directo. Origen de releases, despliegue de
  producción de la landing y de la documentación.

Reglas:

- **Nunca** `feature/* → main`. **Nunca** `experiment → main` salvo política de hotfix documentada.
- La promoción a `main` es **solo** vía `testing`.
- `testing` y `main` protegidas: PR obligatorio + CI en verde + revisión. `experiment` corre CI.
- Commits: Conventional Commits. Versionado semántico; _tags_ sobre `main`.
- Los criterios de promoción (`experiment→testing` y el Release Gate `testing→main`) viven en
  `RELEASE.md`.

## Consecuencias

**Positivas**
- `main` siempre desplegable; separación clara entre experimentar, estabilizar y publicar.
- Compatible con despliegues Vercel por rama (`main`→prod, `testing`→preview).

**Negativas / costes**
- Tres ramas de larga vida ⇒ hay que hacer merges de promoción con disciplina y evitar divergencia.
- Un hotfix urgente necesita política explícita para no saltarse `testing`.

## Alternativas consideradas

- **GitHub Flow (solo `main` + features)** — más simple, pero el master prompt exige el modelo de
  tres ramas y una fase de estabilización explícita.
- **Git Flow completo (con `develop`, `release/*`, `hotfix/*`)** — más ceremonioso; el modelo de tres
  ramas es la versión acordada.

## Referencias

- Master prompt §0 "Mandatory Git promotion model", §4, §61–67.
