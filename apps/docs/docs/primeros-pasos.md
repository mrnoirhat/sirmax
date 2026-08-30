---
title: "Primeros pasos"
sidebar_position: 3
description: "Del primer arranque al primer trámite cobrado."
---

# Primeros pasos

Media hora desde instalar hasta cobrar el primer trámite.

## 1. Crear el ayuntamiento y la cuenta administradora

En el primer arranque SIRMAX pide el municipio y una cuenta. Que sea la primera
vez lo decide **que no exista ningún usuario**, nunca un ajuste.

:::danger La contraseña no se puede recuperar
No hay servidor que la reponga ni correo de recuperación: los datos están
cifrados en el equipo. Mínimo 12 caracteres. Anótala donde el ayuntamiento
guarda lo importante.
:::

## 2. Completar los datos del ayuntamiento

**Configuración → Datos del ayuntamiento**: RNC, dirección, teléfono, pie de
factura.

Hazlo antes de emitir nada. Estos datos se **congelan dentro** de cada documento
al emitirlo, así que una factura emitida con la ficha vacía se queda vacía para
siempre, aunque la rellenes después.

## 3. Crear los departamentos

**Departamentos → Agregar un departamento**. Al menos uno: los trámites se
enrutan a departamentos.

El código (`PLAN`, `CIVIL`, `CEM`) aparece en la numeración de los trámites, así
que conviene corto y estable.

## 4. Cargar el catálogo de servicios

**Servicios → Cargar catálogo base** trae los servicios municipales dominicanos
más habituales, todos como **borrador**.

Nada de eso está publicado todavía, y es a propósito: los montos vienen en cero
porque ningún catálogo genérico sabe lo que cobra tu ayuntamiento.

## 5. Revisar y publicar un servicio

1. Selecciónalo en la lista.
2. En el editor de abajo pon requisitos, plazo, vigencia y **monto**.
3. **Guardar borrador**.
4. Revísalo entero.
5. **Publicar**.

:::warning Publicar es definitivo
Una versión publicada no se edita nunca más, porque cada trámite se queda fijado
a la versión con la que se abrió. Para cambiar algo se crea una **versión nueva**.
Revisa el monto antes de publicar.
:::

## 6. El primer trámite

1. **Inicio → Registrar un trámite**.
2. Busca al ciudadano o créalo.
3. Elige el servicio.
4. Marca los requisitos entregados.
5. Avanza el trámite.

## 7. Cobrar

1. **Caja → Abrir caja** con el fondo inicial.
2. **Facturación**: el caso aparece en *Casos por facturar* → **Emitir factura**.
3. Selecciona la factura, método de pago, **Cobrar**. En efectivo, escribe lo
   entregado y SIRMAX calcula el cambio.
4. **Imprimir recibo**.

## 8. Antes de irte

**Configuración → Copias de seguridad**: activa la copia automática a una hora
fuera del horario de caja. Es lo que evita perder el trabajo de un mes.
