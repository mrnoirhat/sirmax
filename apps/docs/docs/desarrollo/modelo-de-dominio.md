---
title: "Modelo de dominio"
sidebar_position: 2
description: "Las entidades, sus invariantes y dónde viven."
---

# Modelo de dominio

Todo en `sirmax-domain`, Java puro. Sin anotaciones, sin framework, sin
dependencias: una clase de dominio se prueba instanciándola.

## Los grupos

| Paquete | Qué guarda |
| --- | --- |
| `identity` | `Person`, `Organization`, `Identification`, `Address` |
| `org` | `OrganizationUnit`, `Department`, `InstitutionProfile` |
| `service` | `ServiceDefinition`, `ServiceDefinitionVersion`, `RequirementDef`, `Sla` |
| `procedure` | `Procedure`, sus requisitos, eventos y adjuntos |
| `finance` | `Invoice`, `Payment`, `Refund`, `CashSession`, `FeeRule` |
| `document` | `IssuedDocument`, `DocumentSnapshot`, `PrinterProfile` |
| `asset` | `MunicipalAsset`, `Agreement`, `AssetHolder` |
| `registry` | Registros municipales e inspecciones |
| `backup` | `BackupRecord`, `BackupSchedule` |
| `audit` | `AuditEvent`, `AuditChain` |
| `security` | `AppUser`, `Permission`, `SecurityPolicy` |
| `workflow` | El motor de flujo |
| `rules` | Evaluador de expresiones para condiciones |

## Piezas que aparecen en todas partes

**`Money`** (en `sirmax-shared`) — un `record` de unidades mínimas más
`Currency`. Las operaciones usan `Math.addExact`, así que un desbordamiento falla
en vez de dar la vuelta en silencio.

**`PartyRef`** — referencia a una persona o a una organización. Una factura puede
ir a nombre de cualquiera de las dos, y sin esto haría falta duplicar cada
relación.

**`ArchiveStatus`** — activo o archivado. La alternativa al `DELETE`.

## Los invariantes viven en la entidad

Un `ServiceDefinitionVersion` publicado rechaza que lo modifiquen; no hay un
validador externo que pueda olvidarse. `Money.plus` exige la misma moneda.
`Procedure` no salta a un paso que su flujo no permite.

La regla práctica: si algo nunca debe pasar, el constructor o el método lo tiene
que impedir, no un `if` en la capa de arriba.

## El snapshot del documento

`DocumentSnapshot` es el caso más ilustrativo. Al emitir, congela dentro del
documento la institución, el cliente, las líneas y los totales.

Deliberadamente redundante respecto a las tablas de origen: reimprimir tiene que
dar **el mismo papel**, y si el documento apuntara a los datos actuales, dejaría
de ser una copia para ser un documento nuevo con fecha vieja.

## La cadena de auditoría

`AuditChain` encadena cada evento con el hash SHA-256 del anterior. Alterar uno
invalida todos los siguientes, y la verificación dice **en qué entrada** se rompe.

No impide que alguien manipule el archivo por fuera; garantiza que se note.
