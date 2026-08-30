---
title: "Solución de problemas"
sidebar_position: 17
description: "Qué hacer cuando SIRMAX no arranca, no imprime o no cuadra."
---

# Solución de problemas

## SIRMAX no abre

**Ventanita con el título `SIRMAX.exe` y nada más.** Es un error de arranque. El
detalle está en `%LOCALAPPDATA%\SIRMAX\logs`.

**«Migration Vxxxx has changed since it was applied».** La base de datos se creó
con una versión del programa cuyo esquema no coincide con el instalado. Suele
pasar al mezclar una compilación de desarrollo con una release. La copia de
seguridad más reciente y una [restauración](./restauracion.md) lo resuelven; si
la base era de pruebas, borra
`%LOCALAPPDATA%\SIRMAX\data\sirmax.sqlite` y arranca de cero.

**No pasa nada al hacer doble clic.** Otra instancia ya está abierta. Búscala en
la barra de tareas.

## Windows bloquea la instalación

Es esperable: SIRMAX no tiene certificado comercial. Comprueba la suma SHA-256
contra `SHA256SUMS.txt`, pulsa **Más información → Ejecutar de todas formas**.
Detalle en
[SIGNING.md](https://github.com/mrnoirhat/sirmax/blob/main/docs/SIGNING.md).

Si tu antivirus lo borra directamente, restaura el archivo desde su cuarentena y
añade la carpeta de instalación a sus excepciones. Verifica la suma **antes**.

## No imprime

1. ¿Imprime otra cosa esa impresora? Si no, es de Windows.
2. **Documentos → Impresoras configuradas**: ¿el perfil apunta a la cola
   correcta? Vacío = predeterminada de Windows.
3. ¿El papel es el correcto? Un recibo va a rollo de 58 u 80 mm; una factura, a
   carta.

**«Se canceló la impresión».** Alguien cerró el diálogo de Windows. El documento
**sigue emitido** y conserva su número: no lo emitas otra vez, reimprímelo desde
[Documentos](./documentos-oficiales.md).

## No me deja cobrar en efectivo

No hay caja abierta. **Caja → Abrir caja**. Cada cajero tiene la suya.

## La caja no cuadra

No la fuerces. Escribe lo que contaste de verdad: la diferencia queda registrada
y esa es la señal que el ayuntamiento necesita. Repasa después los cobros del
turno en [Reportes](./reportes.md), filtrando por el día.

## No encuentro a un ciudadano

La búsqueda ignora tildes: `pena` encuentra `Peña`. Prueba solo por el número de
cédula, o por un apellido. Si aparece dos veces, es un duplicado: SIRMAX avisa al
registrar, pero uno anterior puede haberse colado.

## Un botón no aparece

Es permiso, no avería. Ver
[Usuarios y permisos](./usuarios-y-permisos.md).

## No puedo editar un servicio

Está publicado. Se crea una **versión nueva**. Ver [Servicios](./servicios.md).

## La auditoría sale rota

**Configuración → Verificar auditoría** ha encontrado una entrada alterada:
alguien tocó la base de datos por fuera de SIRMAX. Restaura desde una copia
anterior a esa fecha y revisa quién tiene acceso al equipo.

## Reunir información para pedir ayuda

Al [abrir una issue](https://github.com/mrnoirhat/sirmax/issues), incluye:

- Versión (**Ayuda → Acerca de**).
- Qué hacías y qué esperabas.
- El texto exacto del error.
- Lo último de `%LOCALAPPDATA%\SIRMAX\logs`.

:::danger Revisa el registro antes de publicarlo
Puede contener nombres y cédulas de ciudadanos reales.
:::
