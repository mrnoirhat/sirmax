---
title: "Facturación"
sidebar_position: 6
description: "Emitir facturas desde las tasas del servicio, cobrar, devolver y anular."
---

# Facturación

SIRMAX separa tres cosas que suelen confundirse:

| Concepto | Qué es |
| --- | --- |
| **Tasa** | Lo que el servicio cuesta según sus reglas. No es un documento. |
| **Factura** | El documento que pide ese importe a un ciudadano. |
| **Pago** | El dinero que efectivamente entró. |

## Emitir

Desde el trámite, o desde **Facturación**. SIRMAX aplica las reglas de tasas de
**la versión del servicio con la que se abrió el trámite** — no las de hoy. Un
permiso solicitado en diciembre se factura a la tarifa de diciembre aunque la
tarifa haya cambiado en enero.

Al emitir, la factura recibe su número (`FACT-2026-000001`) y **queda
congelada**: sus líneas y totales ya no cambian. Las correcciones se hacen con
devolución, anulación o una factura nueva; nunca editando.

Un descuento requiere permiso `fee.override` **y un motivo**.

## Cobrar

En **Facturación**, selecciona la factura. El monto viene lleno con lo pendiente,
porque eso es lo que pasa casi siempre; un pago parcial es una edición
deliberada.

- **Efectivo** exige una caja abierta (ver [Caja](./caja.md)) y muestra el
  **cambio** en su propia línea. Un cajero que tenga que calcularlo de cabeza se
  equivocará tarde o temprano.
- **Transferencia, tarjeta y cheque** piden número de referencia.

Si el ciudadano paga de más, SIRMAX registra **solo lo que debía** y el resto es
cambio. El exceso no es ingreso municipal.

## Devolver y anular

- **Devolver** escribe una fila nueva junto al pago; el pago original no se toca.
  Después de los hechos, la caja tiene que mostrar las dos patas: el dinero que
  entró y el que salió. Admite devoluciones parciales.
- **Anular** solo funciona si no hay dinero cobrado. Si lo hay, primero se
  devuelve. Una factura anulada con efectivo en la caja dejaría dinero sin
  documento que lo explique.

El número de una factura anulada **no se reutiliza jamás**.
