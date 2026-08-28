# 0013 — UI: JavaFX programático (sin FXML) y navegación/i18n sin framework

- **Estado:** Aceptado
- **Fecha:** 2026-08-28
- **Decisores:** Ingeniería principal SIRMAX
- **Fase relacionada:** 2

## Contexto

La Fase 2 construye el shell de escritorio y el Design System. Hay que decidir cómo se estructura la
capa `sirmax-ui`: FXML + controladores vs. construcción programática; y cómo se modela la navegación
y el i18n para que la lógica del shell sea testeable sin arrancar el toolkit.

## Decisión

- **JavaFX programático, sin FXML.** Las vistas se construyen en código Java. Motivos: menos "magia"
  y menos ficheros que sincronizar; el Design System se expresa mejor como **factorías de componentes**
  (`Buttons`, `Cards`, `Banner`, `StatefulContent`, `ToastHost`, `Dialogs`, `FormField`, `DataTable`)
  + un único `sirmax.css`; y las vistas se pueden instanciar directamente en pruebas.
- **Navegación sin framework.** `Navigator` es una interfaz **sin tipos JavaFX**; `ShellNavigator`
  es Java plano (ruta actual + back-stack acotado + listeners síncronos) y está cubierto por pruebas
  unitarias normales. El `ShellView` se suscribe y cambia el nodo del área de contenido.
- **i18n propio y ligero.** `Messages` resuelve claves contra `ResourceBundle`
  (`messages.properties` en español como bundle base; `messages_en`, `messages_fr` se añaden sin
  tocar las llamadas). El dominio y la aplicación llevan `MessageKey` (de `sirmax-shared`); la UI
  resuelve. **Ningún texto de usuario literal en código.**
- **Estilos vía CSS de JavaFX** con _looked-up colors_ (`-sirmax-*`) y `derive()`. Tokens de color,
  tipografía, radios y espaciado en `.root`. El color nunca es el único indicador semántico
  (icono + texto acompañan siempre a la severidad).
- **Pruebas de UI:** la lógica (navegación, i18n, máquinas de estado de vista) con JUnit normal; un
  _smoke test_ (`ShellViewSmokeTest`) arranca el toolkit con `Platform.startup` y verifica en el hilo
  de JavaFX que el shell, todas las vistas registradas y la `Scene` se construyen sin error y que la
  navegación intercambia el contenido. Corre en el runner Windows de CI (con sesión de escritorio).
- **MVVM ligero:** cuando una vista necesite estado observable usará _properties_ de JavaFX dentro de
  `sirmax-ui` (capa que sí puede depender de JavaFX). No se introduce un framework MVVM.

## Consecuencias

**Positivas**
- El shell y sus vistas se testean sin FXML y con arranque mínimo del toolkit.
- La navegación es depurable y determinista (historia + listeners explícitos).
- Un solo lugar para el tema; componentes consistentes por construcción.

**Negativas / costes**
- Construir jerarquías grandes en código es más verboso que FXML; se mitiga con factorías.
- Sin editor visual (Scene Builder). Aceptable para el estilo de UI buscado (calmado, denso, no
  "arrastrar y soltar").
- El _smoke test_ de UI necesita un entorno con toolkit (runner Windows headed en CI).

## Alternativas consideradas

- **FXML + controladores + Scene Builder** — estándar, buen tooling visual, pero añade ficheros y
  _binding_ por convención de nombres; el _hot path_ del proyecto es densidad de datos y consistencia,
  no maquetación visual libre.
- **Framework MVVM (mvvmFX, afterburner.fx)** — innecesario para el alcance; más dependencias.
- **Navegación con _properties_ de JavaFX en el `Navigator`** — ataría la lógica de navegación al
  toolkit y complicaría las pruebas.

## Referencias

- Master prompt Fase 2, §12 "Final UX standard", §34–35 (service-first), §36 (i18n), §78 (UX review).
- ADR relacionados: [0002](./0002-javafx-desktop.md), [0005](./0005-modular-domain-architecture.md).
