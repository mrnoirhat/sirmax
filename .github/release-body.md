<!-- SPDX-License-Identifier: AGPL-3.0-or-later -->
## Descargar

| Archivo | Para qué |
| --- | --- |
| **`SIRMAX-*.msi`** | Lo habitual. Instala con entrada en el menú Inicio y acceso directo opcional. |
| `SIRMAX-*-windows.zip` | Portable: se descomprime y se ejecuta `SIRMAX.exe`. Para evaluarlo o correrlo desde una carpeta compartida. |
| `SHA256SUMS.txt` | Sumas de verificación de los dos anteriores. |

**No hace falta instalar Java, Node ni Python.** El artefacto trae su propio
runtime.

## Antes de instalar en un ayuntamiento

Imprime un recibo y una factura en el hardware real. Es lo único del Release
Gate que no se puede verificar automáticamente: las plantillas se comprueban como
PDF y como texto que cabe en el rollo, pero ninguna prueba sustituye poner papel
en una impresora de impacto.

## Dónde quedan los datos

En `%LOCALAPPDATA%\SIRMAX`, **nunca** dentro del directorio de instalación.
Actualizar conserva la base de datos; desinstalar también la deja. Detalles en
[`docs/PACKAGING.md`](https://github.com/mrnoirhat/sirmax/blob/main/docs/PACKAGING.md).

## Enlaces

- [Sitio web](https://sirmax.vercel.app)
- [Documentación](https://sirmax-docs.vercel.app) · [espejo en GitHub Pages](https://mrnoirhat.github.io/sirmax)
- [Release Gate](https://github.com/mrnoirhat/sirmax/blob/main/docs/RELEASE-GATE-1.0.md) — incluye lo que **no** cubre
- [CHANGELOG](https://github.com/mrnoirhat/sirmax/blob/main/CHANGELOG.md)

---

SIRMAX **no declara cumplimiento legal ni fiscal** de ninguna jurisdicción. Es
una herramienta de gestión; la conformidad la determina cada municipio con su
asesoría. AGPL-3.0-or-later.
