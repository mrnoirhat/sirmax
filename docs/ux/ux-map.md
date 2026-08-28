# Mapa de UX de SIRMAX

Guía de experiencia para la Fase 2 (shell + Design System) y siguientes. El estándar es el del master
prompt: **una persona no técnica debe entender qué hacer sin preguntar a nadie**.

## 1. Principios

- Claridad sobre densidad. Jerarquía visual calmada. Acción primaria obvia.
- Acciones reversibles siempre que se pueda; confirmaciones con significado; errores amables.
- Formularios cortos; divulgación progresiva.
- Soporte de teclado y accesibilidad en todo.
- Patrones consistentes. Nunca infantil, burocrático, recargado ni ruidoso.
- El operador **no** elige términos técnicos (`aggregate`, `entity`, `workflow definition`,
  `transaction type`, `state machine`). Esa jerga vive en la documentación de desarrollo.

## 2. Navegación: primero la tarea, después el departamento

Pantalla de inicio con la pregunta central:

```text
¿Qué necesitas hacer?

[ Registrar un trámite ]      [ Emitir una certificación ]
[ Registrar un pago ]         [ Registrar un documento ]
[ Registrar una solicitud/queja ]   [ Gestionar un contrato ]
```

La navegación por departamento existe, pero es secundaria.

### Estructura del shell

```text
┌───────────────────────────────────────────────────────────────┐
│  ⌂ Inicio   🔍 Búsqueda global (Ctrl+K)      👤 Usuaria · Caja  │  ← barra superior
├──────────────┬────────────────────────────────────────────────┤
│ Tareas       │                                                │
│  Trámites    │            Área de contenido                   │
│  Facturación │   (dashboard por rol / lista / detalle /        │
│  Caja        │    asistente)                                   │
│  Documentos  │                                                │
│  Ciudadanos  │                                                │
│  ───────     │                                                │
│ Departamentos│                                                │
│ Configuración│                                                │
│  Reportes    │                                                │
└──────────────┴────────────────────────────────────────────────┘
```

## 3. Dashboard por rol

No es un muro de gráficos. Responde: **¿Qué necesita mi atención? ¿Qué hago después? ¿Qué pasó hoy?**

| Rol | Ve primero |
| --- | --- |
| Cajera | Caja abierta/cerrada, cobros del día, arqueo, "Registrar pago", "Cerrar caja" |
| Operador | Sus trámites: urgentes, vencidos, hoy, esta semana; "Nuevo trámite" |
| Supervisor | Cuellos de botella, trámites vencidos por departamento, aprobaciones pendientes |
| Administrador | Salud del sistema, último backup, configuración pendiente, usuarios |

Cola de trabajo (worklist):

```text
Mis pendientes
  Urgente      2
  Vencidos     4
  Hoy          7
  Esta semana 12      ← al hacer clic, filtra la lista
```

## 4. Recorrido del front-office (el más importante)

```text
1. Buscar persona          → si no existe, alta rápida con detección de duplicados
2. Elegir servicio         → navegación service-first, con buscador
3. Completar solicitud     → formulario corto del servicio
4. Validar requisitos      → checklist visible (ver §5)
5. Cobrar si aplica        → total, método, pago; el sistema calcula el cambio
6. Generar recibo/documento→ imprimir / guardar PDF
7. Finalizar               → confirmación clara + próximas acciones
```

Clics mínimos para tareas comunes. Tareas de un clic desde el inicio: _Nuevo trámite, Nueva factura,
Registrar pago, Buscar ciudadano, Emitir certificación, Buscar documento, Abrir caja, Cerrar caja,
Crear backup_.

## 5. Checklist de requisitos (patrón obligatorio)

Cada trámite muestra por qué no puede avanzar:

```text
Solicitud de licencia de construcción

Requisitos
  ✓ Identificación
  ✓ Documento de propiedad
  ✕ Planos
  ✕ Comprobante de pago

  2 pendientes

  [ Subir documento ]   [ Registrar pago ]
```

El operador nunca adivina.

## 6. Recibo / factura tras el pago

```text
Pago registrado correctamente.

Recibo:  RC-2026-000128
Total:   RD$ 1,250.00

[ Imprimir recibo ] [ Imprimir factura ] [ Guardar PDF ]
[ Ver trámite ] [ Ver ciudadano ] [ Nuevo trámite ]
```

Impresión: botón **Imprimir** que imprime de verdad (integración Windows), sin exportar imágenes a
mano. Dos modelos: **A** angosta/mostrador (58/80 mm) y **B** oficina **US Letter**. Reimpresión
autorizada marcada como `COPIA`, sin duplicar factura ni pago, auditada.

## 7. Estados de cada pantalla

Toda vista define y diseña sus estados: **loading**, **empty** (con acción para empezar), **error**
(mensaje amable + reintento + detalle técnico oculto/colapsado), **success**.

## 8. SLA y vencimientos

El dashboard muestra "4 trámites vencidos"; al hacer clic, filtra la lista de casos.

## 9. Búsqueda global

`Ctrl+K` abre la búsqueda. Resultados **categorizados**: persona, organización, trámite, factura,
pago, certificación, documento registrado, propiedad, contrato, registro de cementerio, casilla de
mercado, solicitud/queja.

## 10. Design System (Fase 2) — inventario mínimo

Tema (claro; oscuro opcional a futuro), tipografía, color con contraste validado (nunca el color como
único indicador semántico), botones (primario/secundario/peligro/terciario), inputs y validación,
tablas con paginación y estados, diálogos, notificaciones/toasts, banners, tabs, wizard/stepper,
tarjetas de dashboard, barra de búsqueda, breadcrumbs, menús, tooltips, skeleton loaders. Atajos de
teclado documentados y visibles.

## 11. Idioma y accesibilidad

Español (contexto dominicano) primero; arquitectura i18n lista para inglés y francés. Sin texto de
usuario quemado en servicios de dominio. Navegación por teclado completa, foco visible, objetivos de
clic amplios, mensajes de error asociados a su campo.

## 12. Test mental por pantalla

> **¿Puede una persona no técnica entender qué hacer aquí sin preguntar a quien desarrolla?**
> Si no, se simplifica.
