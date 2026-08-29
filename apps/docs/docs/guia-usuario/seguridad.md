---
title: "Seguridad"
sidebar_position: 13
description: "Contraseñas, bloqueo de cuentas, permisos e integridad de la auditoría."
---

# Seguridad

«Local» no significa «sin proteger».

## Contraseñas

Se guardan con PBKDF2 y sal por cuenta; SIRMAX nunca guarda la contraseña.

La política pide una longitud mínima y rechaza las que se adivinan solas. **No**
exige «una mayúscula, un número y un símbolo»: esa regla produce
`Contrasena1!` de forma fiable y no mejora nada.

## Bloqueo de cuenta

Tras varios intentos fallidos seguidos, la cuenta se bloquea. El bloqueo:

- vive en la cuenta, así que **sobrevive a un reinicio** — cerrar SIRMAX no es la
  forma de saltárselo;
- **expira solo**. Un ayuntamiento cuyo único administrador está de vacaciones
  también tiene que abrir por la mañana.

Un usuario inexistente y una contraseña incorrecta responden **exactamente
igual**. Distinguirlos convertiría la pantalla de acceso en un directorio del
personal.

Todos los intentos quedan registrados, también los correctos: eso es lo que
distingue «se equivoca al teclear los lunes» de «alguien probó nueve usuarios a
las 3 de la mañana».

## Permisos

Cada acción exige un permiso concreto. Un botón que tu usuario no puede usar **no
aparece**, en vez de aparecer deshabilitado.

## Integridad de la auditoría

Cada entrada de auditoría se encadena con la anterior mediante un hash. La base
de datos ya rechaza modificar o borrar entradas, pero esos disparadores los puede
eliminar quien tenga el fichero.

La cadena no impide la manipulación: **la hace visible**. Editar una entrada
rompe su propio hash; borrar una rompe el enlace de la siguiente. La verificación
recorre la cadena y señala la primera entrada afectada.

## Archivos adjuntos

Se validan por su **contenido**, no por su extensión. Un `.exe` renombrado a
`.pdf` se rechaza. Solo se aceptan PDF e imágenes escaneadas; quedan fuera los
ejecutables, los comprimidos y todo formato con motor de macros, documentos de
Office incluidos.
