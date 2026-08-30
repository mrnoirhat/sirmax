---
title: "Introducción"
sidebar_position: 1
description: "Qué es SIRMAX, para quién es y qué decisiones lo hacen distinto."
slug: /
---

# SIRMAX

**Sistema Integral de Registros Municipales y Administración eXtensible.**

Un ayuntamiento pequeño hace todos los días lo mismo: atiende a un ciudadano,
comprueba requisitos, cobra una tasa, imprime un recibo y archiva el trámite.
SIRMAX es ese circuito completo en un solo programa, instalado en la máquina del
ayuntamiento.

## Para quién es

Para ayuntamientos que hoy trabajan con hojas de cálculo, talonarios y carpetas.
No hace falta un departamento de informática ni una conexión permanente.

## Las cuatro decisiones que lo definen

**Local-first.** La base de datos vive en el equipo del ayuntamiento. Sin
internet se sigue atendiendo, cobrando e imprimiendo. Internet sirve para la copia
de seguridad fuera del edificio, no para trabajar.

**El dinero se guarda en enteros.** Cada importe es un número de centavos más su
moneda, nunca un decimal en coma flotante. Un céntimo perdido en un redondeo es
un céntimo que alguien tiene que explicar.

**Un servicio publicado no se edita.** Cuando cambia una tasa se publica una
**versión nueva**; los trámites abiertos conservan la que tenían. Un ciudadano
que entregó su expediente en marzo paga lo que le dijeron en marzo.

**Todo lo que importa deja rastro.** Cobros, anulaciones, reimpresiones y cambios
de configuración se registran en una cadena de auditoría enlazada por hash: si
alguien altera la base de datos por fuera, la verificación lo detecta y dice
dónde.

## Qué cubre

| Área | Incluye |
| --- | --- |
| Ciudadanos | Registro único, detección de duplicados, historial completo |
| Servicios | Catálogo versionado, requisitos, plazos, tasas |
| Trámites | Flujo por pasos, requisitos, asignación, decisión |
| Dinero | Facturas, pagos, devoluciones, caja y cuadre |
| Documentos | Recibos, facturas y certificaciones con código de verificación |
| Registros | Contratos, cementerios, mercados, inspecciones |
| Operación | Copias de seguridad, restauración, auditoría, reportes |

## Por dónde empezar

1. [Instalación](./instalacion.md) — descargar e instalar en Windows.
2. [Primeros pasos](./primeros-pasos.md) — crear el ayuntamiento y el primer usuario.
3. [Guía de usuario](./guia-usuario/index.md) — el trabajo del día a día.

## Licencia

AGPL-3.0-or-later. Ver [Licencia](./licencia.md).
