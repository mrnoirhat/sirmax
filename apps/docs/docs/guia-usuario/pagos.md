---
title: "Pagos"
sidebar_position: 8
description: "Cobrar, cobrar a medias, devolver y anular."
---

# Pagos

Un pago siempre va **contra una factura**. No hay cobros sueltos: un ingreso que
no cuelga de una factura es un ingreso que nadie puede explicar en una auditoría.

Todo esto vive en **Facturación**, debajo de la lista de facturas.

## Cobrar

1. Selecciona la factura. El importe se rellena con **el saldo pendiente**, que
   es lo que pasa noventa y nueve de cada cien veces.
2. Elige el método.
3. **Cobrar**.

### Métodos

| Método | Pide | Afecta a la caja |
| --- | --- | --- |
| Efectivo | Entregado (para el cambio) | Sí |
| Transferencia | Referencia | No |
| Tarjeta | Referencia | No |
| Cheque | Referencia | No |

Solo el efectivo pasa por el cajón, así que solo el efectivo exige
[caja abierta](./caja.md).

### El cambio

En efectivo, escribe **lo que te entregó el ciudadano**. SIRMAX muestra el cambio
en una línea aparte. Un cajero que lo calcula de cabeza acaba equivocándose, y el
error aparece en el cuadre del cierre sin que nadie sepa de dónde vino.

## Pagos parciales

Cobra menos que el saldo y la factura queda en **Pago parcial**. Sigue en la
lista con el saldo actualizado y se puede cobrar el resto en otro momento, con
otro método, incluso por otro cajero. Cada cobro es su propio recibo.

## Devoluciones

Selecciona el **pago** en la tabla inferior y pulsa **Devolver**. Hace falta el
permiso `payment.refund` y **un motivo escrito**.

La devolución no borra el cobro: queda el pago original más la devolución, ambos
en el historial y en la auditoría. Así se puede reconstruir qué pasó, que es
justo lo que hace falta cuando alguien pregunta.

## Anular una factura

**Anular** invalida la factura entera. Requiere `invoice.void` y motivo.

:::warning Anular no devuelve el dinero
Son dos operaciones distintas y a propósito. Si ya se cobró, primero se devuelve
el pago y después se anula la factura. Una anulación que moviera dinero sola
haría imposible distinguir un error administrativo de una devolución real.
:::

Los documentos ya emitidos contra esa factura siguen existiendo; se anulan aparte
desde [Documentos](./documentos-oficiales.md).

## Qué queda registrado

De cada pago: importe, método, referencia, quién cobró, cuándo, con qué caja y
contra qué factura. De cada devolución y anulación, además, **el motivo**.
