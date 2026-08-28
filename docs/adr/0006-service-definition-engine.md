# 0006 — Motor de definición de servicios configurable

- **Estado:** Aceptado
- **Fecha:** 2026-08-27
- **Decisores:** Ingeniería principal SIRMAX
- **Fase relacionada:** 4

## Contexto

Los servicios municipales **no** deben modelarse como una lista plana de formularios quemados. El
master prompt exige un catálogo configurable: un administrador debería poder definir un servicio
nuevo sin desarrollador cuando sea razonable. Un servicio agrupa requisitos, formulario, flujo,
reglas de tasa, plantillas de documento, SLA, numeración, validez y país.

## Decisión

Se implementa un **motor de definición de servicios** con estas piezas de datos:

```text
ServiceCatalog → ServiceDefinition (versionada) → {
  Requirements[]        (declarativos, por etapa, condicionales)
  FormSchema            (campos configurables cuando aportan)
  WorkflowDefinition    (ver ADR 0007)
  FeeRules[]            (ver ADR 0008)
  AuthorizationRules    (roles por acción/decisión)
  OutputDocuments[]     (plantillas + secuencia de numeración)
  Sla                   (días hábiles/naturales, escalado)
  Metadata              (código, categoría, departamento, país, override permitido)
}
```

- `ServiceDefinition` es **versionada e inmutable** una vez publicada (`DRAFT → ACTIVE → INACTIVE →
  ARCHIVED`). Un trámite guarda la versión con la que se abrió.
- No se borran definiciones con historial: se archivan/desactivan.
- El catálogo se entrega con **plantillas editables** (semilla dominicana), no con definiciones
  inmutables.
- La representación persistida combina columnas tipadas para lo estable (código, categoría, estado,
  departamento) y JSON validado para lo flexible (requisitos, form schema, reglas), con validación al
  guardar.

## Consecuencias

**Positivas**
- Un módulo especializado (cementerios, mercados…) reutiliza el mismo motor en vez de crear su propia
  arquitectura.
- Cambios de política municipal sin recompilar.
- El versionado protege trámites y facturas históricas.

**Negativas / costes**
- Validación no trivial del JSON de configuración; hace falta un editor guiado y buenos mensajes.
- Riesgo de sobre-configuración; se mitiga con plantillas y valores por defecto sensatos.

## Alternativas consideradas

- **Una pantalla/entidad por servicio** — inmantenible y contrario al master prompt.
- **Motor 100% genérico dirigido por reglas** — demasiado abstracto; se acota con un modelo de
  dominio específico (ADR 0007).

## Referencias

- Master prompt §15, §16, §22, §39, §54, §55, §80.
