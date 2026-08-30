---
title: "Releases"
sidebar_position: 9
description: "Tres ramas, cuatro checks y una etiqueta que publica."
---

# Releases

## Las tres ramas

```text
experiment  ──▶  testing  ──▶  main
 trabajo         Release Gate   publicado
```

- **`experiment`** — donde se trabaja. Se empuja a diario.
- **`testing`** — lo que se cree listo. Se promueve con `git merge --no-ff`.
- **`main`** — solo lo que pasó el Release Gate. **Exige pull request.**

## Qué protege cada rama

| Rama | Protección |
| --- | --- |
| `main` | PR obligatorio + los cuatro checks + sin `force push` ni borrado |
| `testing` | Sin `force push` ni borrado |
| `experiment` | Sin `force push` ni borrado |

`testing` y `experiment` no exigen checks a propósito: exigirlos bloquearía
también el push directo —GitHub rechaza un commit cuyos checks aún no han
corrido— y obligaría a abrir un PR para cada promoción, que es justo lo que el
merge `--no-ff` resuelve.

Las tres tienen `enforce_admins`: una protección que el dueño se salta no protege
de la prisa del dueño.

## Los cuatro checks

**Desktop** (`Build & test (JDK 25)`), **Landing**
(`Lint, typecheck & build`), **Docs** (`Build Docusaurus`) y **Security**
(`Secret scan (gitleaks)`).

Corren en cada push a las ramas permanentes y en cada PR. Los nombres tienen que
coincidir **exactamente** con los contextos requeridos en `main`.

## Publicar una versión

1. Subir la versión en `apps/desktop/build.gradle.kts` y en
   `apps/landing/lib/site.ts`.
2. Escribir el `CHANGELOG.md`.
3. Promover hasta `main` por el flujo.
4. Etiquetar:

```bash
git tag -a v1.0.2 -m "SIRMAX 1.0.2"
git push origin v1.0.2
```

La etiqueta dispara `release.yml`: compila el MSI en un runner con WiX, verifica
que el runtime quedó dentro, calcula las sumas SHA-256 y publica los artefactos.

## Los artefactos

| Archivo | Qué es |
| --- | --- |
| `SIRMAX-x.y.z.msi` | Instalador con runtime incluido |
| `SIRMAX-x.y.z-windows.zip` | Portable |
| `SHA256SUMS.txt` | Sumas de comprobación |

`verifyReleaseArtifacts` comprueba que existen, que llevan el ejecutable y el
runtime, y que superan un tamaño mínimo. Ese umbral no es arbitrario: por debajo
significa que jlink no llegó a incluirse, lo que produce un instalador que
instala bien y luego no arranca.

## Firma

Ver [SIGNING.md](https://github.com/mrnoirhat/sirmax/blob/main/docs/SIGNING.md). Se salta sin
fallar cuando no hay certificado, porque CI no tiene ninguno.

## Versionado

[SemVer](https://semver.org/lang/es/). En una aplicación de escritorio, *mayor*
se reserva para lo que obligue a intervenir al instalar —una migración
irreversible, un cambio de formato de las copias.
