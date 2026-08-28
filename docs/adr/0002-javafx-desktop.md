# 0002 — JavaFX para la UI de escritorio (no Electron)

- **Estado:** Aceptado
- **Fecha:** 2026-08-27
- **Decisores:** Ingeniería principal SIRMAX
- **Fase relacionada:** 1–2

## Contexto

El producto principal es una aplicación de escritorio Windows que debe sentirse **nativa y rápida**,
operar offline y empaquetarse con un runtime embebido. El master prompt prohíbe explícitamente
construir el escritorio principal como Electron.

## Decisión

La UI de escritorio se construye con **JavaFX** (OpenJFX), gestionada por el plugin
`org.openjfx.javafxplugin` de Gradle. El código JavaFX vive **solo** en el módulo `sirmax-ui`. El
resto de capas no depende de JavaFX.

Se adopta un Design System propio (tema, tipografía, componentes reutilizables) sobre JavaFX en la
Fase 2, con CSS de JavaFX para theming y `TestFX` para smoke tests headless en CI.

## Consecuencias

**Positivas**
- Rendimiento nativo, arranque rápido, sin proceso de navegador embebido ni consumo de Electron.
- Un solo lenguaje/toolchain con el dominio (Java).
- Empaquetado directo con `jpackage` + `jlink`.
- Theming con CSS de JavaFX; accesibilidad y navegación por teclado soportadas por la plataforma.

**Negativas / costes**
- Ecosistema de componentes menor que el web; algunos controles (tablas ricas, editores) requieren
  trabajo propio o librerías como `ControlsFX`.
- Curva para colaboradores acostumbrados a React.

**A vigilar**
- Distribución de los módulos nativos de JavaFX por plataforma en el empaquetado.

## Alternativas consideradas

- **Electron / Tauri + web** — descartado por mandato del master prompt y por peso/latencia.
- **Swing** — maduro pero anticuado para el estándar de UX exigido (moderno, calmado, profesional).
- **Compose Multiplatform (Kotlin)** — atractivo, pero se aparta de Java puro y añade riesgo de
  ecosistema para un v1 de escritorio Windows.

## Referencias

- Master prompt §2.1, §12 "Final UX standard", Fase 2.
