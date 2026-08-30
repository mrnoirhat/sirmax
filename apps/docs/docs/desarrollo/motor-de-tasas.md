---
title: "Motor de tasas"
sidebar_position: 5
description: "Siete formas de calcular un cobro, todas en enteros."
---

# Motor de tasas

Cada versión de servicio lleva sus **reglas de tasa**. Es
[ADR 0008](https://github.com/mrnoirhat/sirmax/blob/main/docs/adr/0008-fee-engine.md).

## Los siete tipos

| `FeeRuleType` | Calcula |
| --- | --- |
| `FIXED` | Importe fijo |
| `QUANTITY_X_RATE` | Cantidad × tarifa |
| `AREA_BASED` | Superficie × tarifa por m² |
| `DURATION_BASED` | Días × tarifa diaria |
| `CATEGORY_BASED` | Tarifa según una clave (zona, categoría) |
| `LOCATION_BASED` | Tarifa según ubicación |
| `TIERED` | Por tramos |

Cada regla lleva su **tipo de cobro** —impuesto, arbitrio, tasa, contribución,
cargo por servicio, arrendamiento, recargo— porque la contabilidad municipal los
separa.

## El cálculo

`FeeCalculator` recibe un `FeeInput` con las variables del trámite y devuelve un
`Charge` con sus `ChargeLine`. Todo en `Money`, es decir en enteros: ver
[Reglas de negocio](../administracion/reglas-de-negocio.md).

Los tramos de `TIERED` se aplican **progresivamente**, como un impuesto por
tramos: cada porción del valor paga su tramo.

## Vigencia

Cada regla tiene `effectiveFrom` y opcionalmente `effectiveTo`. Permite dejar
preparada una tarifa que entra en vigor en enero sin tocar nada ese día.

Ojo: eso no sustituye al versionado del servicio. La vigencia decide qué regla
aplica **hoy**; el versionado decide qué reglas vio un expediente **al abrirse**.

## Desde la interfaz, solo monto fijo

La pantalla de servicios edita `FIXED` y nada más. Es lo que es casi cualquier
tasa dominicana, y las demás son configuración que se importa.

Un constructor de reglas a medias invita a publicar una tarifa que no calcula lo
que su autor creía, y eso se descubre cobrando de menos durante un mes. Ver
[Servicios](../guia-usuario/servicios.md).

## Descuentos

Requieren `fee.override` y quedan en la factura con **su motivo**. Un descuento
sin motivo es indistinguible de un error de tecleo.
