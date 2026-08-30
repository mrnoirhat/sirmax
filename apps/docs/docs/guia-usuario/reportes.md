---
title: "Reportes"
sidebar_position: 11
description: "Cuánto entró, por qué vía, por qué servicio y qué falta cobrar."
---

# Reportes

**Administración → Reportes**, con permiso `report.view`.

Cuatro preguntas que un ayuntamiento se hace al cerrar un día, una semana o un
mes. Las cuatro tablas se calculan sobre **el mismo rango de fechas**, así que
dos cifras de esta pantalla nunca pueden discrepar sobre qué período cubren.

## El rango

**Desde** y **Hasta**, y **Actualizar**. Por defecto, el mes en curso. El rango
**incluye el día final**: un reporte «hasta el 31» que se dejara fuera el 31 está
mal justo de la manera en que alguien lo va a notar.

Arriba queda la cifra de cabecera: total cobrado y número de cobros.

## Las cuatro tablas

**Cobros por medio de pago** — cuánto entró en efectivo, por transferencia, por
tarjeta. Es la que se contrasta con los cierres de caja del período.

**Cobros por servicio** — qué servicios producen el ingreso, ordenados por
número de operaciones. Sirve para decidir dónde poner personal.

**Trámites por estado** — cómo está repartida la carga: cuántos abiertos, cuántos
esperando requisitos, cuántos esperando pago. No lleva columna de total porque un
trámite no tiene dinero propio; una columna de guiones fingiría ser un dato.

**Pendiente de cobro** — facturas con saldo, por cliente. La lista de a quién
reclamar.

## Exportar

Cada tabla tiene **Exportar a CSV**. Se abre en Excel con los acentos correctos:
el archivo lleva marca UTF-8, sin ella un Windows en español convierte
«Facturación» en un jeroglífico.

El separador es el punto y coma, que es lo que espera el Excel en español.

## Sobre los importes

Los totales se suman **por moneda** y se muestran juntos si hubiera más de una.
Una instalación normal es de una sola moneda y se lee como una cifra; sumar pesos
con dólares en un único número es un error que solo descubre quien tiene que
explicarlo.
