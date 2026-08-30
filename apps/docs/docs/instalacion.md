---
title: "Instalación"
sidebar_position: 2
description: "Descargar, instalar y comprobar SIRMAX en Windows."
---

# Instalación

SIRMAX se distribuye para Windows con **el runtime de Java incluido**. No hace
falta instalar Java ni ninguna otra cosa.

## Requisitos

| | Mínimo | Cómodo |
| --- | --- | --- |
| Sistema | Windows 10 (64 bits) | Windows 11 |
| Memoria | 4 GB | 8 GB |
| Disco | 500 MB | 2 GB (deja sitio a las copias) |
| Pantalla | 1280 × 720 | 1920 × 1080 |

## Descargar

Desde [las releases de GitHub](https://github.com/mrnoirhat/sirmax/releases/latest):

- **`SIRMAX-x.y.z.msi`** — instalador. Lo normal.
- **`SIRMAX-x.y.z-windows.zip`** — portable. Se descomprime y se ejecuta
  `SIRMAX.exe`; útil para probar sin instalar.

### Comprobar que el archivo llegó íntegro

Cada release publica `SHA256SUMS.txt`. En PowerShell, dentro de la carpeta de
descargas:

```powershell
Get-FileHash .\SIRMAX-1.0.2.msi -Algorithm SHA256
```

El valor tiene que coincidir con el del archivo de sumas. Si no coincide, la
descarga se corrompió o fue alterada: bórrala y repite.

## Instalar

Doble clic en el `.msi`. El instalador propone `C:\Program Files\SIRMAX`, crea
el acceso directo y la entrada del menú Inicio.

:::warning Windows avisará de que el editor no está verificado
SIRMAX es software libre sin certificado comercial. Comprueba la suma SHA-256,
pulsa **Más información** y luego **Ejecutar de todas formas**. El detalle
completo está en
[SIGNING.md](https://github.com/mrnoirhat/sirmax/blob/main/docs/SIGNING.md), y
cómo se firma cada release en la
[política de firma](https://github.com/mrnoirhat/sirmax/blob/main/docs/CODE-SIGNING-POLICY.md).
:::

## Dónde quedan los datos

| Qué | Dónde |
| --- | --- |
| Base de datos | `%LOCALAPPDATA%\SIRMAX\data\sirmax.sqlite` |
| Copias de seguridad | `%LOCALAPPDATA%\SIRMAX\backups` |
| Registros | `%LOCALAPPDATA%\SIRMAX\logs` |

**Desinstalar no borra esta carpeta.** Es deliberado: quitar el programa no debe
llevarse por delante los registros del ayuntamiento. Para empezar de cero hay que
borrarla a mano.

## Actualizar

Instala la versión nueva encima. Conserva el mismo código de actualización, así
que reemplaza la anterior sin duplicar entradas, y las migraciones de esquema se
aplican solas al primer arranque.

## Comprobar que funciona

1. Abre SIRMAX. La primera vez muestra **Configuración inicial**.
2. Crea el municipio y la cuenta administradora.
3. Entra con esa cuenta: debe aparecer «¿Qué necesitas hacer?».

Si algo falla, ver [Solución de problemas](./guia-usuario/solucion-de-problemas.md).
