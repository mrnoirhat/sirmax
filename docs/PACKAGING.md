<!-- SPDX-License-Identifier: AGPL-3.0-or-later -->
# Empaquetado para Windows

SIRMAX se distribuye con su propio runtime de Java. Un PC municipal **no necesita
instalar Java, Node ni Python** (master prompt §44).

## Artefactos

| Artefacto | Tarea Gradle | Necesita | Para qué |
| --- | --- | --- | --- |
| Carpeta autocontenida | `packageAppImage` | nada más que el JDK | Evaluar SIRMAX, ejecutarlo desde una carpeta compartida o un USB |
| Instalador MSI | `packageWindows` | [WiX 3.x](https://wixtoolset.org) en el `PATH` | Despliegue real en el ayuntamiento |

Ambos se verifican con `verifyReleaseArtifacts`, que falla si el runtime no
quedó incluido — un artefacto que instala bien y luego no arranca es peor que
uno que no se construye.

```bash
cd apps/desktop && ./gradlew verifyReleaseArtifacts
```

Sin WiX, la tarea del MSI se **salta** con un mensaje explicando qué falta, en
lugar de romper la compilación de quien solo está desarrollando.

## Qué hace el instalador

- Entrada en el menú Inicio, bajo el grupo `SIRMAX`.
- Acceso directo en el escritorio **opcional** (`--win-shortcut-prompt`): se
  pregunta, no se impone.
- Instalación por usuario (`--win-per-user-install`), así no exige permisos de
  administrador en cada mostrador.
- Elección del directorio de instalación (`--win-dir-chooser`).

## Dónde viven los datos

**Nunca dentro del directorio de instalación.** Una actualización reemplaza esa
carpeta por completo, así que la base de datos quedaría destruida en cada
versión nueva. `AppPaths` la coloca en:

```text
%LOCALAPPDATA%\SIRMAX\data\sirmax.sqlite
%LOCALAPPDATA%\SIRMAX\backups\
%LOCALAPPDATA%\SIRMAX\logs\
```

Consecuencias, todas deliberadas:

- **Actualizar** conserva los datos. El `--win-upgrade-uuid` es constante para
  toda la línea de producto, así que una versión nueva *actualiza* la anterior en
  lugar de instalarse al lado.
- **Desinstalar** deja los datos. El desinstalador solo quita lo que instaló. Un
  ayuntamiento que desinstala para reinstalar no puede perder su registro; si
  quiere borrar los datos, tiene que hacerlo a propósito.

## Tamaño

El runtime recortado con `jlink` ocupa unos 51 MB y la imagen completa unos 81 MB.
Los módulos incluidos están enumerados en `sirmax-app/build.gradle.kts`; añadir
una dependencia que necesite otro módulo del JDK falla al arrancar, no al
compilar, así que la lista se revisa cuando cambia el classpath.

## Verificado

La imagen empaquetada se ejecutó en Windows 11 sin `JAVA_HOME` ni Java en el
`PATH`: arrancó, aplicó las nueve migraciones y abrió la ventana de JavaFX.
