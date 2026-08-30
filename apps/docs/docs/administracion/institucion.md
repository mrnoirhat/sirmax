---
title: "Datos del ayuntamiento"
sidebar_position: 1
description: "La identidad que aparece impresa, y por qué se congela en cada documento."
---

# Datos del ayuntamiento

**Configuración → Datos del ayuntamiento**, con permiso `config.manage`.

## Qué se guarda

| Campo | Dónde aparece |
| --- | --- |
| Nombre del municipio | Encabezado de todo documento |
| RNC | Facturas |
| Dirección, teléfono, correo, web | Encabezado |
| Pie de factura | Última línea de cada factura |
| Encabezado de documentos | Sobre el título de las certificaciones |

## La regla que hay que entender

Al emitir un documento, SIRMAX **copia estos datos dentro del documento**. No
guarda una referencia: guarda el texto.

Suena redundante hasta que se piensa en reimprimir. Si el documento apuntara a la
ficha actual, reimprimir una factura del año pasado la mostraría con el teléfono
de hoy — y dejaría de ser una copia del original para pasar a ser un documento
nuevo con fecha vieja. Lo mismo vale para el nombre del cliente, las líneas y los
totales: todo se congela.

:::danger Rellénalo antes de emitir el primer documento
Lo que emitas con la ficha a medias se queda a medias para siempre. No hay forma
de reparar un documento ya emitido, y es deliberado: si se pudieran editar, no
servirían como comprobante.
:::

## Cambiar los datos

Se puede en cualquier momento y afecta a lo que se emita **a partir de entonces**.
Un cambio de teléfono no reescribe la historia, y así es como debe ser.

Si el cambio es sustancial —una fusión de municipios, un cambio de RNC— conviene
[hacer una copia de seguridad](../guia-usuario/backup.md) antes, para poder
señalar el momento exacto.

## Logotipo

`logoPath` y `secondaryLogoPath` admiten rutas a imágenes para el encabezado.

Los documentos se diseñan para seguir siendo legibles **en blanco y negro**: el
color nunca es la única señal. Una impresora de recibos térmica no tiene color, y
un ayuntamiento imprime la mayoría de sus papeles en la más barata que tenga.
