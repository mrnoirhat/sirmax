---
title: "Restauración"
sidebar_position: 12
description: "Restaurar una copia sin perder el camino de vuelta."
---

# Restauración

Restaurar es **la operación más destructiva de SIRMAX**: descarta todo lo que se
haya hecho desde que se tomó la copia.

Por eso la secuencia no es negociable:

1. **Se valida la copia de destino.** Si su hash no coincide, no se toca nada.
2. **Se toma una copia de emergencia del estado actual.** Automáticamente, se
   haya pedido o no. Es el único camino de vuelta si la restauración resulta ser
   el error.
3. **Se confirma.** SIRMAX no restaura sin una confirmación explícita.
4. **Se restaura**, después de comprobar la integridad del archivo desempaquetado
   con la propia verificación de SQLite.
5. **Se registra**, en la base restaurada.

## Después de restaurar

Hay que **reiniciar SIRMAX**: la base de datos con la que estaba hablando ya no
existe.

La base restaurada es más antigua que su propio historial de copias, así que no
conoce ni la copia de la que vino ni la de emergencia recién tomada. SIRMAX
**las vuelve a inscribir**. Sin eso, la copia que guarda el estado descartado
sería imposible de encontrar desde el sistema que la reemplazó — que es
precisamente para lo que se tomó.

## Si algo falla

Una restauración fallida deja la base **intacta**: el archivo se desempaqueta y
se comprueba antes de reemplazar nada. El intento queda registrado con su motivo.
