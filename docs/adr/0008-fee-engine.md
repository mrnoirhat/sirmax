# 0008 — Motor de tasas versionable, separado de la facturación

- **Estado:** Aceptado
- **Fecha:** 2026-08-27
- **Decisores:** Ingeniería principal SIRMAX
- **Fase relacionada:** 4, 6

## Contexto

El master prompt es explícito: **tasa ≠ factura**. El flujo es
`Servicio → Regla de tasa → Cargo/Liquidación → Factura/Recibo → Pago`. Las tarifas municipales
cambian y son específicas de cada municipio; nunca se debe sobrescribir una regla histórica de forma
que cambie transacciones antiguas.

## Decisión

Se separa el **motor de tasas** del dominio de facturación:

- `FeeRule` con `tipo`: `FIXED`, `QUANTITY_X_RATE`, `AREA_BASED`, `DURATION_BASED`, `CATEGORY_BASED`,
  `LOCATION_BASED`, `TIERED`, `PERIODIC`. Parámetros tipados por tipo.
- Cada `FeeRule` tiene: `effectiveFrom`, `effectiveTo?`, `currency`, `referenciaLegal`,
  `metadatosDeAprobación`, e historial. Las reglas son **inmutables**; "editar" = crear una versión
  nueva con nueva vigencia.
- Un `ChargeType` configurable (taxonomía) distingue `IMPUESTO`, `ARBITRIO`, `TASA`, `CONTRIBUCIÓN`,
  `CARGO_SERVICIO`, `ARRENDAMIENTO`, `RECARGO`/penalización — para expansión a otros países.
- El motor produce un `Charge`/`Liquidation` (líneas con concepto, cantidad, precio unitario,
  descuento, recargo, categoría de cargo). Ese `Charge` es la entrada del módulo de facturación
  (Fase 6), que genera la `Invoice`.
- Override manual del importe **solo** con autorización por rol y motivo, auditado.
- La tabla de tarifas se configura sin recompilar; los cálculos usan `Money` (enteros/decimal
  exacto), nunca coma flotante.

## Consecuencias

**Positivas**
- Trámites y facturas antiguas siguen siendo interpretables con su versión de regla.
- Añadir un país = nueva taxonomía de `ChargeType` y nuevas reglas, sin tocar facturación.
- Auditoría financiera sólida.

**Negativas / costes**
- Más entidades que "un precio por servicio".
- La UI de configuración de reglas debe ser muy clara para personal no técnico.

## Alternativas consideradas

- **Precio fijo por servicio en la definición** — insuficiente para área/duración/tramos y para el
  versionado.
- **Fusionar tasa y factura** — rompe el histórico y la separación que el master prompt exige.

## Referencias

- Master prompt §19, §20, §21, §39, §59A.
