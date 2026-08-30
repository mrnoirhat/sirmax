<!-- SPDX-License-Identifier: AGPL-3.0-or-later -->
# Política de firma de código

Este documento es un requisito de la [SignPath Foundation](https://signpath.org)
y, sobre todo, es lo que permite a un ayuntamiento comprobar que el archivo que
descargó salió de este repositorio y no de otro sitio.

Versión de la política: **1.0** · Última revisión: 2026-08-30

---

## Qué se firma

Los artefactos de Windows publicados en las
[releases de GitHub](https://github.com/mrnoirhat/sirmax/releases):

| Artefacto | Contenido |
| --- | --- |
| `SIRMAX-x.y.z.msi` | Instalador |
| `SIRMAX-x.y.z-windows.zip` | Portable, con `SIRMAX.exe` firmado dentro |

No se firman las bibliotecas del runtime de Java incluidas en el paquete: son de
Oracle y de Microsoft y llegan ya firmadas por ellos. Sustituir su firma por la
nuestra cambiaría una firma de confianza por otra que lo es menos.

## De dónde sale el binario

Únicamente de una compilación automatizada en GitHub Actions, disparada por una
etiqueta `vX.Y.Z` sobre la rama `main`:
[`.github/workflows/release.yml`](https://github.com/mrnoirhat/sirmax/blob/main/.github/workflows/release.yml).

Ningún artefacto firmado se construye en una máquina de desarrollo. Compilar
desde la etiqueta es lo que hace que lo descargado corresponda exactamente al
commit que la etiqueta nombra.

La compilación ejecuta `verifyReleaseArtifacts`, que comprueba que el ejecutable
y el runtime empaquetado están dentro y superan un tamaño mínimo. Ese umbral
existe porque un artefacto sin el runtime se instala sin problemas y luego no
arranca.

## Quién puede desencadenar una firma

| Rol | Quién | Qué puede hacer |
| --- | --- | --- |
| **Autor** | Cualquier persona que contribuya | Abrir un pull request contra `experiment` |
| **Revisor** | Mantenedores | Revisar y aprobar el merge a `main` |
| **Aprobador** | Andriezer Galva Montero (`@mrnoirhat`) | Etiquetar una versión y aprobar cada firma |

Cada firma requiere **aprobación manual** del aprobador en SignPath. No hay firma
automática.

## Qué protege el camino hasta ahí

La rama `main` está protegida: exige pull request, exige los cuatro checks
—compilación y pruebas del escritorio, del sitio, de la documentación y análisis
de secretos— y no admite `force push` ni borrado. La protección se aplica también
a los administradores.

`testing` y `experiment` no admiten `force push` ni borrado.

Todas las cuentas con permiso de escritura tienen **autenticación multifactor**
activada.

## Claves

La clave privada de firma la custodia la SignPath Foundation en un HSM. El
proyecto **nunca** la posee, ni puede exportarla.

El certificado autofirmado de desarrollo descrito en
[SIGNING.md](https://github.com/mrnoirhat/sirmax/blob/main/docs/SIGNING.md) es
para pruebas locales y **no** se usa en releases publicadas.

## Cómo comprobar una descarga

Cada release publica `SHA256SUMS.txt`. Antes de instalar:

```powershell
Get-FileHash .\SIRMAX-1.0.2.msi -Algorithm SHA256
```

El valor tiene que coincidir con el del archivo de sumas. Si no coincide, la
descarga se corrompió o fue alterada: bórrala y repite.

La firma se puede inspeccionar con:

```powershell
Get-AuthenticodeSignature .\SIRMAX-1.0.2.msi | Format-List Status, SignerCertificate
```

## Incidencias

Si sospechas que un artefacto publicado está comprometido, escribe a través de
[las issues del repositorio](https://github.com/mrnoirhat/sirmax/issues) o, si
prefieres reportarlo en privado, por el aviso de seguridad de GitHub. Se retirará
la release y se solicitará la revocación del certificado.
