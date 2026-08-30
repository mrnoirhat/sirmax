<!-- SPDX-License-Identifier: AGPL-3.0-or-later -->
# Firma de la aplicación

SIRMAX firma su ejecutable y su instalador con un certificado a nombre de
**Andriezr Glava Montero (Mrnoirhat)**. Este documento dice qué consigue eso y —
más importante — qué **no** consigue, porque en firma de código hay mucha
promesa fácil y la diferencia se paga en soporte.

---

## Lo que hay hoy

`tools/new-signing-cert.ps1` crea un certificado **autofirmado** en el almacén del
usuario. `tools/sign-windows.ps1` firma con él `SIRMAX.exe` y el `.msi`, con sello
de tiempo. El build lo hace solo:

```bash
./gradlew :sirmax-app:verifyReleaseArtifacts
```

La tarea `signWindowsArtifacts` se salta sin fallar cuando no hay certificado —
CI no tiene ninguno, y un pipeline que se pone en rojo porque la única clave vive
en un portátil detiene el trabajo de todos.

Solo se firman los binarios propios. Las DLL de `runtime\bin` son de Oracle y de
Microsoft y ya vienen firmadas por ellos: volver a firmarlas con nuestra clave
cambiaría una firma de confianza por otra que lo es menos.

## Lo que un autofirmado sí resuelve

| Antes | Después |
| --- | --- |
| «Editor desconocido» | «Andriezr Glava Montero (Mrnoirhat)» |
| Cualquiera puede alterar el `.msi` sin que se note | Alterarlo invalida la firma |
| Sin sello de tiempo | Sello RFC-3161: las copias ya descargadas siguen verificando cuando el certificado caduque |

## Lo que **no** resuelve

**Windows no confía en un certificado autofirmado.** La cadena termina en una raíz
que el sistema no conoce, así que `Get-AuthenticodeSignature` devuelve
`UnknownError` y SmartScreen sigue avisando. Esto no es un fallo de configuración
que se pueda arreglar con más banderas: es el diseño del sistema. Un certificado
en el que confiara todo Windows sin que nadie lo autorice haría inútil el
mecanismo entero.

En consecuencia:

- **En esta PC** se puede resolver, instalando el certificado como de confianza
  (abajo).
- **En otra PC cualquiera, sin tocar nada, no.** No existe forma legítima de
  conseguirlo con un autofirmado, y las que existen son evasión de una protección
  de seguridad, que es justo lo que ese aviso está para impedir.

### Confiar en el certificado en esta máquina

Es una **decisión de seguridad**, no un paso de instalación: a partir de ese
momento tu usuario confía en cualquier binario firmado con esa clave. Por eso lo
dejo como un comando que ejecutas tú y no algo que el build hace por su cuenta.

```powershell
$c = (Get-ChildItem Cert:\CurrentUser\My -CodeSigningCert | Where-Object { $_.Subject -like '*Andriezr*' })[0]; Export-Certificate -Cert $c -FilePath "$env:TEMP\sirmax.cer" | Out-Null; Import-Certificate -FilePath "$env:TEMP\sirmax.cer" -CertStoreLocation Cert:\CurrentUser\Root; Import-Certificate -FilePath "$env:TEMP\sirmax.cer" -CertStoreLocation Cert:\CurrentUser\TrustedPublisher
```

Después, `Get-AuthenticodeSignature` sobre el `.msi` pasa a `Valid` y el instalador
deja de anunciarse como de editor desconocido. Para deshacerlo, borra el
certificado de `Cert:\CurrentUser\Root` y de `Cert:\CurrentUser\TrustedPublisher`.

---

## Si quieres que no avise en ninguna PC

Hace falta un certificado emitido por una autoridad en la que Windows ya confíe.
No hay ninguno gratuito: Let's Encrypt no emite certificados de firma de código y
ninguna CA los regala, porque el valor del certificado es precisamente que alguien
verificó una identidad.

| Opción | Coste aproximado | Qué consigue |
| --- | --- | --- |
| **Certum Open Source Code Signing** | ~25–30 €/año | La vía barata pensada para proyectos libres. Pide documentación de identidad y que el proyecto sea open source — SIRMAX cumple. |
| OV estándar (Sectigo, SSL.com…) | ~200–400 €/año | Igual de válido; más caro sin ventaja real para este caso. |
| **EV (token físico)** | ~350–600 €/año | La única que quita el aviso de SmartScreen **desde la primera descarga**. |

Detalle que conviene saber antes de pagar: con un certificado OV (las dos primeras
filas) SmartScreen **sigue avisando** hasta que esa firma acumula reputación —
descargas e instalaciones sin incidencias durante semanas. Firmar no compra
reputación, la empieza. Solo EV la concede de entrada.

Cuando tengas uno, no hay que cambiar código: impórtalo en el almacén del usuario
y `tools/sign-windows.ps1` lo encontrará por el asunto, o pásale otro con
`-Subject`. Para firmar en CI hay que meter el `.pfx` como secreto del repositorio
y desbloquearlo en el runner.

## Lo que no vamos a hacer

Desactivar Defender, añadir exclusiones en las máquinas de otros, o cualquier
técnica para que el binario no sea inspeccionado. Un ayuntamiento instalando
software de gestión municipal es exactamente el usuario al que esas protecciones
sirven, y un proyecto que pide desactivarlas se parece demasiado a aquello de lo
que protegen.

## Qué decirle a quien instala

Mientras no haya certificado de CA, esto es lo honesto y suele bastar:

> Windows avisará de que el editor no está verificado, porque SIRMAX es software
> libre sin certificado comercial. Comprueba que la suma SHA-256 del archivo
> coincide con la publicada en la release de GitHub, pulsa **Más información** y
> luego **Ejecutar de todas formas**.

Las sumas se publican en `SHA256SUMS.txt` con cada release, y sirven para lo mismo
que la firma: detectar que el archivo llegó alterado.
