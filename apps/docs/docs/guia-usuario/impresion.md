---
title: "Impresión"
sidebar_position: 9
description: "Imprimir facturas y recibos, perfiles de impresora y reimpresiones."
---

# Impresión

SIRMAX genera **PDF de verdad**, no capturas de pantalla, y los envía a la cola
de impresión de Windows.

## Dos plantillas

| Plantilla | Papel | Para qué |
| --- | --- | --- |
| **Factura** | Carta (8.5 × 11 in) | Impresora de oficina: membrete, tabla de detalle, totales, bloque de pago, QR. |
| **Recibo** | Rollo de 58 o 80 mm | Impresora de mostrador: una columna monoespaciada, sin filetes ni color. |

El recibo angosto no es la factura encogida. A 180 puntos por pulgada y en blanco
y negro, las líneas finas se emborronan y los tonos se vuelven una mancha gris,
así que esa plantilla usa solo texto y espacios. Un concepto largo **se parte en
varias líneas**: un recibo que dice «Certificación de uso de su…» no le sirve a
nadie.

## Perfiles de impresora

Cada puesto configura los suyos una vez:

- Qué cola de Windows.
- Qué formato de papel.
- **Silencioso** o con diálogo. La impresora de recibos debe ser silenciosa: el
  cajero pulsa *Imprimir* y sale papel.

Se imprime a **tamaño real**, sin «ajustar a la página». El ajuste automático es
justo cómo una factura de tamaño Carta acaba impresa al 94 % sobre A4 con la
columna de totales cortada.

## Reimprimir

Con permiso `invoice.reprint`. Una reimpresión:

- **no genera un número nuevo**;
- **no duplica el pago**;
- sale marcada **COPIA**;
- queda en el historial del documento y en la auditoría.

Se puede responder «¿cuántas veces se imprimió esto y quién lo hizo?» con una
sola consulta.

## Verificación

Cada documento lleva un **código de verificación** y su QR. El código usa un
alfabeto sin `O/0`, `I/1` ni `S/5`, porque en la práctica se dicta por teléfono y
esos son justo los pares que se confunden.

El QR contiene **solo el código**. Nunca datos del ciudadano: cualquiera puede
fotografiarlo.
