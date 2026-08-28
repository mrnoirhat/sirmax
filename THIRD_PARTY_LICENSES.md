# Licencias de terceros

SIRMAX se distribuye bajo **AGPL-3.0-or-later** (ver [`LICENSE`](./LICENSE)). Este documento inventaría
las dependencias de terceros y su licencia, y debe mantenerse al día en cada cambio de dependencias.

> Estado: **pendiente de poblar**. La fundación del repositorio (Fase 1) aún no fija versiones
> definitivas. Cada PR que añada o actualice una dependencia debe actualizar la tabla correspondiente
> y confirmar compatibilidad de licencia con AGPL-3.0-or-later.

## 1. Compatibilidad de licencias

Se aceptan, en general, dependencias bajo licencias permisivas o copyleft compatibles: Apache-2.0,
MIT, BSD-2/3-Clause, EPL-2.0, MPL-2.0, LGPL-2.1+/3.0+, GPL-3.0+, AGPL-3.0+, CC0/CC-BY para recursos.
**No** se aceptan dependencias con licencias incompatibles con AGPL o con cláusulas de uso
restringido (p. ej. "non-commercial", SSPL, BUSL sin excepción aplicable). Ante la duda, se abre un
issue de revisión de licencia.

## 2. Escritorio (`apps/desktop` — Java)

| Componente | Uso | Licencia | Notas |
| --- | --- | --- | --- |
| OpenJDK 25 | Runtime / compilación | GPL-2.0-with-Classpath-Exception | Empaquetado con jpackage; la excepción de classpath permite la distribución |
| OpenJFX (JavaFX) | UI de escritorio | GPL-2.0-with-Classpath-Exception | — |
| SQLite | Base de datos embebida | Public Domain | — |
| _driver JDBC de SQLite_ | Acceso a datos | _por fijar (Apache-2.0 esperado)_ | p. ej. `org.xerial:sqlite-jdbc` |
| _motor de migraciones_ | Migraciones de esquema | _por fijar_ | Flyway Community (Apache-2.0) o runner propio |
| _librería PDF_ | Generación de PDF | _por fijar_ | Debe ser compatible con AGPL (evitar iText comercial) |
| _generación de QR_ | Códigos de verificación | _por fijar (Apache-2.0 esperado)_ | p. ej. ZXing |
| JUnit 5 | Pruebas | EPL-2.0 | Solo test |
| AssertJ | Pruebas | Apache-2.0 | Solo test |
| ArchUnit | Pruebas de arquitectura | Apache-2.0 | Solo test |
| TestFX | Smoke de UI | EUPL-1.1 / Apache-2.0 | Solo test |

## 3. Web — landing (`apps/landing`)

| Componente | Licencia | Notas |
| --- | --- | --- |
| Next.js | MIT | — |
| React / React DOM | MIT | — |
| TypeScript | Apache-2.0 | — |
| ESLint + config | MIT | Solo desarrollo |

## 4. Web — documentación (`apps/docs`)

| Componente | Licencia | Notas |
| --- | --- | --- |
| Docusaurus | MIT | — |
| React (transitivo) | MIT | — |
| Mermaid | MIT | Diagramas |

## 5. Recursos (fuentes, iconos, imágenes)

| Recurso | Licencia | Notas |
| --- | --- | --- |
| _tipografía de UI_ | _por fijar (OFL-1.1 o similar)_ | Debe permitir empaquetado y redistribución |
| _set de iconos_ | _por fijar (MIT / Apache-2.0 / CC-BY)_ | — |

## 6. Cómo regenerar este inventario

- Java: `./gradlew licenseReport` (plugin de reporte de licencias, a configurar en Fase 1/10).
- Web: `npx license-checker --summary` en cada workspace.
Los resultados se revisan y se transcriben aquí; no se sustituye la revisión humana por la
herramienta.
