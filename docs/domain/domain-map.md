# Mapa de dominio de SIRMAX

Visión de alto nivel de los contextos del dominio y cómo se conectan a la **columna vertebral
compartida**. Detalle de entidades en [`erd.md`](./erd.md); vocabulario en [`glossary.md`](./glossary.md).

## 1. Columna vertebral (todos los módulos la reutilizan)

```mermaid
flowchart TD
    C[Ciudadano / Entidad] --> S[Solicitud / Caso]
    S --> P[Trámite / Expediente]
    P --> R[Requisitos]
    R --> D[Documentos]
    D --> V[Revisión / Inspección]
    V --> DEC[Decisión / Aprobación]
    DEC --> F[Tasa / Liquidación]
    F --> INV[Factura]
    INV --> PAY[Pago]
    PAY --> REC[Recibo / Documento oficial]
    REC --> EN[Entrega]
    EN --> AU[Auditoría]
    AU --> AR[Archivo]

    DEC -. "trámite gratuito" .-> REC
```

Reglas que el mapa hace explícitas:

- No todo trámite llega a `Tasa` (`GRATUITO`, `PAGO_EXTERNO`).
- `Tasa` ≠ `Factura` ≠ `Pago` ≠ `Movimiento de caja` ≠ `Recibo`.
- Un `Documento` adjunto no es un `Documento registrado`.
- La `Auditoría` acompaña cada transición relevante, no solo el final.

## 2. Contextos del dominio

```mermaid
flowchart LR
    subgraph Core["Núcleo compartido"]
        IDN[Identidad<br/>personas · organizaciones · direcciones]
        SVC[Servicios<br/>catálogo · definiciones · requisitos · workflow]
        PRC[Trámites<br/>expedientes · tareas · decisiones · inspecciones · SLA]
        DOC[Documentos<br/>adjuntos · registrados · plantillas · numeración]
        FIN[Finanzas<br/>tasas · facturas · pagos · caja · reembolsos]
        PRN[Impresión y PDF<br/>plantillas Letter/angosta · perfiles · reimpresión]
        CFG[Configuración<br/>institución/marca · reglas versionadas · país]
        SEC[Seguridad<br/>usuarios · roles · permisos · sesión]
        AUD[Auditoría]
        BKP[Backup / Restauración]
        RPT[Reportes]
        SRCH[Búsqueda global]
    end

    subgraph Specialty["Módulos especializados (se enchufan al núcleo)"]
        REG[Registro de Documentos / Conservaduría]
        CERT[Certificaciones y cartas]
        URB[Planeamiento Urbano / Construcción]
        CAD[Propiedad / Catastro]
        CEM[Cementerios]
        MKT[Mercados y espacios comerciales]
        PERM[Negocios / Publicidad / Permisos]
        MOV[Espacio público / Movilidad]
        OPS[Solicitudes / Quejas / Residuos / Órdenes de trabajo]
        COM[Casos comunitarios / sociales]
    end

    Specialty --> PRC
    Specialty --> DOC
    Specialty --> FIN
    Specialty --> AUD
    IDN --- PRC
    SVC --- PRC
    PRC --- DOC
    PRC --- FIN
    FIN --- PRN
    CFG --- SVC
    CFG --- FIN
    SEC --- PRC
    AUD --- FIN
```

## 3. Qué aporta cada módulo especializado

| Módulo | Entidades propias | Qué reutiliza del núcleo |
| --- | --- | --- |
| **Registro de Documentos / Conservaduría** | Documento registrado, libro/folio, anotación, copia certificada | Trámite, tasa→factura→pago, numeración, auditoría, búsqueda |
| **Certificaciones y cartas** | Plantilla de certificado, variables, política de reimpresión/anulación | Trámite, numeración, tasa/factura, firma/decisión, PDF/impresión |
| **Planeamiento Urbano / Construcción** | Proyecto, tipo de proyecto, planos, etapas de revisión, resolución | Propiedad/parcela, inspección, decisión, tasa por área/tipo, permiso (documento) |
| **Propiedad / Catastro** | Parcela, titularidad, estado de propiedad municipal, referencias legales | Persona/organización, contratos/arrendamientos, certificaciones, historial |
| **Cementerios** | Cementerio→sección→manzana→lote/nicho, ocupante/fallecido, inhumación, exhumación, disponibilidad | Contrato/concesión, tasa periódica/única, factura/pago, documentos, auditoría |
| **Mercados y espacios comerciales** | Mercado→zona→casilla, comerciante, acuerdo de ocupación, morosidad | Contrato, tasa periódica, factura/pago, inspección, reasignación/traspaso |
| **Negocios / Publicidad / Permisos** | Permiso/licencia, actividad, dimensiones/aforo, condiciones, renovación | Motor permiso+inspección, tasa (letrero/valla/tarima…), flujo de aprobación, documento |
| **Espacio público / Movilidad** | Permiso de uso de vía, cierre parcial, vehículo/placa, ventana horaria, ruta | Inspección, tasa por duración/ubicación, documento de permiso |
| **Operaciones (Solicitudes/Quejas/Residuos)** | Solicitud ciudadana, canal, categoría, asignación a cuadrilla, SLA, orden de trabajo, activo | Trámite (variante no financiera), asignación, seguimiento, cierre, auditoría |
| **Casos comunitarios / sociales** | Institución comunitaria, programa, carta de domicilio de junta de vecinos | Caso no financiero, certificación, auditoría |

## 4. Ciclos de vida principales

```mermaid
stateDiagram-v2
    [*] --> BORRADOR
    BORRADOR --> ABIERTO: registrar
    ABIERTO --> EN_REVISION: requisitos completos
    EN_REVISION --> DEVUELTO: faltan/errores
    DEVUELTO --> EN_REVISION: corregido
    EN_REVISION --> PENDIENTE_PAGO: requiere tasa
    PENDIENTE_PAGO --> EN_DECISION: pago registrado
    EN_REVISION --> EN_DECISION: trámite gratuito
    EN_DECISION --> APROBADO
    EN_DECISION --> RECHAZADO
    APROBADO --> ENTREGADO: documento emitido
    ENTREGADO --> COMPLETADO
    COMPLETADO --> ARCHIVADO
    RECHAZADO --> ARCHIVADO
    ABIERTO --> CANCELADO
    PENDIENTE_PAGO --> CANCELADO
```

```mermaid
stateDiagram-v2
    [*] --> DRAFT
    DRAFT --> ISSUED: emitir (asigna número + snapshot)
    ISSUED --> PARTIALLY_PAID: pago parcial
    ISSUED --> PAID: pago total
    PARTIALLY_PAID --> PAID: saldo cubierto
    ISSUED --> VOIDED: anular (autorizado, auditado)
    PARTIALLY_PAID --> VOIDED
    PAID --> REFUNDED: reembolso autorizado
```

## 5. Fronteras que no se cruzan

- `domain` no conoce JavaFX, JDBC ni Google Drive.
- Los módulos especializados **no** definen su propia arquitectura de finanzas/auditoría/documentos:
  usan la del núcleo.
- Las reglas específicas de país (identidad, moneda, requisitos legales) viven en el adaptador de
  país, no en el núcleo.
- Nada de reglas legales jurisdiccionales quemadas: si el contexto no las establece, se hacen
  configurables y se marcan para verificación legal.
