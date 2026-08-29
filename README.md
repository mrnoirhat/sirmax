<div align="center">

# SIRMAX

**Sistema Integral de Registros Municipales y Administración eXtensible**

_La gestión municipal, simplificada._

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](./LICENSE)
[![Desktop CI](https://github.com/mrnoirhat/sirmax/actions/workflows/desktop.yml/badge.svg?branch=main)](https://github.com/mrnoirhat/sirmax/actions/workflows/desktop.yml)
[![Landing CI](https://github.com/mrnoirhat/sirmax/actions/workflows/landing.yml/badge.svg?branch=main)](https://github.com/mrnoirhat/sirmax/actions/workflows/landing.yml)
[![Docs CI](https://github.com/mrnoirhat/sirmax/actions/workflows/docs.yml/badge.svg?branch=main)](https://github.com/mrnoirhat/sirmax/actions/workflows/docs.yml)
[![Última versión](https://img.shields.io/github/v/release/mrnoirhat/sirmax?label=descargar&color=2563eb)](https://github.com/mrnoirhat/sirmax/releases/latest)

**[Sitio web](https://sirmax.vercel.app) · [Documentación](https://sirmax-docs.vercel.app) · [Descargar para Windows](https://github.com/mrnoirhat/sirmax/releases/latest) · [Issues](https://github.com/mrnoirhat/sirmax/issues) · [Contribuir](./CONTRIBUTING.md) · [Roadmap](./ROADMAP.md)**

_La documentación también está en [GitHub Pages](https://mrnoirhat.github.io/sirmax)._

</div>

---

## Qué es SIRMAX

SIRMAX es una plataforma **open source, local-first y orientada a municipios** para gestionar en un solo
lugar servicios, trámites, registros ciudadanos, expedientes, documentos, **facturación**, pagos, caja,
**impresión física de facturas y recibos**, reportes, auditoría y copias de seguridad.

Diseñada inicialmente para **ayuntamientos y distritos municipales de República Dominicana**, su
arquitectura está preparada para adaptarse a diferentes municipios, países, monedas, normativas y modelos
administrativos sin reescribir el núcleo del producto.

La **X** de SIRMAX representa **extensibilidad, tecnología, integración y evolución** más allá de un
catálogo fijo de formularios.

### El principio de diseño no negociable

> **El producto debe ser más fácil de operar para un empleado municipal no técnico
> de lo que es de explicar para una persona desarrolladora.**

El operador debe poder entender el siguiente paso sin saber nada de bases de datos, flujos de trabajo,
APIs, entidades, SQL, Git, Java, OAuth o infraestructura en la nube.

---

## El bucle operativo municipal

SIRMAX no es una aplicación de facturación genérica. Su dominio organiza el ciclo de vida completo de un
servicio o registro municipal, y **no todo trámite se paga**:

```text
Ciudadano / Entidad → Solicitud → Trámite → Requisitos → Documentos → Revisión / Inspección
      → Decisión → Tasa / Liquidación → Factura → Pago → Recibo / Documento oficial
      → Entrega → Auditoría → Archivo
```

Un trámite puede ser `GRATUITO`, `CON_TASA`, `TASA_CONDICIONAL` o `PAGO_EXTERNO`.

---

## Características principales

- Gestión central de **ciudadanos y organizaciones** (sin duplicar la persona en cada trámite).
- **Catálogo configurable** de servicios municipales (un administrador puede definir un servicio nuevo
  sin desarrollador cuando es razonable).
- **Motor de trámites y expedientes** con estructura compartida.
- **Motor de requisitos** con checklist visible: el operador nunca adivina por qué un trámite no avanza.
- **Motor de flujo de trabajo** pragmático (pasos, aprobaciones, devoluciones, SLA).
- **Motor de tasas** versionable (importe fijo, cantidad × tarifa, por área, por duración, por categoría…).
- **Facturación de primera clase**: cargo → factura → pago → recibo, sin salir de la aplicación.
- **Impresión física obligatoria** en dos modelos profesionales:
  - **Modelo A** — impresora angosta / de mostrador (58 mm, 80 mm, ancho configurable).
  - **Modelo B** — factura de oficina estándar en **US Letter 8.5 × 11"** (arquitectura lista para A4).
- **Marca institucional** en facturas y documentos: logo, nombre, colores, RNC configurable, QR/verificación.
- **Registro de documentos** distinto de los adjuntos genéricos (libro, folio, número de registro, custodia).
- **Certificaciones y documentos oficiales** con plantillas, numeración y política de reimpresión.
- Módulos municipales especializados sobre el mismo núcleo: cementerios, mercados, planeamiento urbano,
  catastro/propiedades, espacio público/movilidad, solicitudes y quejas ciudadanas.
- **Inspecciones**, **decisiones y aprobaciones**, **SLA y vencimientos**.
- **Funcionamiento offline / local-first**: internet nunca es una condición de error.
- **Copias de seguridad locales** y, opcionalmente, en **Google Drive** (la cuenta del usuario es la dueña).
- **Auditoría completa**: quién, cuándo, qué, objeto, valores antes/después, motivo, sesión.
- **Usuarios, roles y permisos (RBAC)**.
- **Búsqueda global** categorizada y navegación orientada a tareas.
- Arquitectura preparada para **múltiples municipios, países y una futura API/nube**.

---

## Tecnología

| Área | Stack |
| --- | --- |
| **Escritorio** (plataforma principal: Windows) | Java 25 LTS · JavaFX · SQLite · Gradle · jpackage |
| **Landing pública** | Next.js · React · TypeScript · Vercel |
| **Documentación** | Docusaurus · Markdown · Mermaid |
| **CI/CD** | GitHub Actions |
| **Licencia del código** | GNU AGPL-3.0-or-later |

La aplicación de escritorio **no es Electron** y **no requiere** que el usuario final instale Java aparte.

Decisiones de arquitectura documentadas en [`docs/adr/`](./docs/adr/).

---

## Estructura del monorepo

```text
sirmax/
├── apps/
│   ├── desktop/     # Aplicación Windows (Java + JavaFX, multi-módulo Gradle)
│   ├── landing/     # Sitio público (Next.js) optimizado para Vercel
│   └── docs/        # Documentación (Docusaurus)
├── backend/         # Frontera para una futura API/nube (aún no requerida para 1.0)
├── database/        # Migraciones SQL y recursos de base de datos
├── scripts/         # Utilidades de build / release / mantenimiento
├── docs/
│   ├── adr/         # Architecture Decision Records
│   ├── domain/      # Glosario, mapa de dominio, ERD, mapa de módulos
│   └── ux/          # Mapa de experiencia de usuario
└── .github/         # Workflows de CI, plantillas de issues y PR
```

---

## Empezar (desarrollo)

Requisitos: **JDK 25**, **Node.js ≥ 20.11**, **Git**. Detalle completo en [`DEVELOPMENT.md`](./DEVELOPMENT.md).

```bash
# 1. Clonar y situarse en la rama de desarrollo
git clone https://github.com/mrnoirhat/sirmax.git
cd sirmax
git checkout experiment

# 2. Aplicación de escritorio
cd apps/desktop
./gradlew build           # compila y ejecuta las pruebas
./gradlew :sirmax-app:run # lanza el shell de la aplicación

# 3. Web (landing + documentación), desde la raíz del repo
npm install
npm run landing:dev       # http://localhost:3000
npm run docs:dev          # http://localhost:3001
```

> En Windows usa `gradlew.bat` en lugar de `./gradlew`.

---

## Modelo de ramas

SIRMAX usa **exactamente tres ramas permanentes**. La promoción a `main` ocurre **solo** a través de `testing`.

```text
feature/*  ─▶  experiment  ─▶  testing  ─▶  main
```

- **`experiment`** — desarrollo activo y experimentación controlada. Puede ser inestable.
- **`testing`** — integración, QA, migraciones, empaquetado, impresión, backup y seguridad.
- **`main`** — solo código estable de producción. Sin desarrollo directo.

Ramas temporales: `feature/*`, `fix/*`, `refactor/*`, `ux/*`, `docs/*`, `chore/*`, parten de `experiment`.
Detalle en [`CONTRIBUTING.md`](./CONTRIBUTING.md) y [`RELEASE.md`](./RELEASE.md).

---

## Estado del proyecto

SIRMAX está en construcción por fases. El estado vive en [`ROADMAP.md`](./ROADMAP.md).

| Fase | Descripción | Estado |
| --- | --- | --- |
| 0 | Discovery, auditoría de repo y arquitectura | ✅ Completada |
| 1 | Fundación del repositorio | ✅ Completada |
| 2 | Shell de escritorio y Design System | ✅ Completada |
| 3 | Dominio central y base de datos | ✅ Completada |
| … | … | ⚪ Pendiente |
| 14 | Release 1.0 | ⚪ Pendiente |

---

## Documentación del repositorio

| Documento | Contenido |
| --- | --- |
| [`ARCHITECTURE.md`](./ARCHITECTURE.md) | Capas, límites, módulos y decisiones estructurales |
| [`DEVELOPMENT.md`](./DEVELOPMENT.md) | Entorno, build, pruebas y flujo diario |
| [`DATABASE.md`](./DATABASE.md) | Esquema, migraciones, dinero, backups e integridad |
| [`BACKUP.md`](./BACKUP.md) | Estrategia de copia de seguridad y restauración |
| [`RELEASE.md`](./RELEASE.md) | Gate de release, promoción de ramas y artefactos |
| [`ROADMAP.md`](./ROADMAP.md) | Fases de entrega y su estado |
| [`CONTRIBUTING.md`](./CONTRIBUTING.md) | Cómo contribuir, commits y PRs |
| [`SECURITY.md`](./SECURITY.md) | Política de seguridad y divulgación responsable |
| [`SUPPORT.md`](./SUPPORT.md) | Dónde pedir ayuda |
| [`CODE_OF_CONDUCT.md`](./CODE_OF_CONDUCT.md) | Código de conducta de la comunidad |
| [`TRADEMARK_POLICY.md`](./TRADEMARK_POLICY.md) | Uso del nombre y el logo SIRMAX |
| [`THIRD_PARTY_LICENSES.md`](./THIRD_PARTY_LICENSES.md) | Inventario de licencias de dependencias |
| [`CHANGELOG.md`](./CHANGELOG.md) | Historial de cambios |

---

## Aviso legal

> SIRMAX ofrece flujos administrativos configurables. Los requisitos legales, fiscales, tributarios,
> archivísticos y regulatorios específicos de cada municipio deben ser revisados y configurados por
> personal cualificado antes de su uso en producción. El proyecto **no** declara cumplimiento legal
> o tributario automático.

---

## Licencia

Código bajo **[GNU Affero General Public License v3.0 or later](./LICENSE)** (`AGPL-3.0-or-later`).

El nombre y el logo **SIRMAX** están reservados: ver [`TRADEMARK_POLICY.md`](./TRADEMARK_POLICY.md).
La AGPL establece condiciones de copyleft; **no** obliga a enviar tus cambios al repositorio original.

---

<div align="center">

**SIRMAX — La gestión municipal, simplificada.**

</div>
