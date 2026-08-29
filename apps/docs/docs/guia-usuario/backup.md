---
title: "Copias de seguridad"
sidebar_position: 11
description: "Cómo se hacen las copias, qué llevan dentro y cuándo salen del ayuntamiento."
---

# Copias de seguridad

SIRMAX hace copias completas de la base de datos, con esta secuencia:

```text
instantánea → huella → comprimir → cifrar → hash → disco → (opcional) Google Drive
```

## Qué se guarda

La instantánea la toma **SQLite mismo**, no es una copia del fichero. Copiar el
fichero con la aplicación abierta capturaría páginas a medio escribir.

Cada copia guarda además una **huella**: cuántas personas, cuántos trámites,
cuántas facturas. Sirve para distinguir de un vistazo una copia completa de una
tomada contra una base vacía, sin descifrar nada.

## Cifrado

Con AES-256-GCM, que **autentica además de cifrar**: un archivo manipulado falla
al descifrar en vez de restaurar datos corruptos encima de la base del
ayuntamiento.

:::danger La frase de cifrado no se guarda en ninguna parte
Si se pierde, **las copias cifradas no se pueden recuperar**. Una copia cifrada
cuya clave está en el mismo disco no es una copia cifrada.
:::

## Verificación

Cada copia se **relee inmediatamente** y se compara con su hash. Una copia que
nadie ha leído es una promesa, no un respaldo — y el momento de descubrir un
disco defectuoso es ahora, no durante una restauración.

## Fuera del ayuntamiento

La subida a Google Drive está **apagada por defecto** y hay que activarla
eligiendo una carpeta concreta. Las credenciales son del ayuntamiento, no de
SIRMAX. Un padrón de ciudadanos no sale del edificio sin que alguien lo decida.

Si la subida falla, la copia local sigue siendo válida: se avisa, no se descarta.

## Retención

Las copias rutinarias antiguas se borran según la política. Las copias de
**emergencia** y las **previas a migración** nunca — existen justo para el
momento en que algo salió mal, que es exactamente cuando una regla de retención
las habría tirado.

De una copia purgada se conserva la ficha: su hash, su huella y, si se subió, su
identificador remoto.
