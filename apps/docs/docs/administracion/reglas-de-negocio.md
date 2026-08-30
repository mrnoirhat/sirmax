---
title: "Reglas de negocio"
sidebar_position: 2
description: "Las invariantes que SIRMAX impone, y el motivo de cada una."
---

# Reglas de negocio

SIRMAX se niega a hacer ciertas cosas. Todas parecen rigidez hasta que se ve qué
evitan. Están aquí para que se puedan discutir con conocimiento en vez de
sortearse.

## El dinero se guarda en enteros

Cada importe es **un número de unidades mínimas** (centavos) más su moneda. Nunca
un decimal en coma flotante.

`0.1 + 0.2` no es `0.3` en coma flotante. Sobre miles de cobros eso produce un
descuadre que nadie puede explicar y que aparece justo en el arqueo. Con enteros
la suma es exacta por construcción.

Consecuencia visible: los totales se suman **por moneda** y se muestran por
separado si hubiera varias, en lugar de sumar peras con manzanas.

## Una versión publicada de un servicio no cambia

Cada trámite guarda **el identificador de la versión** con la que se abrió, y lo
conserva de por vida.

Sin esto, subir una tasa cambiaría retroactivamente el precio de expedientes ya
entregados. Un ciudadano que presentó papeles en marzo paga lo de marzo, y lo
cobrado sigue cuadrando con lo que decía el catálogo entonces.

Para cambiar algo se publica una versión nueva. Ver
[Servicios](../guia-usuario/servicios.md).

## Nada se borra

Se **anula**, se **archiva** o se **devuelve**, siempre con autor y motivo.

- Anular una factura la deja anulada, no la elimina.
- Devolver un pago añade la devolución; el cobro original sigue.
- Archivar un departamento lo saca de las listas; los trámites que lo
  referencian siguen íntegros.

Un registro que desaparece es un registro que nadie puede explicar después, y una
auditoría con huecos no es una auditoría.

## Un cobro cuelga siempre de una factura

No hay ingresos sueltos, y una factura cuelga siempre de un trámite. Un ingreso
sin expediente detrás es exactamente lo que una auditoría busca.

## Solo el efectivo pasa por la caja

Una transferencia se cobra contra su factura pero no toca el cajón, así que no
entra en el cuadre. Cobrar en efectivo exige caja abierta; los demás métodos, no.

## El conteo de cierre no viene rellenado

La caja no muestra el total esperado antes de pedir el conteo. Enseñar la
respuesta antes de la pregunta convierte el arqueo en un trámite; toda su
utilidad está en que la cifra se descubre.

La diferencia se registra, nunca se corrige. Ver
[Caja](../guia-usuario/caja.md).

## Una reimpresión va marcada como COPIA

Y exige un motivo. Un recibo reimpreso sin marca no se distingue de un segundo
pago, y esa confusión termina siempre en la misma discusión con el ciudadano.

## Un documento emitido conserva su número

Si falla la impresión, el documento **ya existe** con su número. Se reimprime, no
se vuelve a emitir. Emitir otra vez daría dos documentos para un solo hecho.

## La auditoría está encadenada por hash

Cada evento incluye el hash del anterior. Alterar uno rompe la cadena a partir de
ahí, y **Verificar auditoría** dice en qué entrada. No impide manipular la base de
datos por fuera; garantiza que se sepa.

## Restaurar tiene una secuencia obligatoria

Validar el archivo, copia de emergencia del estado actual, confirmación
explícita, restauración, verificación de integridad y registro de lo ocurrido.
Ninguno de esos pasos se puede saltar. Ver
[Restauración](../guia-usuario/restauracion.md).

## La búsqueda de personas ignora tildes

`pena` encuentra `Peña`. En un mostrador nadie escribe la tilde, y un duplicado
creado porque la búsqueda no encontró a alguien cuesta mucho más que la
comparación laxa.
