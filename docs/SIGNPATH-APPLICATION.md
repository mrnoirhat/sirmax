<!-- SPDX-License-Identifier: AGPL-3.0-or-later -->
# Solicitud a la SignPath Foundation

Lista para presentar la solicitud de firma gratuita. Lo que puede hacerse desde
el repositorio ya está hecho; lo que queda exige tu cuenta de GitHub o tu
decisión.

Solicitud: <https://signpath.org/apply>

---

## Requisitos y su estado

| Requisito | Estado |
| --- | --- |
| Licencia aprobada por la OSI | ✅ AGPL-3.0-or-later |
| Sin doble licencia comercial | ✅ |
| Sin componentes propietarios | ✅ Solo bibliotecas de sistema y dependencias libres |
| Repositorio público, propiedad del equipo | ✅ `github.com/mrnoirhat/sirmax` |
| Proyecto mantenido y ya publicado | ✅ v1.0.2 publicada |
| No es herramienta de seguridad ofensiva | ✅ Gestión municipal |
| Compilación automatizada y verificable | ✅ `.github/workflows/release.yml`, disparada por etiqueta |
| Metadatos en el binario (producto y versión) | ✅ jpackage los escribe |
| Política de firma publicada | ✅ [`docs/CODE-SIGNING-POLICY.md`](./CODE-SIGNING-POLICY.md) |
| Roles definidos | ✅ En la política: autor, revisor, aprobador |
| Aprobación manual por release | ✅ Así queda configurado en SignPath |
| **MFA en todas las cuentas con escritura** | ⬜ **Verifícalo tú** |

## Lo único que falta antes de solicitar

**Activa la autenticación multifactor** en tu cuenta de GitHub, si no la tiene:
<https://github.com/settings/security>. Es el único requisito que no se puede
comprobar ni cumplir desde el repositorio, y SignPath lo exige.

## Datos que pedirá el formulario

| Campo | Valor |
| --- | --- |
| Nombre del proyecto | SIRMAX |
| Repositorio | `https://github.com/mrnoirhat/sirmax` |
| Licencia | AGPL-3.0-or-later |
| Descripción | Sistema de gestión municipal para ayuntamientos, local-first, de código abierto |
| Sitio | `https://sirmax.vercel.app` |
| Política de firma | `https://github.com/mrnoirhat/sirmax/blob/main/docs/CODE-SIGNING-POLICY.md` |
| Contacto / aprobador | Andriezer Galva Montero (`@mrnoirhat`) |
| Artefactos a firmar | `SIRMAX-x.y.z.msi` y `SIRMAX-x.y.z-windows.zip` |

## Cuando la aprueben

El workflow ya tiene el paso de firma; se salta solo mientras no exista el
secreto. Para activarlo hacen falta dos valores que da SignPath:

```bash
gh secret set SIGNPATH_API_TOKEN --repo mrnoirhat/sirmax
gh variable set SIGNPATH_ORGANIZATION_ID --repo mrnoirhat/sirmax --body "<el-id>"
```

Y comprobar que el *project slug* y el *signing policy slug* que SignPath asigne
coinciden con los del workflow (`sirmax` y `release-signing`); si no, ajústalos
en `.github/workflows/release.yml`.

A partir de ahí, cada etiqueta `vX.Y.Z` sube el artefacto a SignPath, espera tu
aprobación manual y descarga el binario firmado antes de calcular las sumas —el
orden importa, porque firmar reescribe el archivo y un hash tomado antes no
coincidiría con lo que nadie descarga.

## Qué esperar después

El aviso de SmartScreen **no desaparece el primer día**. Lo que cambia:

- El aviso muestra un editor verificado, «SignPath Foundation», en lugar de nada.
- La reputación se acumula entre versiones al firmar siempre con la misma
  identidad, y el aviso deja de salir en unas semanas de descargas limpias.
- En Windows 11, Smart App Control deja de bloquear el binario por no estar
  firmado.

El detalle y el porqué están en [SIGNING.md](./SIGNING.md).
