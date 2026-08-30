---
title: "Pruebas"
sidebar_position: 6
description: "Qué se prueba, con qué, y las trampas que ya nos han costado tiempo."
---

# Pruebas

JUnit 6 y AssertJ. Todo con `./gradlew build`, en la carpeta `apps/desktop`.

## Los niveles

**Dominio** — instanciar una entidad y comprobar su invariante. Sin base de
datos, sin JavaFX, milisegundos.

**Casos de uso** — contra dobles en memoria de los puertos. Comprueban permisos,
validación y el `Result` devuelto.

**Adaptadores** — contra SQLite en memoria, con las migraciones reales aplicadas.

**Arquitectura** — `LayerBoundaryTest` usa ArchUnit para que la dirección de las
dependencias sea un hecho comprobado y no un diagrama.

**Interfaz** — `FxTestSupport` arranca el toolkit JavaFX; las vistas se
construyen y se miden de verdad.

**Integración** — `FrontOfficeUiIT` (mostrador) y `BackOfficeUiIT`
(administración) montan el grafo completo: SQLite real, migraciones reales,
casos de uso reales, vistas reales.

## Trampas que ya costaron tiempo

Están escritas aquí porque cada una produjo una prueba en verde con la aplicación
rota.

**No te fíes de una captura de escritorio.** Dos veces «demostró» que la tarjeta
de inicio estaba descentrada. En una pantalla con escalado al 150 %, un proceso
que no es DPI-aware recibe una vista virtualizada y coordenadas que no
corresponden con lo dibujado. Usa `Scene.snapshot` / `Node.snapshot`, o mide el
layout en un test.

**Adjunta la hoja de estilos real.** Un test de layout sin el CSS mide una
disposición que la aplicación nunca dibuja.

**Renderiza a la altura natural y recorta.** Las vistas son más altas que una
ventana y el shell las desplaza. En una escena de altura fija, el `VBox` encoge
cada hijo a su mínimo, y para un `TableView` eso es la cabecera sola: sale una
captura con tablas vacías de datos que sí están.

**Comprueba también las claves de traducción.** Una clave que falta se dibuja
como la propia clave, que es el comportamiento correcto y justo por eso pasa
desapercibido. `MessageKeyAuditTest` compara el código con el catálogo y los
valores de cada enumeración.

**Un toolkit por JVM.** Cada test de integración protegía su `Platform.startup`
con su propio `static boolean`; el segundo en ejecutarse fallaba. El indicador
vive ahora en `FxToolkit`.

## Generadores, apagados por defecto

Escriben fuera de `build/`, así que hay que pedirlos:

```bash
./gradlew :sirmax-ui:test --tests "*ScreenshotGenerator*" -Dsirmax.screenshots=true
./gradlew :sirmax-ui:test --tests "*BrandAssetGenerator*" -Dsirmax.brand=true
```

## Qué se espera de un cambio

Una corrección trae la prueba que falla sin ella. Una función nueva, la cobertura
de su camino principal y de la decisión que la hace no trivial.

No perseguimos un porcentaje: un número alto con aserciones flojas es peor que
uno honesto.
