---
title: "Trámites"
sidebar_position: 4
description: "Abrir un trámite, completar requisitos, avanzar el flujo y cerrar el expediente."
---

# Trámites

Un **trámite** es cualquier gestión que un ciudadano abre en el ayuntamiento: una
certificación, un permiso de construcción, una queja, la inscripción de un
documento. Todos usan la misma pantalla porque, para quien atiende, todos son el
mismo trabajo: recibir, comprobar requisitos, cobrar si corresponde, resolver.

## Abrir un trámite

**Inicio → Registrar un trámite**, o el botón **Registrar un trámite** en la
pantalla de Trámites.

La pantalla pide dos cosas, en el orden en que se deciden en el mostrador:

1. **Qué servicio.** Solo aparecen los servicios con una versión publicada. Si la
   lista está vacía, todavía no se ha configurado el catálogo — ver
   [Servicios](./servicios.md).
2. **Para quién.** Escribe el nombre; SIRMAX busca mientras tecleas y **sin
   acentos**: «pena» encuentra «Peña». Si el ciudadano no aparece, se registra
   ahí mismo.

:::tip Antes de crear un ciudadano nuevo
Al pulsar *Registrar ciudadano*, SIRMAX comprueba primero si ya existe. Si la
cédula coincide, **selecciona a esa persona** en vez de crear un duplicado. Si
solo se parece el nombre, muestra los candidatos y espera a que decidas. Ver
[Ciudadanos](./ciudadanos.md).
:::

Al abrir el trámite, SIRMAX le asigna un número (`TRM-2026-000001`), copia la
lista de requisitos del servicio y calcula la fecha de vencimiento según el SLA
configurado. Todo eso ocurre en una sola operación: si algo falla, no queda un
trámite a medias ni se gasta un número.

## La pantalla del trámite

Está ordenada como se trabaja, de arriba abajo:

| Bloque | Para qué |
| --- | --- |
| **Aviso de bloqueo** | «Faltan 2 requisitos». Es lo primero porque es la única pregunta que importa en el mostrador. |
| **Requisitos** | La lista con casillas. Marcar y desmarcar según lo que traiga el ciudadano. |
| **Datos del trámite** | El formulario que el servicio define. Cambia por servicio. |
| **¿Qué hago ahora?** | Solo las acciones que el flujo permite **y** que tu permiso alcanza. |
| **Historial** | Todo lo ocurrido, lo más reciente arriba. |

### Requisitos

Un requisito puede ser **condicional**: «copia del título» solo aparece si el
solicitante es propietario. SIRMAX evalúa esas condiciones con los datos del
formulario, así que la lista cambia mientras se completa.

Un requisito obligatorio que falte **bloquea su etapa y todas las siguientes**.
No se puede saltar hacia adelante.

### Dispensar un requisito

Con permiso `procedure.decide` aparece **Dispensar** junto a los requisitos
pendientes. Siempre pide un motivo, que queda en el historial y en la auditoría.
Es la salida de emergencia del mostrador, no un atajo.

## Avanzar el trámite

Los botones de **¿Qué hago ahora?** salen del flujo configurado en el servicio,
no de una lista fija. Si un botón no está, es porque el flujo no lo permite en
este paso o porque tu usuario no tiene el permiso — nunca aparece deshabilitado
con una explicación.

Antes de avanzar, SIRMAX comprueba en este orden:

1. **Permiso** — `procedure.work` para avanzar; `procedure.decide` para aprobar,
   rechazar o anular.
2. **Requisitos** — los obligatorios de la etapa de **destino**.
3. **Pago** — un paso de tipo *punto de cobro* no deja pasar hasta que la
   factura esté saldada.

Un rechazo siempre pide motivo. El ciudadano tiene derecho a saber qué pasó.

## Cerrar

- **Aprobar** deja el trámite listo para entrega.
- **Rechazar** y **Anular** lo cierran.
- Un trámite cerrado no se modifica. Si hace falta, se **reabre** — y eso también
  queda registrado.

## Colas de trabajo

La pantalla de **Trámites** ofrece cuatro colas guardadas: *Todos los abiertos*,
*Míos*, *Sin asignar* y *Vencidos*. Abre en «todos los abiertos» a propósito: en
una oficina pequeña la asignación a menudo ni se usa, y una pantalla vacía al
entrar se lee como una aplicación rota.

Los trámites se ordenan por urgencia y luego por fecha de vencimiento. Un
vencido lo dice **con palabras**, no solo con color: el color no sobrevive a una
impresión en blanco y negro.
