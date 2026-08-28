# Contribuir a SIRMAX

Gracias por tu interés en **SIRMAX — Sistema Integral de Registros Municipales y Administración
eXtensible**. Este documento explica cómo trabajar en el proyecto de forma que otra persona pueda
continuar tu trabajo de inmediato.

- Código de conducta: [`CODE_OF_CONDUCT.md`](./CODE_OF_CONDUCT.md)
- Arquitectura: [`ARCHITECTURE.md`](./ARCHITECTURE.md)
- Entorno de desarrollo: [`DEVELOPMENT.md`](./DEVELOPMENT.md)
- Base de datos y migraciones: [`DATABASE.md`](./DATABASE.md)
- Política de release: [`RELEASE.md`](./RELEASE.md)
- Estado por fases: [`ROADMAP.md`](./ROADMAP.md)

---

## 1. Principios

1. **Prefiere una configuración a una suposición municipal quemada en el código.**
2. **Prefiere un núcleo de flujo compartido antes que módulos duplicados.**
3. **Local-first.** Internet nunca es una condición de error.
4. **Simplicidad para el operador.** Si una pantalla necesita explicación de una persona desarrolladora,
   simplifícala.
5. **Auditabilidad.** Los eventos financieros, legales y de configuración se auditan.
6. **Preserva el histórico.** Nunca reescribas silenciosamente registros financieros o legales.
7. **No declares cumplimiento legal sin evidencia.**
8. **Documenta las decisiones importantes** con un ADR en [`docs/adr/`](./docs/adr/).

Una funcionalidad **no** está completa solo porque su UI existe. Ver la
[Definición de Hecho](#8-definición-de-hecho-definition-of-done).

---

## 2. Modelo de ramas (obligatorio)

Existen **exactamente tres ramas permanentes**:

```text
feature/*  ─▶  experiment  ─▶  testing  ─▶  main
```

| Rama | Propósito | Estabilidad |
| --- | --- | --- |
| `experiment` | Desarrollo activo y experimentación controlada | Puede ser inestable |
| `testing` | Integración, QA, regresión, empaquetado, impresión, backup, seguridad | Release candidate |
| `main` | Solo producción estable | Siempre estable |

Reglas:

- Toda rama de trabajo parte de `experiment` **actualizado**.
- **Nunca** se hace merge de una `feature/*` directamente a `main`.
- La promoción a `main` ocurre **solo** vía `testing`.
- `testing` y `main` están protegidas: requieren Pull Request + CI en verde + revisión.

Prefijos de ramas temporales:

```text
feature/   fix/   refactor/   ux/   docs/   chore/   perf/   ci/
```

Ejemplo: `feature/cemetery-space-management`.

---

## 3. Flujo diario

```text
1.  git checkout experiment && git pull
2.  Lee los docs / ADR relevantes.
3.  Inspecciona el código existente ANTES de modificarlo.
4.  git checkout -b feature/mi-cambio
5.  Implementa un cambio pequeño y coherente.
6.  Ejecuta las pruebas y corrige fallos.
7.  Actualiza la documentación afectada.
8.  Commit con Conventional Commits.
9.  Abre un PR hacia `experiment`.
10. Cuando un hito coherente está listo: promoción experiment → testing.
11. Estabiliza `testing`.
12. Promoción testing → main para release.
```

No hagas commits enormes y opacos.

---

## 4. Convención de commits — Conventional Commits

```text
<tipo>(<ámbito opcional>): <resumen en imperativo, minúscula, sin punto final>
```

Tipos permitidos:

```text
feat   fix   docs   refactor   test   perf   chore   build   ci
```

Ámbitos habituales: `desktop`, `landing`, `docs`, `domain`, `billing`, `printing`, `backup`,
`security`, `db`, `ci`, `repo`.

Ejemplos:

```text
feat(billing): add configurable invoice numbering sequences
fix(printing): prevent duplicate receipt numbers on reprint
docs(adr): record decision to use SQLite for local-first storage
test(billing): cover partial payment reconciliation
```

Un cambio con impacto incompatible añade `!` (`feat(db)!: ...`) y una sección `BREAKING CHANGE:` en el
cuerpo.

---

## 5. Pull Requests

Toda PR debe explicar, usando la plantilla del repositorio:

- **Qué** cambió;
- **Por qué**;
- **Cómo se probó**;
- **Impacto en UX**;
- **Impacto en base de datos** (¿hay migración?);
- **Impacto en seguridad**;
- **Impacto en documentación**.

Requisitos de CI para fusionar (según rama): build de escritorio, pruebas automatizadas, análisis
estático, build de documentación, build de landing y comprobaciones de seguridad/dependencias donde
estén configuradas.

---

## 6. Estilo de código

- **Java**: sigue [`.editorconfig`](./.editorconfig). El dominio (`sirmax-domain`) **no** depende de
  JavaFX ni de infraestructura. La UI **no** ejecuta SQL directamente. Ver [`ARCHITECTURE.md`](./ARCHITECTURE.md).
- **Dinero**: nunca uses coma flotante. Usa el tipo `Money` del módulo `sirmax-shared`.
- **Texto de usuario**: nunca se quema en servicios de dominio. Usa claves de i18n (español primero).
- **TypeScript/React** (landing): `npm run lint` debe pasar; HTML semántico y accesible.
- **Markdown**: una idea por línea larga está bien; mantén los enlaces entre documentos vivos.

---

## 7. Pruebas

- Toda lógica de dominio y de aplicación nueva llega con pruebas.
- Áreas con pruebas obligatorias: numeración de facturas, totales, descuentos, pagos parciales,
  reembolsos, anulaciones, reimpresión sin duplicar, generación de PDF, impresión angosta y Letter,
  render de marca, snapshots históricos, permisos y auditoría.
- Migraciones de base de datos con pruebas sobre base nueva y base actualizada.

---

## 8. Definición de Hecho (Definition of Done)

Una funcionalidad está hecha cuando es:

```text
funcional
+ probada
+ suficientemente segura para su alcance
+ auditada cuando corresponde
+ documentada
+ accesible
+ con buen rendimiento
+ recuperable (backup/restore no se rompe)
+ visualmente consistente
+ integrada en el recorrido real del usuario
+ compatible con el empaquetado cuando aplica
```

---

## 9. ADRs

Si tu cambio toma una decisión de arquitectura significativa (nueva dependencia estructural, cambio de
límites entre capas, nuevo motor, formato de datos), añade un ADR:

```bash
cp docs/adr/0000-adr-template.md docs/adr/00NN-titulo-corto.md
```

Enlázalo desde el índice [`docs/adr/README.md`](./docs/adr/README.md).

---

## 10. Licencia de las contribuciones

Al contribuir aceptas que tu aportación se distribuya bajo **AGPL-3.0-or-later**, la licencia del
proyecto. La AGPL **no** te obliga a enviar tus cambios a este repositorio; establece condiciones de
copyleft para el uso y la distribución del software cubierto. Cualquier requisito futuro de CLA o
cesión se gestionará por separado y con revisión legal.
