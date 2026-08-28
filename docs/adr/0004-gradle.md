# 0004 — Gradle (Kotlin DSL) para el build del escritorio

- **Estado:** Aceptado
- **Fecha:** 2026-08-27
- **Decisores:** Ingeniería principal SIRMAX
- **Fase relacionada:** 1

## Contexto

El master prompt fija Gradle para la automatización de build del escritorio. Necesitamos un build
multi-módulo (capas), catálogo de versiones centralizado, integración con el plugin de JavaFX y con
`jpackage`/`jlink`, y un wrapper para que CI y colaboradores no dependan de un Gradle instalado.

## Decisión

Build con **Gradle** usando **Kotlin DSL** (`*.gradle.kts`), en `apps/desktop/`:

- `settings.gradle.kts` declara los módulos `sirmax-shared`, `sirmax-domain`, `sirmax-application`,
  `sirmax-infrastructure`, `sirmax-ui`, `sirmax-app`.
- `build.gradle.kts` raíz configura convenciones comunes (`java`, `toolchain 25`, JUnit 5, warnings).
- `gradle/libs.versions.toml` es el **catálogo de versiones** único.
- Wrapper comiteado (`gradlew`, `gradlew.bat`, `gradle/wrapper/`).
- Plugins clave: `org.openjfx.javafxplugin`, `application`, y `org.beryx.jlink` (o `jpackage` directo)
  para el instalador Windows en la Fase 11.
- Análisis estático y formato (`spotless`, `errorprone`/`checkstyle`) se añaden como convención.

## Consecuencias

**Positivas**
- Multi-módulo con límites de dependencia explícitos (soporta las reglas de capas del ADR 0005).
- Catálogo de versiones evita divergencia de versiones entre módulos.
- Kotlin DSL da autocompletado y tipado en los scripts de build.
- Wrapper ⇒ builds reproducibles en CI y en cualquier máquina.

**Negativas / costes**
- Kotlin DSL tiene arranque en frío algo más lento que Groovy DSL.
- Complejidad de configuración de `jlink`/`jpackage` multiplataforma (se aborda en Fase 11).

## Alternativas consideradas

- **Maven** — muy estándar, pero el master prompt pide Gradle y el modelado multi-módulo + jlink es
  más flexible en Gradle.
- **Gradle Groovy DSL** — se prefiere Kotlin DSL por tipado y herramientas.

## Referencias

- Master prompt §2.1, Fase 1, Fase 11.
