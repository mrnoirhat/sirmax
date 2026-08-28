# Gobernanza de release de SIRMAX

## 1. Ramas permanentes

```text
feature/*  ─▶  experiment  ─▶  testing  ─▶  main
```

| Rama | Propósito | Despliegues asociados |
| --- | --- | --- |
| `experiment` | Desarrollo activo y experimentación | Preview de landing si se necesita |
| `testing` | Integración y estabilización (release candidate) | Preview/staging de landing y docs |
| `main` | Solo producción estable | Landing producción · docs producción · artefactos de release |

**Nunca** `experiment → main` salvo una política de hotfix de emergencia documentada y aprobada.

## 2. Versionado

Versionado semántico: `MAJOR.MINOR.PATCH`. Los _tags_ se crean sobre `main` (`v1.0.0`).
El pre-1.0 usa `0.x` y puede introducir cambios incompatibles en `MINOR`.

## 3. Convención de commits

Conventional Commits (`feat`, `fix`, `docs`, `refactor`, `test`, `perf`, `chore`, `build`, `ci`).
`CHANGELOG.md` se actualiza en cada promoción a `testing` y se finaliza en la promoción a `main`.

## 4. Promoción `experiment → testing`

Requisitos:

- [ ] La funcionalidad compila.
- [ ] Las pruebas automatizadas pasan.
- [ ] No hay flujos obviamente rotos.
- [ ] Documentación iniciada.
- [ ] Migraciones probadas (base nueva + actualizada).
- [ ] UX revisada contra el checklist de [`ROADMAP.md`](./ROADMAP.md) / §7.

## 5. Promoción `testing → main` — Release Gate

`main` solo puede contener código que sea: compilable en una máquina Windows limpia, seguro para
migraciones, probado, documentado, consciente de permisos, auditado donde corresponde, imprimible,
recuperable, empaquetado y estable.

Checks obligatorios:

- [ ] Build de escritorio Java en Windows limpio.
- [ ] Migraciones de base de datos (nueva y actualizada).
- [ ] Pruebas de facturación.
- [ ] Unicidad de número de factura e integridad de pagos.
- [ ] Validación de impresión angosta (Modelo A).
- [ ] Validación de impresión US Letter (Modelo B).
- [ ] Generación de PDF.
- [ ] Render correcto de logo, nombre y colores de la institución.
- [ ] Reimpresión sin duplicar transacciones.
- [ ] Autorización / permisos aplicados.
- [ ] Se generan registros de auditoría.
- [ ] Backup y restauración validados.
- [ ] Flujo de Google Drive donde esté habilitado.
- [ ] Build de landing.
- [ ] Build de Docusaurus.
- [ ] GitHub Actions en verde.
- [ ] Documentación requerida presente y al día.
- [ ] Sin secretos comiteados.
- [ ] Notas de versión / `CHANGELOG.md` actualizados.
- [ ] Ruta de actualización de datos verificada.
- [ ] Aprobación de una persona revisora.

## 6. Artefactos de release (desde `main`)

```text
Instalador de escritorio para Windows (.msi / .exe vía jpackage)
Código fuente de la release (tag + tarball)
Despliegue de documentación (Docusaurus)
Despliegue de producción de la landing (Vercel)
```

Cada artefacto de instalador incluye: información de versión, entradas de menú inicio, opción de
acceso directo, datos de aplicación separados de los binarios, ruta de actualización sin destruir
datos y desinstalación limpia.

## 7. Protección de ramas (recomendada en GitHub)

| Rama | PR obligatorio | CI obligatorio | Revisión | Push directo |
| --- | --- | --- | --- | --- |
| `main` | ✅ | ✅ (todos los checks) | ✅ 1+ | ❌ |
| `testing` | ✅ | ✅ | ✅ 1+ | ❌ |
| `experiment` | recomendado | ✅ | opcional | desaconsejado |

## 8. Hotfix de emergencia

Solo con política aprobada. Rama `fix/hotfix-<n>` desde `main`, PR a `main` con CI completo y
revisión, _tag_ de patch, y **merge inmediato de vuelta** a `testing` y `experiment` para evitar
regresiones.
