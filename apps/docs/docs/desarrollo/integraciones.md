---
title: "Integraciones"
sidebar_position: 7
description: "Lo que SIRMAX toca fuera de sí mismo, y lo poco que necesita."
---

# Integraciones

SIRMAX es local-first: funciona con el cable de red desconectado. Todo lo de aquí
es opcional.

## Impresión

A través de la API de impresión de Java. Los **perfiles** dicen a qué cola va
cada tamaño de papel; ver
[Impresión](../guia-usuario/impresion.md).

Formatos: carta, legal, A4 y rollo de 58 y 80 mm. Los recibos se componen para
ancho fijo en monoespaciada, contando columnas: 32 en 58 mm, 48 en 80 mm.

Detalle que costó una corrección: cuando un valor no cabe en su columna, no se
trunca sino que baja a su propia línea alineada a la derecha. Un número de
documento cortado a la mitad no sirve para nada.

## PDF

**PDFBox** para los documentos de tamaño folio, **ZXing** para el código QR de
verificación. Ambos empaquetados: no hay servicio externo.

## Copia de seguridad en Google Drive

Opcional, [ADR 0009](https://github.com/mrnoirhat/sirmax/blob/main/docs/adr/0009-google-drive-backup.md).
Sube la copia a una carpeta que elige el ayuntamiento. Sin cuenta conectada, las
copias son locales y todo lo demás funciona igual.

Es la única función que requiere internet, y sirve para lo único que no puede
resolverse en el edificio: que la copia sobreviva a un incendio o a un robo.

## Lo que SIRMAX **no** hace

- No llama a servicios de terceros para operar.
- No envía telemetría. Nada sale del equipo salvo la copia, si la activas.
- No requiere cuenta en ningún sitio.

Un ayuntamiento pequeño con conexión intermitente tiene que poder atender igual.

## Salidas hacia fuera

**CSV** desde cualquier tabla de [Reportes](../guia-usuario/reportes.md), con
marca UTF-8 y punto y coma, que es lo que espera el Excel en español.

**PDF** de cada documento emitido.

**Código de verificación** en cada documento, comprobable contra la base de
datos. Es la base del portal de verificación descrito en
[API futura](./api-futura.md).
