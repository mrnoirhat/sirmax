# 0007 — Motor de flujo de trabajo pragmático, no genérico

- **Estado:** Aceptado
- **Fecha:** 2026-08-27
- **Decisores:** Ingeniería principal SIRMAX
- **Fase relacionada:** 4

## Contexto

El master prompt pide un motor de flujo que soporte pasos secuenciales, ramificación, aprobaciones,
rechazo, devolución al solicitante, reasignación, SLA, condiciones, roles requeridos, acciones,
generación automática de documentos, generación de tasa y checkpoint de pago — **sin** convertirse en
un motor tan abstracto que nadie pueda mantenerlo.

## Decisión

Motor **específico de dominio**, dirigido por datos pero con un vocabulario cerrado:

- Un `WorkflowDefinition` es una lista ordenada de `Step`, cada uno con: `key`, `nombre` (i18n),
  `tipo` (`TASK`, `REVIEW`, `APPROVAL`, `INSPECTION`, `PAYMENT_CHECKPOINT`, `DOCUMENT_OUTPUT`),
  `rolRequerido`, `sla`, y `transiciones` a otros steps.
- Transiciones permitidas fijas: `ADVANCE`, `APPROVE`, `REJECT`, `RETURN_FOR_CORRECTION`,
  `REASSIGN`, `CANCEL`. No hay lenguaje de scripting arbitrario.
- Condiciones de ramificación mediante un evaluador de expresiones **restringido** sobre un contexto
  tipado (datos del trámite, requisitos cumplidos, tasa calculada, pago registrado). Sin acceso a I/O
  ni a código.
- El estado de ejecución vive en el trámite: `currentStepKey`, historial de transiciones (quién,
  cuándo, motivo), y `stepState` por step.
- Acciones automáticas de un step (`DOCUMENT_OUTPUT`, generación de tasa) se disparan al entrar y son
  idempotentes.
- `PAYMENT_CHECKPOINT` bloquea el avance hasta que la factura asociada esté `PAID` (o `PARTIALLY_PAID`
  si el servicio lo permite).

## Consecuencias

**Positivas**
- Cubre certificados, permisos, registros, quejas y licencias con una sola estructura (§16 del master
  prompt).
- Auditable y depurable: el historial de transiciones es explícito.
- Sin riesgo de "workflow engine dentro de un workflow engine".

**Negativas / costes**
- Un caso raro puede necesitar un `tipo` de step nuevo ⇒ cambio de código (aceptable y controlado).
- El evaluador de expresiones restringido hay que diseñarlo y testearlo con cuidado.

## Alternativas consideradas

- **BPMN / motor externo** — sobredimensionado, curva alta, difícil de auditar en este contexto.
- **Máquina de estados libre configurable por el usuario** — potente pero se vuelve inmantenible.
- **Flujos quemados en código por servicio** — rápido al inicio, pero contrario al producto
  configurable.

## Referencias

- Master prompt §17, §18, §28, §30.
