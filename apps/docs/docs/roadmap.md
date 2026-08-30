---
title: "Hoja de ruta"
sidebar_position: 91
description: "Qué está hecho y qué viene después."
---

# Hoja de ruta

## Lo que ya está

Las quince fases del plan original están completas y publicadas. En resumen:

| Área | Estado |
| --- | --- |
| Ciudadanos, servicios versionados y trámites | Completo |
| Facturación, pagos, devoluciones y caja | Completo |
| Documentos, impresión y verificación | Completo |
| Módulos municipales (contratos, cementerios, mercados, inspecciones) | Completo |
| Copias de seguridad, restauración y auditoría | Completo |
| Reportes y exportación a CSV | Completo |
| Empaquetado Windows, instalador y releases | Completo |

El detalle versión a versión está en el
[CHANGELOG](https://github.com/mrnoirhat/sirmax/blob/main/CHANGELOG.md).

## Lo que viene

Nada de esto tiene fecha. El orden refleja qué desbloquea más trabajo real, no
qué es más vistoso.

**Multi-moneda de verdad.** Hoy SIRMAX viene configurado en pesos dominicanos.
El tipo `Money` ya lleva su moneda y los reportes suman por moneda en lugar de
mezclar, así que la base está; falta la configuración y las reglas de conversión.

**Portal del ciudadano.** Consulta del estado de un trámite y verificación de un
documento por su código, sin cuenta. Requiere una parte publicada en internet,
que es un cambio de modelo respecto a local-first y hay que diseñarlo con
cuidado.

**API de integración.** Para conectar con contabilidad o con sistemas
provinciales. Ver [API futura](./desarrollo/api-futura.md).

**Más idiomas.** La interfaz ya pasa por un catálogo de traducción; falta el
segundo idioma.

**Firma de código.** Un certificado de una autoridad reconocida quitaría el aviso
de editor desconocido. Ver
[SIGNING.md](https://github.com/mrnoirhat/sirmax/blob/main/docs/SIGNING.md).

## Cómo influir

Las [issues de GitHub](https://github.com/mrnoirhat/sirmax/issues) son el sitio.
Un caso concreto de un ayuntamiento real pesa más que una petición de función:
describe qué intentabas hacer y con qué te chocaste.
