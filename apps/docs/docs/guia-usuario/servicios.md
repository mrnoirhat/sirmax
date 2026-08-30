---
title: "Servicios"
sidebar_position: 5
description: "El catálogo del ayuntamiento: qué ofrece, qué exige y qué cobra."
---

# Servicios

Un **servicio** es cada cosa que el ayuntamiento hace por un ciudadano: un uso de
suelo, una certificación, un arrendamiento de nicho. El catálogo define, para
cada uno, qué requisitos pide, en cuánto tiempo se compromete y cuánto cuesta.

Está en **Administración → Servicios** y hace falta el permiso
`service.configure`.

## Borrador y versión publicada

Esta es la regla que gobierna toda la pantalla:

- Un servicio nuevo nace como **borrador**. Se edita libremente.
- Al **publicar**, esa versión queda **fija para siempre**.
- Para cambiar algo se crea una **versión nueva**, que vuelve a ser borrador.

:::info Por qué no se puede editar lo publicado
Cada trámite guarda **con qué versión se abrió**. Si se pudiera editar una
versión viva, subir una tasa cambiaría el precio de expedientes ya entregados y
lo que se cobró dejaría de cuadrar con lo que dice el catálogo. Un ciudadano que
presentó papeles en marzo paga lo de marzo.
:::

Los trámites en curso siguen con su versión. Los nuevos usan la última publicada.

## Cargar el catálogo base

**Cargar catálogo base** trae los servicios municipales dominicanos más
frecuentes, agrupados por categoría y todos en borrador con **monto cero**:
ningún catálogo genérico sabe lo que cobra tu ayuntamiento. Hay que revisarlos y
publicarlos uno a uno.

## Agregar un servicio

| Campo | Qué poner |
| --- | --- |
| **Código** | Corto y estable: `USO-SUELO`. No cambia nunca. |
| **Nombre** | Como lo pide el ciudadano. |
| **Categoría** | Agrupa en el catálogo. |
| **Tipo** | Gratuito, con tasa, tasa condicional o pago externo. |

Se crea el borrador y la pantalla salta a él.

## Configurar el borrador

**Requiere pago antes de entregar** — el trámite no avanza a entrega sin factura
pagada.

**Requisitos** — separados por comas: `cédula, título de propiedad, croquis`.
Cada uno se convierte en una casilla del expediente.

**Plazo comprometido** — en días, laborables o naturales. `0` = sin plazo. Es lo
que marca un trámite como atrasado.

**Vigencia** — cuántos días vale el documento resultante. `0` = no vence.

**Monto** — importe fijo en DOP, con su concepto y tipo de cobro. Vacío = el
servicio es gratuito.

:::note Solo monto fijo desde aquí
Es lo que es casi cualquier tasa dominicana. Las reglas por unidad, por tramos o
por superficie existen en el motor de tasas pero se importan como configuración:
un constructor de reglas a medias invita a publicar una tarifa que no calcula lo
que quien la escribió creía. Ver [Motor de tasas](../desarrollo/motor-de-tasas.md).
:::

**Guardar borrador** y luego **Publicar**.

## Activar y desactivar

**Activar / desactivar** decide si el servicio se puede elegir al abrir un
trámite. Desactivar no borra nada ni afecta a los expedientes abiertos: solo deja
de ofrecerse.

La columna **Disponible** dice `No` mientras el servicio no tenga ninguna versión
publicada. Es lo esperable justo después de cargar el catálogo base.
