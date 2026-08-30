<!-- SPDX-License-Identifier: AGPL-3.0-or-later -->
# Firma de la aplicación

SIRMAX firma su ejecutable y su instalador. Este documento dice qué consigue cada
opción de certificado y —más importante— qué **no** consigue ninguna, porque en
firma de código circula mucha información desactualizada.

---

## Lo que hay hoy: certificado de desarrollo

`tools/new-signing-cert.ps1` crea un certificado **autofirmado** a nombre de
**Andriezer Galva Montero (Mrnoirhat)** en el almacén del usuario.
`tools/sign-windows.ps1` firma con él `SIRMAX.exe` y el `.msi`, con sello de
tiempo. El build lo hace solo:

```bash
./gradlew :sirmax-app:verifyReleaseArtifacts
```

La tarea `signWindowsArtifacts` se salta sin fallar cuando no hay certificado —
CI no tiene ninguno, y un pipeline en rojo porque la única clave vive en un
portátil detiene el trabajo de todos.

Solo se firman los binarios propios. Las DLL de `runtime\bin` son de Oracle y de
Microsoft y ya vienen firmadas por ellos: volver a firmarlas con nuestra clave
cambiaría una firma de confianza por otra que lo es menos.

### Qué resuelve y qué no

| | |
| --- | --- |
| Sí | Detecta que el archivo fue alterado |
| Sí | Sello de tiempo: las copias ya descargadas siguen verificando cuando el certificado caduque |
| **No** | **Windows lo trata igual que si no estuviera firmado** |

Ese último punto es el que importa. Según la
[documentación de Microsoft](https://learn.microsoft.com/en-us/windows/apps/package-and-deploy/smartscreen-reputation),
un certificado autofirmado produce **el mismo aviso que un binario sin firma**, y
ni siquiera muestra el nombre del editor. Sirve para desarrollo interno; no
mejora nada de cara al usuario final.

En **esta** máquina se puede confiar en él manualmente. Es una **decisión de
seguridad**, no un paso de instalación: a partir de ahí tu usuario confía en
cualquier binario firmado con esa clave.

```powershell
$c = (Get-ChildItem Cert:\CurrentUser\My -CodeSigningCert | Where-Object { $_.Subject -like '*Andriezer*' })[0]; Export-Certificate -Cert $c -FilePath "$env:TEMP\sirmax.cer" | Out-Null; Import-Certificate -FilePath "$env:TEMP\sirmax.cer" -CertStoreLocation Cert:\CurrentUser\Root; Import-Certificate -FilePath "$env:TEMP\sirmax.cer" -CertStoreLocation Cert:\CurrentUser\TrustedPublisher
```

Para deshacerlo, borra el certificado de `Cert:\CurrentUser\Root` y de
`Cert:\CurrentUser\TrustedPublisher`.

---

## Cómo funciona SmartScreen en realidad

Conviene entenderlo antes de elegir, porque decide qué vale la pena pagar.

SmartScreen mira **dos** cosas:

1. **Reputación del certificado** — se hereda de una versión a la siguiente si
   firmas siempre con la misma identidad.
2. **Reputación del hash del archivo** — empieza en **cero con cada release**,
   siempre, firmes o no.

> **Dos creencias muy extendidas que son falsas**
>
> **«Un certificado EV quita el aviso desde la primera descarga».** Ya no.
> Microsoft lo dice con todas las letras: *«EV certificates no longer bypass
> SmartScreen […] Paying a premium for EV solely to avoid SmartScreen warnings is
> no longer justified»*. Ese comportamiento existió y se retiró.
>
> **«Firmar hace que no avise».** Firmar hace que el aviso **muestre tu nombre**
> en lugar de «editor desconocido», y permite que la reputación se acumule entre
> versiones. El primer día sigue avisando.

Reputación real se gana con **descargas limpias durante semanas**, firmando cada
release con la misma identidad. No hay forma de comprarla ni de solicitarla para
usuarios domésticos.

---

## Las opciones reales

### SignPath Foundation — gratuito para código abierto

Esto es exactamente el **«certificado de uso mancomunado»**, hecho de forma
legítima: una fundación posee el certificado en un HSM y firma los binarios de
muchos proyectos libres que califican.

SIRMAX cumple los requisitos: licencia aprobada por la OSI (AGPL-3.0), sin doble
licencia comercial, repositorio público y propio, proyecto mantenido y ya
publicado, y no es una herramienta de seguridad.

Lo que pide a cambio:

- Compilación automatizada y verificable (ya la hay, en GitHub Actions).
- MFA en todas las cuentas del equipo.
- Roles definidos: autor, revisor, aprobador.
- Una política de firma publicada en el sitio del proyecto.
- Aprobación manual de cada release.

> **El editor que se muestra es «SignPath Foundation», no tu nombre.**
> El certificado se emite **a la fundación**, no al proyecto. Es el precio de que
> sea gratis, y choca con el requisito de que aparezcas tú como desarrollador.

Solicitud: <https://signpath.org/apply>

### Certum Open Source — ~25–30 €/año

Emitido **a tu nombre**, así que el aviso muestra «Andriezer Galva Montero».
Pensado para proyectos libres; pide documentación de identidad.

Es la opción si el nombre importa más que el coste.

### Azure Artifact Signing (antes Trusted Signing) — 9,99 USD/mes

De Microsoft, sin token físico, integrable en CI. **Para desarrolladores
individuales está limitado hoy a EE. UU. y Canadá**, así que desde República
Dominicana probablemente no sea una opción por ahora.

### Microsoft Store — la única sin aviso

Es la única vía con **cero avisos desde la primera descarga**: la Store vuelve a
firmar la aplicación con un certificado de Microsoft. Requiere empaquetar como
MSIX y pasar su certificación, que es un cambio de modelo de distribución, no
solo de firma.

## Comparativa

| Opción | Coste | Editor mostrado | Primer día |
| --- | --- | --- | --- |
| Autofirmado (hoy) | 0 | ninguno | Avisa, como si no estuviera firmado |
| **SignPath Foundation** | 0 | SignPath Foundation | Avisa; la reputación se acumula |
| **Certum Open Source** | ~30 €/año | Andriezer Galva Montero | Avisa; la reputación se acumula |
| Azure Artifact Signing | ~120 USD/año | Andriezer Galva Montero | Avisa; la reputación se acumula |
| **Microsoft Store** | 0 (alta ~19 USD) | Microsoft | **No avisa** |

## Sobre «que corra en cualquier PC sin ser bloqueado»

Con honestidad: **fuera de la Store, ninguna opción lo garantiza el primer día.**
Lo que sí se consigue firmando con un certificado de una CA reconocida es que el
aviso muestre un nombre verificable y que desaparezca a las pocas semanas, en
lugar de reaparecer con cada versión.

Un detalle extra para Windows 11: **Smart App Control** bloquea la ejecución de
archivos sin firma, y a diferencia de SmartScreen no se limita a lo descargado de
internet. Ahí un certificado real no es cosmética.

## Lo que no vamos a hacer

Compartir o reutilizar la clave privada de otro editor. Es una violación del
acuerdo de suscriptor de cualquier CA, motivo de revocación inmediata, y es
literalmente el mecanismo por el que se firma malware. Un «certificado
compartido» en ese sentido no existe de forma legítima; lo que sí existe es
SignPath, donde una fundación firma **con su identidad** y asume la
responsabilidad.

Tampoco desactivar Defender ni pedir exclusiones en las máquinas de otros. Un
ayuntamiento instalando software de gestión municipal es exactamente el usuario
al que esas protecciones sirven.

## Qué decirle a quien instala

Mientras no haya certificado de CA:

> Windows avisará de que el editor no está verificado, porque SIRMAX es software
> libre sin certificado comercial. Comprueba que la suma SHA-256 del archivo
> coincide con la publicada en la release de GitHub, pulsa **Más información** y
> luego **Ejecutar de todas formas**.

Las sumas se publican en `SHA256SUMS.txt` con cada release y sirven para lo mismo
que la firma: detectar que el archivo llegó alterado.
