---
title: "Configuración"
sidebar_position: 13
description: "Datos del ayuntamiento, apariencia, copias y seguridad."
---

# Configuración

**Administración → Configuración**. Casi todo pide `config.manage`.

Está agrupada por **con qué frecuencia se toca**: la identidad se rellena una vez
al instalar, las copias se programan y se olvidan, la seguridad se revisa de
tanto en tanto.

## Datos del ayuntamiento

Nombre, RNC, dirección, teléfono, correo, web, pie de factura y encabezado de
documentos.

:::warning Esto sale impreso, y se congela
Cada documento guarda **dentro de sí** estos datos tal como estaban al emitirlo.
Una factura emitida con la ficha a medias se queda así para siempre, aunque
después la completes: reimprimirla muestra lo de entonces, que es lo correcto —
un documento reimpreso tiene que ser idéntico al original.

Rellénalo **antes** de emitir nada.
:::

El **pie de factura** es una línea al final de cada factura: el horario de caja,
por ejemplo.

## Apariencia

**Tema claro** u **oscuro**, aplicado al momento y recordado para la próxima
sesión. También con **Ctrl+Shift+D**.

## Copias de seguridad

| Ajuste | Para qué |
| --- | --- |
| Hacer copias automáticas | Actívalo. |
| Frecuencia | Diaria salvo que el volumen sea mínimo. |
| Hora | Fuera del horario de caja. |
| Copias que se conservan | Al pasar de ese número se borran las más viejas. |
| Cifrar las copias | Protege la copia si sale del edificio. |

**Hacer copia ahora** lanza una manual. Con el cifrado activado hace falta la
frase de cifrado, que se pide en la pantalla de respaldos: una frase escrita en
un formulario de configuración es una frase que acaba en una captura de pantalla.

Debajo, las últimas copias con fecha, tipo, tamaño y si salieron del equipo. Ver
[Copias de seguridad](./backup.md).

## Seguridad

Largo mínimo de contraseña, intentos antes de bloquear, duración del bloqueo,
bloqueo por inactividad, duración máxima de sesión y tamaño máximo de adjunto.

Los valores por defecto son conservadores. Subirlos endurece el acceso; bajarlos
lo abre.

**Verificar auditoría** recorre la cadena de eventos y comprueba que nadie la ha
alterado. Si está rota, dice **en qué entrada**. Ver [Seguridad](./seguridad.md).

## Impresoras

Los perfiles de impresión no están aquí sino en
[Documentos](./documentos-oficiales.md), junto a lo que se imprime.
