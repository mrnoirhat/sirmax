# Glosario de dominio de SIRMAX

Vocabulario compartido entre producto, dominio y documentación. La columna **Concepto interno
neutral** es el término que usa el código (`domain`); la columna **UI (es-DO)** es lo que ve la
persona operadora en República Dominicana.

## Organización y personas

| Concepto interno neutral | UI (es-DO) | Definición |
| --- | --- | --- |
| Local Government Organization | Ayuntamiento / Institución | Organización de gobierno local que opera SIRMAX. Neutral para soportar otros países. |
| Municipality | Municipio | Municipio o distrito municipal. |
| Department | Departamento | Unidad organizativa interna (Planeamiento, Registro Civil, Caja…). Un servicio **no** pertenece obligatoriamente a un único departamento. |
| User | Usuario | Cuenta de operación con credenciales y roles. |
| Role | Rol | Conjunto de permisos (RBAC). Ej.: Cajera, Operador, Supervisor, Administrador. |
| Permission | Permiso | Autorización atómica para una acción (`invoice.void`, `service.configure`…). |
| Person | Persona | Ser humano registrado una sola vez; puede aparecer como solicitante, propietario, comerciante, pagador, fallecido, representante, testigo… |
| Organization / Business | Organización / Negocio | Entidad no-persona (empresa, junta de vecinos, institución). |
| Party | Parte | Rol de una persona/organización en un contexto concreto (parte de un documento, propietario de una parcela…). |
| Identification | Identificación | Documento identificativo (cédula, RNC, pasaporte…). Tipo configurable por país. |
| Address / Location | Dirección / Ubicación | Ubicación normalizada: municipio, sector, barrio, calle, número, referencia, código postal, lat/long. |
| Contact | Contacto | Teléfono, email, etc. |

## Servicios y trámites

| Concepto interno neutral | UI (es-DO) | Definición |
| --- | --- | --- |
| Service Catalog | Catálogo de servicios | Colección de servicios que ofrece la institución. |
| Service Definition | Servicio / Definición de servicio | Configuración versionada de un servicio: requisitos, formulario, flujo, tasas, plantillas, SLA, numeración, validez. |
| Service Type | Tipo de servicio (pago) | `GRATUITO`, `CON_TASA`, `TASA_CONDICIONAL`, `PAGO_EXTERNO`. |
| Requirement | Requisito | Elemento necesario para avanzar: documento, campo, verificación de identidad, pago, inspección, aprobación, firma, referencia externa, evidencia. `required`/`conditional`/`stage`/`validation`. |
| Form Schema | Formulario | Campos configurables de captura de datos del trámite. |
| Procedure / Case | Trámite / Expediente / Caso | Instancia de un servicio para una persona/organización. Estructura compartida (código, servicio, solicitante, activo relacionado, fechas, estado, prioridad, departamento, usuario, SLA, requisitos, documentos, tareas, revisiones, inspecciones, tasas, pagos, decisiones, documentos generados, notas, historial, auditoría). |
| Workflow Definition | Flujo de trabajo | Lista ordenada de pasos con tipo, rol, SLA y transiciones. |
| Step | Paso | Unidad del flujo: `TASK`, `REVIEW`, `APPROVAL`, `INSPECTION`, `PAYMENT_CHECKPOINT`, `DOCUMENT_OUTPUT`. |
| Transition | Transición | `ADVANCE`, `APPROVE`, `REJECT`, `RETURN_FOR_CORRECTION`, `REASSIGN`, `CANCEL`. |
| Decision | Decisión | Resultado formal: `APPROVED`, `REJECTED`, `RETURNED_FOR_CORRECTION`, `CONDITIONALLY_APPROVED`, `EXPIRED`, `CANCELLED`. Guarda quién, rol, fecha, motivo, comentarios, documento firmado. |
| Inspection | Inspección | Visita/verificación: nº, trámite, inspector, fecha prevista/real, ubicación, checklist, hallazgos, adjuntos, resultado, seguimiento. |
| SLA / Deadline | Plazo / Vencimiento | Tiempo objetivo (días hábiles o naturales), fecha de expiración, umbral de escalado. |
| Task | Tarea | Trabajo asignable dentro de un trámite. |
| Assignment | Asignación | Departamento y/o usuario responsable de un trámite o tarea. |

## Documentos y registros

| Concepto interno neutral | UI (es-DO) | Definición |
| --- | --- | --- |
| Attached Document | Documento adjunto | Archivo aportado a un trámite. **No** implica registro oficial. |
| Registered Document | Documento registrado | Documento con valor oficial en el Registro de Documentos / Conservaduría: tipo, partes, fecha del documento, fecha de presentación, número de registro, libro/volumen, folio, estado, tasas, copias certificadas, anotaciones, propiedad/personas relacionadas, escaneo, cadena de custodia. |
| Certificate / Official Document | Certificación / Documento oficial | Salida generada (certificado, permiso, carta) con plantilla, variables, numeración, firma, validez, QR/verificación, política de reimpresión y de corrección/anulación. |
| Document Template | Plantilla de documento | Maqueta con marca institucional y variables. |
| Numbering Sequence | Secuencia de numeración | Serie independiente por tipo (`FACT-`, `TRM-`, `CERT-RES-`, `PER-URB-`, `REG-`…): única, segura ante concurrencia, sin reutilización, prefijo/serie/año/padding/reinicio configurables. |
| Verification Code / QR | Código de verificación / QR | Dato para verificación pública futura (código, QR, sello temporal, tipo, emisor). |
| Data Classification | Clasificación de datos | `PUBLIC`, `INTERNAL`, `CONFIDENTIAL`, `RESTRICTED`. |

## Finanzas

| Concepto interno neutral | UI (es-DO) | Definición |
| --- | --- | --- |
| Fee Rule | Regla de tasa | Cómo se calcula un cargo: `FIXED`, `QUANTITY_X_RATE`, `AREA_BASED`, `DURATION_BASED`, `CATEGORY_BASED`, `LOCATION_BASED`, `TIERED`, `PERIODIC`. Versionada, con vigencia, moneda y referencia legal. |
| Charge Type | Tipo de cargo | Taxonomía configurable: `IMPUESTO`, `ARBITRIO`, `TASA`, `CONTRIBUCIÓN`, `CARGO_SERVICIO`, `ARRENDAMIENTO`, `RECARGO`. |
| Charge / Liquidation | Cargo / Liquidación | Importe(s) calculado(s) para un trámite antes de facturar. Entrada del módulo de facturación. |
| Invoice | Factura | Entidad con número público, serie, período, emisor, cliente, trámite/servicio, líneas, subtotal, descuentos, cargos, total, pagado, balance, moneda, estado de pago, estado de factura, cajera, sesión de caja, notas, anulación, auditoría. |
| Invoice Status | Estado de factura | `DRAFT`, `ISSUED`, `PARTIALLY_PAID`, `PAID`, `VOIDED`, `REFUNDED`. |
| Invoice Line | Línea de factura | Concepto, descripción, cantidad, unidad, precio unitario, descuento, recargo, total de línea, categoría de cargo. |
| Payment | Pago | Movimiento con importe, fecha/hora, método, referencia, pagador, factura, trámite, cajera/sesión, notas, estado, auditoría. |
| Payment Method | Método de pago | `CASH`, `BANK_TRANSFER`, `CARD`, `CHECK`, `OTHER` + métodos definidos por el municipio. |
| Cash Session / Drawer | Sesión de caja / Caja | Apertura/cierre de caja de una cajera; base para conciliación. |
| Refund | Reembolso | Devolución autorizada de dinero ya pagado. |
| Void | Anulación | Anulación de una factura sin pago efectivo o con reverso auditado. |
| Adjustment | Ajuste | Corrección controlada que no reescribe el histórico. |
| Receipt | Recibo | Comprobante de pago. |
| Historical Financial Snapshot | Snapshot financiero histórico | Copia de identidad de institución/cliente, líneas, precios, totales y moneda al emitir; inmutable. |

## Propiedad, contratos y activos

| Concepto interno neutral | UI (es-DO) | Definición |
| --- | --- | --- |
| Property / Parcel | Propiedad / Parcela | Registro reusable: identificador de parcela, ubicación, propietario(s), estado de propiedad municipal, referencias legales, contratos, trámites de planeamiento, arrendamientos, certificaciones, historial. |
| Agreement / Lease / Concession | Contrato / Arrendamiento / Concesión | Modelo genérico: partes, objeto, fechas inicio/fin, renovación, importe, frecuencia de pago, estado, traspaso, terminación, documentos. Aplica a propiedad municipal, casillas de mercado, terrenos/nichos de cementerio, uso temporal de espacio público. |
| Asset | Activo | Bien municipal (parque, luminaria, mobiliario…): ubicación, tipo, condición, solicitudes de servicio, órdenes de trabajo, cuadrilla responsable, historial de mantenimiento. |
| Work Order | Orden de trabajo | Trabajo operativo sobre un activo o zona. |

## Módulos especializados

| Concepto interno neutral | UI (es-DO) | Definición |
| --- | --- | --- |
| Cemetery | Cementerio | Jerarquía `Cementerio → Sección → Manzana/Bloque → Lote/Espacio/Nicho` con propiedad/arrendamiento/concesión, ocupante/fallecido, contratos, inhumaciones, exhumaciones, pagos y estado de disponibilidad visual. |
| Market | Mercado | `Mercado → Edificio/Zona → Casilla` con comerciante, negocio, acuerdo de ocupación, tasa periódica, morosidad, inspección, estado, reasignación, traspaso, terminación. |
| Permit / License | Permiso / Licencia | Motor genérico de permiso+inspección: sujeto (negocio/propiedad), actividad, ubicación, dimensiones/aforo, validez, renovación, inspección, condiciones, cálculo de tasa, flujo de aprobación, documento de permiso. |
| Citizen Request / Case | Solicitud / Queja ciudadana | Caso no necesariamente financiero: solicitante, canal (`PRESENTIAL`, `PHONE`, `EMAIL`, `WEB`, `WHATSAPP`, `OTHER`), categoría, descripción, adjuntos, departamento, responsable, fecha límite, estado, respuesta, motivo de cierre, auditoría. |

## Transversal

| Concepto interno neutral | UI (es-DO) | Definición |
| --- | --- | --- |
| Audit Event | Evento de auditoría | Registro inmutable: quién, cuándo, qué, objeto, valores antes/después, motivo, sesión/dispositivo/origen. |
| Institution Profile / Branding | Perfil de institución / Marca | Nombre, nombre corto, municipio, provincia, país, logo(s), colores (primario/secundario/acento/texto/fondo), dirección, teléfono, email, web, RNC/identificador, pie de factura, texto de cabecera, ajustes de QR/verificación. |
| Business Rule Version | Versión de regla de negocio | Instantánea de una regla (tasa, requisitos, flujo, rol de aprobación, validez) para interpretar trámites antiguos. |
| Archive Status | Estado de archivo | `ACTIVE`, `COMPLETED`, `CLOSED`, `ARCHIVED`, `VOID`, `CANCELLED`. |
| Country Adapter | Adaptador de país | Frontera con reglas específicas: identidad, moneda, reglas municipales/documentales/de tasas. |
| Money | Money | Importe en unidad mínima (entero) + moneda ISO 4217. **Nunca** coma flotante. |
