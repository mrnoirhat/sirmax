# 0005 — Arquitectura Java modular y por capas

- **Estado:** Aceptado
- **Fecha:** 2026-08-27
- **Decisores:** Ingeniería principal SIRMAX
- **Fase relacionada:** 1, 3

## Contexto

El master prompt exige límites claros: `UI → Aplicación → Dominio → Infraestructura → SQLite/FS/
Integraciones`, con módulos transversales (seguridad, logging, configuración, reporting, backup,
printing, i18n). Reglas: la UI no llama a SQL; el dominio no depende de JavaFX; las integraciones
externas no contaminan el núcleo. También pide **no** caer en microservicios ni en abstracción
prematura.

## Decisión

Un solo proceso de escritorio dividido en **módulos Gradle por capa**:

| Módulo | Rol | Puede depender de |
| --- | --- | --- |
| `sirmax-shared` | `Money`, `Result`, identidad, errores base, claves i18n | — |
| `sirmax-domain` | Entidades, agregados, invariantes, servicios de dominio (Java puro) | `shared` |
| `sirmax-application` | Casos de uso, orquestación de transacciones, **puertos** (interfaces) | `domain`, `shared` |
| `sirmax-infrastructure` | Adaptadores: JDBC/SQLite, migraciones, ficheros, PDF, impresión, Drive, cifrado | `application`, `domain`, `shared` |
| `sirmax-ui` | JavaFX: shell, vistas, componentes, view-models | `application`, `shared` |
| `sirmax-app` | Composition root: `main`, wiring manual de dependencias, config de jpackage | todos |

Prohibiciones verificadas por prueba de arquitectura (ArchUnit): `domain → javafx`, `domain → jdbc`,
`domain → infrastructure`, `ui → jdbc`, `application → javafx`.

Los "módulos transversales" son **paquetes/servicios** dentro de estas capas (p. ej. `audit` como
puerto en `application` + adaptador en `infrastructure`), no módulos Gradle separados salvo que
crezcan lo suficiente.

Inyección de dependencias **manual** en `sirmax-app` (sin framework DI) para mantener el arranque
explícito y rápido.

## Consecuencias

**Positivas**
- Dominio testeable sin frameworks ni I/O.
- Sustituir SQLite por una API futura = nuevos adaptadores en `infrastructure`, sin tocar reglas.
- Los límites impiden que la UI "haga trampas" con SQL.

**Negativas / costes**
- Más módulos que un proyecto plano; algo de ceremonia en `build.gradle.kts`.
- Wiring manual crece con el nº de casos de uso (aceptable; se puede modularizar por área).

## Alternativas consideradas

- **Proyecto monolítico de un módulo** — más simple al inicio, pero no garantiza los límites que el
  master prompt exige.
- **Microservicios** — prohibido por el master prompt para un v1 de escritorio local.
- **Framework DI (Spring/Guice)** — innecesario para un proceso de escritorio; añade arranque y peso.

## Referencias

- Master prompt §2.2 "Layering", §1.3 "No uncontrolled complexity", §2.4 "Future API/cloud readiness".
