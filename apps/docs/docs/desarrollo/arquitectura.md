---
title: "Arquitectura"
sidebar_position: 1
description: "Siete módulos, una dirección de dependencia y un test que la vigila."
---

# Arquitectura

## Los módulos

```text
sirmax-shared          Money, Result, identificadores, errores base
      ↑
sirmax-domain          entidades e invariantes — Java puro, sin dependencias
      ↑
sirmax-application     casos de uso + puertos (interfaces)
      ↑
sirmax-infrastructure  adaptadores: SQLite, Jackson, PDFBox, ZXing, impresión
      ↑
sirmax-ui              JavaFX: shell, design system, vistas
      ↑
sirmax-app             composición: main, cableado manual, jpackage
```

Más `sirmax-architecture-tests`, que comprueba con ArchUnit que lo de arriba es
verdad y no una aspiración de un diagrama.

## La regla

**Las dependencias apuntan hacia dentro.** El dominio no sabe que existe SQLite;
la interfaz no sabe que existe una base de datos.

`sirmax-application` define **puertos** —`PersonRepository`,
`DocumentPrinter`, `BackupEngine`— e `infrastructure` los implementa. Quien
decide qué implementación se usa es `sirmax-app`, en un único sitio.

Esto no es purismo. Los tests del dominio corren sin base de datos ni JavaFX, en
milisegundos, y cambiar de SQLite a otra cosa tocaría un módulo en lugar de todos.

## Cómo la ve la interfaz

`sirmax-ui` se construye contra **`AppServices`**, una interfaz que enumera lo
único que la interfaz puede pedir. `CompositionRoot`, en `sirmax-app`, la
implementa.

Por eso una vista nunca importa nada de `infrastructure`: no podría, porque
`sirmax-ui` ni siquiera depende de ese módulo. Es
[ADR 0005](https://github.com/mrnoirhat/sirmax/blob/main/docs/adr/0005-modular-domain-architecture.md) y
lo vigila `LayerBoundaryTest`.

## Errores: `Result`, no excepciones

Los casos de uso devuelven `Result<T>`: `Ok` con el valor o `Err` con un código y
**una clave de traducción**. Un fallo esperable —permiso denegado, saldo
insuficiente— no es excepcional, y usar excepciones para lo esperable acaba en
`catch` vacíos.

Las excepciones quedan para lo que de verdad lo es: disco lleno, base corrupta.

## Concurrencia

La aplicación es de escritorio y monousuario por instalación. Las escrituras van
por `UnitOfWork`, que abre una transacción SQLite; la interfaz es de un solo hilo,
el de JavaFX.

No hay pool de conexiones ni servidor. Es lo que permite que funcione sin red.
