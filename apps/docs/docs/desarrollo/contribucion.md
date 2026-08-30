---
title: "Contribución"
sidebar_position: 10
description: "Cómo proponer un cambio y qué se espera de él."
---

# Contribución

## Antes de escribir código

**Abre una issue.** Sobre todo si el cambio es grande: es más barato discutir el
enfoque que revisar doscientas líneas que había que plantear de otra forma.

Para un fallo, lo útil es: qué hacías, qué esperabas, qué pasó, versión, y el
final de `%LOCALAPPDATA%\SIRMAX\logs` — revisado antes de pegarlo, que puede
llevar datos de ciudadanos reales.

## El flujo

```bash
git checkout experiment
git pull
git checkout -b mi-cambio
# ...
cd apps/desktop && ./gradlew build
npm run lint --workspace apps/landing
```

El PR va contra **`experiment`**, nunca contra `main`.

## Qué se espera

**Que compile y pase todo.** Los cuatro checks corren igualmente.

**Pruebas.** Una corrección trae la prueba que falla sin ella; ver
[Pruebas](./pruebas.md).

**Respetar las capas.** `LayerBoundaryTest` lo comprueba, pero conviene leer
[Arquitectura](./arquitectura.md) antes.

**No editar una migración publicada.** Se añade una nueva; ver
[Base de datos](./base-de-datos.md).

**Textos en el catálogo.** Nada de literales en las vistas: la clave va en
`messages.properties` y `MessageKeyAuditTest` lo verifica.

## Comentarios

El código dice *qué* hace; el comentario, **por qué**. Los que hay explican
decisiones —por qué el conteo de caja no viene rellenado, por qué el documento
congela sus datos— porque eso es lo que un lector futuro no puede deducir.

Un comentario que repite la línea siguiente es ruido que además envejece mal.

## Mensajes de commit

[Conventional Commits](https://www.conventionalcommits.org/es/):
`feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `ci`.

El cuerpo explica el porqué. Si el cambio corrige algo, di qué fallaba: es lo que
alguien buscará en el historial dentro de un año.

## Estilo

Java: sangría de 4, líneas de 100, `final` donde aporte. TypeScript y CSS: el
formato del proyecto.

Sin herramienta de formateo automático — si tu editor reformatea el archivo
entero, apágalo para este repositorio: un PR con doscientas líneas movidas y dos
cambiadas es imposible de revisar.

## Licencia

Al contribuir aceptas que tu código se publique bajo **AGPL-3.0-or-later**. Cada
archivo lleva su cabecera `SPDX-License-Identifier`.
