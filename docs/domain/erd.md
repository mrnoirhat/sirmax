# ERD inicial de SIRMAX

Modelo entidad-relación **inicial** para orientar la Fase 3 (dominio + base de datos). No es el
esquema final: la fuente de verdad del esquema serán las migraciones en
[`database/migrations/`](../../database/migrations/). Convenciones de tipos/dinero/numeración en
[`DATABASE.md`](../../DATABASE.md).

## 1. Núcleo — identidad, servicios, trámites

```mermaid
erDiagram
    ORGANIZATION_UNIT ||--o{ DEPARTMENT : tiene
    ORGANIZATION_UNIT ||--|| INSTITUTION_PROFILE : "marca/branding"
    DEPARTMENT ||--o{ APP_USER : "adscribe"
    APP_USER ||--o{ USER_ROLE : tiene
    ROLE ||--o{ USER_ROLE : asigna
    ROLE ||--o{ ROLE_PERMISSION : concede
    PERMISSION ||--o{ ROLE_PERMISSION : "en"

    PERSON ||--o{ IDENTIFICATION : posee
    ORGANIZATION ||--o{ IDENTIFICATION : posee
    PERSON ||--o{ CONTACT : tiene
    ORGANIZATION ||--o{ CONTACT : tiene
    PERSON ||--o{ ADDRESS : "reside/localiza"
    ORGANIZATION ||--o{ ADDRESS : "domicilia"

    SERVICE_CATEGORY ||--o{ SERVICE_DEFINITION : agrupa
    SERVICE_DEFINITION ||--o{ SERVICE_DEFINITION_VERSION : versiona
    SERVICE_DEFINITION_VERSION ||--o{ REQUIREMENT_DEF : declara
    SERVICE_DEFINITION_VERSION ||--o{ WORKFLOW_STEP_DEF : define
    SERVICE_DEFINITION_VERSION ||--o{ FEE_RULE : "tarifa"
    SERVICE_DEFINITION_VERSION ||--o{ OUTPUT_DOCUMENT_DEF : produce
    SERVICE_DEFINITION_VERSION }o--|| NUMBERING_SEQUENCE : "numera trámite"
    DEPARTMENT ||--o{ SERVICE_DEFINITION : "responsable (no exclusivo)"

    PROCEDURE }o--|| SERVICE_DEFINITION_VERSION : "instancia de"
    PROCEDURE }o--|| PERSON : "solicitante (o)"
    PROCEDURE }o--|| ORGANIZATION : "solicitante (o)"
    PROCEDURE }o--o| DEPARTMENT : "asignado a"
    PROCEDURE }o--o| APP_USER : "responsable"
    PROCEDURE ||--o{ PROCEDURE_REQUIREMENT : "checklist"
    PROCEDURE ||--o{ PROCEDURE_TASK : tareas
    PROCEDURE ||--o{ PROCEDURE_TRANSITION : historial
    PROCEDURE ||--o{ REVIEW : revisiones
    PROCEDURE ||--o{ INSPECTION : inspecciones
    PROCEDURE ||--o{ DECISION : decisiones
    PROCEDURE ||--o{ ATTACHED_DOCUMENT : adjuntos
    PROCEDURE ||--o{ GENERATED_DOCUMENT : "documentos oficiales"
    PROCEDURE ||--o{ CHARGE : "tasas/liquidaciones"
    PROCEDURE ||--o{ NOTE : notas
    REQUIREMENT_DEF ||--o{ PROCEDURE_REQUIREMENT : "materializa"
```

## 2. Finanzas — cargo → factura → pago → caja

```mermaid
erDiagram
    CHARGE ||--o{ CHARGE_LINE : contiene
    CHARGE }o--o| FEE_RULE : "calculada por (versión)"
    CHARGE_TYPE ||--o{ CHARGE_LINE : clasifica

    INVOICE }o--|| CHARGE : "factura el"
    INVOICE }o--|| ORGANIZATION_UNIT : emisor
    INVOICE }o--o| PERSON : "cliente (o)"
    INVOICE }o--o| ORGANIZATION : "cliente (o)"
    INVOICE }o--|| PROCEDURE : "referencia"
    INVOICE }o--|| NUMBERING_SEQUENCE : numera
    INVOICE ||--o{ INVOICE_LINE : detalle
    INVOICE ||--|| INVOICE_SNAPSHOT : "snapshot histórico"
    INVOICE ||--o{ PAYMENT : recibe
    INVOICE ||--o{ INVOICE_ADJUSTMENT : "ajustes/anulación"

    PAYMENT }o--|| PAYMENT_METHOD : "vía"
    PAYMENT }o--|| CASH_SESSION : "en"
    PAYMENT ||--o{ REFUND : "reembolsa"
    CASH_SESSION }o--|| APP_USER : "cajera"
    CASH_SESSION }o--|| CASH_REGISTER : "de"
    CASH_SESSION ||--o{ CASH_MOVEMENT : registra

    PRINT_JOB }o--|| INVOICE : "imprime/reimprime"
    PRINT_JOB }o--|| PRINTER_PROFILE : "con perfil"
```

Notas:

- `INVOICE.status` ∈ `{DRAFT, ISSUED, PARTIALLY_PAID, PAID, VOIDED, REFUNDED}`; al pasar a `ISSUED` se
  crea `INVOICE_SNAPSHOT` (identidad institución/cliente, líneas, precios, totales, moneda) y se
  consume un número de `NUMBERING_SEQUENCE` de forma segura ante concurrencia.
- Importes en `*_minor INTEGER` + `currency TEXT(3)`. Nunca `REAL`.
- `PRINT_JOB` con `kind ∈ {ORIGINAL, COPY, REPRINT}`; una reimpresión **no** crea `INVOICE` ni
  `PAYMENT` nuevos y queda auditada.

## 3. Documentos y numeración

```mermaid
erDiagram
    NUMBERING_SEQUENCE ||--o{ NUMBERING_ALLOCATION : emite
    DOCUMENT_TEMPLATE ||--o{ OUTPUT_DOCUMENT_DEF : "usada por"
    OUTPUT_DOCUMENT_DEF ||--o{ GENERATED_DOCUMENT : "instancia"
    GENERATED_DOCUMENT }o--|| NUMBERING_SEQUENCE : numera
    GENERATED_DOCUMENT ||--o| VERIFICATION_CODE : "QR/verificación"

    REGISTERED_DOCUMENT }o--|| REGISTRY_BOOK : "libro/volumen"
    REGISTERED_DOCUMENT ||--o{ REGISTERED_DOCUMENT_PARTY : partes
    REGISTERED_DOCUMENT ||--o{ REGISTERED_DOCUMENT_ANNOTATION : anotaciones
    REGISTERED_DOCUMENT ||--o{ CERTIFIED_COPY : "copias certificadas"
    REGISTERED_DOCUMENT }o--o| PROPERTY : "relaciona"
    REGISTERED_DOCUMENT }o--|| NUMBERING_SEQUENCE : "nº de registro"
    REGISTERED_DOCUMENT }o--o| PROCEDURE : "originado en"
```

## 4. Propiedad, contratos y activos

```mermaid
erDiagram
    PROPERTY ||--o{ PROPERTY_OWNERSHIP : titularidad
    PROPERTY_OWNERSHIP }o--o| PERSON : "titular (o)"
    PROPERTY_OWNERSHIP }o--o| ORGANIZATION : "titular (o)"
    PROPERTY ||--o{ AGREEMENT : "arrendamientos/concesiones"
    PROPERTY ||--o{ PROCEDURE : "trámites de planeamiento"
    PROPERTY ||--o{ LEGAL_REFERENCE : referencias

    AGREEMENT ||--o{ AGREEMENT_PARTY : partes
    AGREEMENT ||--o{ AGREEMENT_CHARGE_SCHEDULE : "cuotas periódicas"
    AGREEMENT }o--o| MARKET_STALL : "objeto (o)"
    AGREEMENT }o--o| CEMETERY_UNIT : "objeto (o)"
    AGREEMENT }o--o| PROPERTY : "objeto (o)"

    ASSET ||--o{ WORK_ORDER : "órdenes"
    ASSET }o--|| ADDRESS : "ubicado en"
    WORK_ORDER }o--o| CREW : "asignada a"
```

## 5. Módulos especializados (selección)

```mermaid
erDiagram
    CEMETERY ||--o{ CEMETERY_SECTION : contiene
    CEMETERY_SECTION ||--o{ CEMETERY_BLOCK : contiene
    CEMETERY_BLOCK ||--o{ CEMETERY_UNIT : "lote/espacio/nicho"
    CEMETERY_UNIT ||--o{ BURIAL : inhumaciones
    CEMETERY_UNIT ||--o{ EXHUMATION : exhumaciones
    BURIAL }o--o| PERSON : "fallecido/a"

    MARKET ||--o{ MARKET_ZONE : contiene
    MARKET_ZONE ||--o{ MARKET_STALL : contiene
    MARKET_STALL ||--o{ MARKET_OCCUPANCY : ocupaciones
    MARKET_OCCUPANCY }o--|| PERSON : comerciante
    MARKET_OCCUPANCY ||--o{ ARREARS_ITEM : morosidad

    PERMIT }o--|| PROCEDURE : "gestionado por"
    PERMIT ||--o{ PERMIT_CONDITION : condiciones
    PERMIT ||--o{ INSPECTION : inspecciones
    PERMIT }o--o| PROPERTY : "sobre (o)"
    PERMIT }o--o| ORGANIZATION : "negocio (o)"

    CITIZEN_REQUEST }o--|| PROCEDURE : "es una variante de"
    CITIZEN_REQUEST }o--o| DEPARTMENT : asignada
    CITIZEN_REQUEST }o--o| ASSET : "sobre activo"
    CITIZEN_REQUEST ||--o{ REQUEST_UPDATE : seguimiento
```

## 6. Auditoría, configuración y migraciones

```mermaid
erDiagram
    AUDIT_EVENT }o--o| APP_USER : "actor"
    AUDIT_EVENT {
        string id PK
        string when_at "ISO-8601 UTC"
        string action
        string entity_type
        string entity_id
        string before_json
        string after_json
        string reason
        string session_id
        string source
    }

    BUSINESS_RULE_VERSION {
        string id PK
        string rule_kind "FEE|REQUIREMENTS|WORKFLOW|APPROVAL_ROLE|VALIDITY"
        string ref_id
        int version
        string effective_from
        string effective_to
        string payload_json
    }

    SCHEMA_MIGRATION {
        int version PK
        string description
        string checksum
        string applied_at
        int success
    }

    APP_SETTING {
        string key PK
        string value_json
        string classification "PUBLIC|INTERNAL|CONFIDENTIAL|RESTRICTED"
        string updated_at
    }
```

## 7. Estado de archivo (transversal)

Muchas entidades llevan un `archive_status` ∈ `{ACTIVE, COMPLETED, CLOSED, ARCHIVED, VOID,
CANCELLED}`. **No** se hace `DELETE` de registros con valor legal/financiero.

## 8. Pendiente de decidir en Fase 3

- PK `TEXT` UUIDv7 vs `INTEGER` por tabla (documentar por tabla).
- Estrategia exacta de almacenamiento de configuración flexible (columnas tipadas + JSON validado).
- Adopción de Flyway Community vs runner propio de migraciones.
- Modelo de adjuntos: BLOB en SQLite vs ficheros en disco referenciados (probable: ficheros +
  hash + ruta relativa a la carpeta de datos).
