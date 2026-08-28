# Changelog

Todos los cambios notables de SIRMAX se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa
[Versionado Semántico](https://semver.org/lang/es/).

## [Unreleased]

### Added
- **Fase 2 (en curso) — Shell de escritorio y Design System.**
  - i18n de UI (`Messages` + `messages.properties`, español base); sin texto literal en código.
  - Tema `sirmax.css` con tokens de color/tipografía/espaciado (_looked-up colors_).
  - Design System: `Styles`, `Typography`, `Buttons`, `Cards`, `Banner`, `StatefulContent`
    (loading/empty/error/success), `ToastHost`, `Dialogs`, `FormField`, `DataTable`.
  - Navegación service-first: `RouteKey`, `NavItem`, `Navigator`/`ShellNavigator` (sin JavaFX),
    `ShellView` (menú + barra superior + navegación por tareas + área de contenido + toasts),
    `HomeView` / `DashboardView` / `GlobalSearchView` / `PlaceholderView` / `StyleGuideView`.
  - Barra de menú (`Archivo`/`Ver`/`Ayuda`); atajos `Ctrl+K`, `Alt+Home`, `Ctrl+Shift+G`, `F1`,
    `Ctrl+Q`.
  - Tema claro/oscuro: `Theme` + `ThemeManager` + toggle en el menú; `-Dsirmax.theme=dark`.
  - Pruebas: `ShellNavigatorTest`, `NavItemTest`, `MessagesTest`, `ThemeManagerTest` +
    `ShellViewSmokeTest` (arranca el toolkit JavaFX). ADR 0013 (JavaFX programático sin FXML).
- **Fase 0/1 — Fundación del repositorio.**
  - Estructura del monorepo: `apps/desktop`, `apps/landing`, `apps/docs`, `backend`, `database`,
    `scripts`, `docs`, `.github`.
  - Modelo de ramas permanentes `experiment → testing → main`.
  - Documentos de gobernanza: README, CONTRIBUTING, CODE_OF_CONDUCT, SECURITY, SUPPORT,
    ARCHITECTURE, DEVELOPMENT, DATABASE, BACKUP, RELEASE, ROADMAP, THIRD_PARTY_LICENSES,
    TRADEMARK_POLICY.
  - ADR 0001–0012 (Java 25, JavaFX, SQLite, Gradle, arquitectura modular, motores de servicio /
    workflow / tasas, backup en Google Drive, AGPL-3.0-or-later, monorepo, flujo de tres ramas).
  - Documentación de dominio: glosario, mapa de dominio, ERD inicial, mapa de módulos, mapa de UX,
    plan de build.
  - Esqueleto del escritorio: proyecto Gradle multi-módulo (`sirmax-shared`, `sirmax-domain`,
    `sirmax-application`, `sirmax-infrastructure`, `sirmax-ui`, `sirmax-app`) con catálogo de
    versiones.
  - Esqueleto de la landing (Next.js + TypeScript) con navegación y CTAs obligatorios
    (_VER PROYECTO EN GITHUB_, _Descargar SIRMAX_).
  - Esqueleto de documentación (Docusaurus) con la estructura de secciones prevista.
  - GitHub Actions: `desktop`, `landing`, `docs`, `security`; plantillas de issues y PR.
  - `database/migrations/V0001__baseline.sql` (línea base mínima del esquema).
  - `package-lock.json` comiteado; los workflows usan `npm ci` con caché de npm.

### Changed
  - Gradle **8.12 → 9.7.1** (Gradle 8.x no arranca sobre JDK 25). El wrapper queda comiteado con
    verificación de checksum.
  - El cableado de dependencias se movió del bloque `subprojects {}` al `build.gradle.kts` de cada
    módulo (el accesor `libs` no está disponible en `subprojects {}`).
  - ArchUnit **1.3.0 → 1.5.0** (ASM de 1.3.0 no lee bytecode de Java 25).
  - Docusaurus **3.7.0 → 3.10.2** (3.7 falla con el webpack actual y con Node ≥ 24). Se pospone el
    tema de Mermaid a la Fase 12.

### Verified
  - Los cuatro workflows de CI (Desktop, Docs, Landing, Security) **en verde** en `experiment`
    (commit `2b24c71`). Build local reproducido: `./gradlew build`, `docs build`, landing
    `lint`/`typecheck`/`build`.

[Unreleased]: https://github.com/mrnoirhat/sirmax/commits/experiment
