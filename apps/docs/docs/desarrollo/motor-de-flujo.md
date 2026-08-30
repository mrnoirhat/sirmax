---
title: "Motor de flujo"
sidebar_position: 4
description: "Cómo avanza un trámite, definido como datos y no como código."
---

# Motor de flujo

Cada versión de servicio lleva su **flujo**: los pasos por los que pasa un
trámite y las transiciones posibles entre ellos. Es
[ADR 0007](https://github.com/mrnoirhat/sirmax/blob/main/docs/adr/0007-workflow-engine.md).

Se guarda como datos, no como código, para que añadir un trámite municipal no
exija recompilar.

## Las piezas

**`WorkflowDefinition`** — el primer paso y la lista de pasos.

**`WorkflowStep`** — clave, etiqueta, tipo, departamento responsable, orden y
transiciones que salen de él.

**`StepType`** — `TASK`, `REVIEW`, `APPROVAL`, `INSPECTION`,
`PAYMENT_CHECKPOINT`.

**`Transition`** — a qué paso lleva, o que termina el trámite.
`TransitionKind`: `ADVANCE`, `APPROVE`, `REJECT`, `RETURN_FOR_CORRECTION`,
`REASSIGN`.

## Cómo decide

`WorkflowEngine.availableTransitions` recibe el flujo, el paso actual y las
variables del trámite, y devuelve **solo las transiciones aplicables**. Una
transición puede llevar una condición, evaluada por `rules.ExpressionEvaluator`
sobre esas variables.

La interfaz dibuja un botón por transición disponible, y filtra además por
permisos: aprobar necesita `procedure.decide`.

## Un ejemplo

```text
recepción  ──ADVANCE──▶  revisión  ──APPROVE──▶  (aprobado)
                            │
                            └──RETURN_FOR_CORRECTION──▶ recepción
```

## Validación al publicar

`WorkflowValidator` comprueba antes de publicar que el paso inicial existe, que
toda transición apunta a un paso real, que no hay pasos inalcanzables y que hay
al menos una salida terminal.

Un flujo con un paso sin salida atrapa expedientes reales. Encontrarlo al
publicar cuesta un mensaje; encontrarlo en producción cuesta una intervención en
la base de datos.

## Trazabilidad

Cada avance añade un `ProcedureEvent` con el paso, quién y cuándo. El historial
del expediente es esa lista.
