---
title: "API futura"
sidebar_position: 8
description: "Qué haría falta para exponer SIRMAX, y por qué todavía no existe."
---

# API futura

**Hoy no hay API.** SIRMAX es una aplicación de escritorio contra una base de
datos local, y esta página existe para decir qué habría que resolver antes de que
la hubiera — no para describir algo que ya se pueda usar.

## Por qué no existe todavía

No es que falte tiempo. Es que exponer una API cambia el modelo de seguridad
entero: hoy el límite es **quién tiene acceso al equipo**, y una API lo convierte
en autenticación, autorización por petición, límites de tasa y superficie
expuesta. Ninguna de esas piezas se improvisa sobre un diseño que no las previó.

La demanda tampoco está clara: un ayuntamiento pequeño rara vez tiene otro
sistema con el que integrarse.

## Los tres casos que la justificarían

**Verificación pública de documentos.** Un ciudadano con un certificado
comprueba, escaneando el QR, que es auténtico. Solo necesita una consulta de
lectura, sin datos personales: código válido, tipo de documento, fecha, si está
anulado.

Es el más valioso y el de menor riesgo, y sería el primero.

**Consulta del estado de un trámite.** El ciudadano mira si su expediente avanzó
sin desplazarse. Requiere identificarlo sin crear cuentas.

**Integración contable.** Volcar cobros al sistema del ayuntamiento. Hoy se cubre
razonablemente exportando CSV.

## Qué habría que decidir

**Dónde vive.** Un ayuntamiento sin servidor no puede publicar su equipo en
internet. Probablemente haga falta un componente aparte que reciba solo lo
publicable, lo que a su vez plantea cómo sincronizarlo sin romper local-first.

**Autenticación.** Tokens por instalación, rotables, revocables.

**Qué se expone.** Por defecto nada. Cada campo publicado, una decisión explícita.
Un endpoint que devuelve la entidad entera «por comodidad» acaba publicando
cédulas.

**Versionado.** Una API pública no se puede romper con cada release.

## Mientras tanto

El código está preparado en lo que importa: los casos de uso son independientes
de la interfaz y devuelven `Result`, así que una capa HTTP los invocaría igual
que lo hace JavaFX. No hay lógica de negocio en las vistas que hubiera que
reescribir.

Si tu ayuntamiento tiene un caso concreto,
[abre una issue](https://github.com/mrnoirhat/sirmax/issues) contándolo. Un caso
real ordena esta lista mejor que cualquier suposición.
