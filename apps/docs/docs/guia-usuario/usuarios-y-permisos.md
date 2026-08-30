---
title: "Usuarios y permisos"
sidebar_position: 12
description: "Quién puede hacer qué, y por qué conviene no dárselo todo a todos."
---

# Usuarios y permisos

## Cómo funciona

Un usuario tiene uno o más **roles**; cada rol trae un conjunto de **permisos**;
cada permiso habilita una acción concreta. La interfaz **esconde** lo que tu
cuenta no puede hacer: si un botón no aparece, es permiso, no avería.

La comprobación se hace además en el momento de ejecutar la acción, no solo al
dibujar la pantalla. Ocultar un botón es comodidad; la seguridad está detrás.

## El personal

**Administración → Departamentos**, panel *Personal*: quién existe, con qué
usuario entra y qué roles tiene. Es de solo lectura, y contestar «quién trabaja
en Planeamiento» es justo la pregunta que se tiene al mirar departamentos.

## Los permisos

| Permiso | Deja |
| --- | --- |
| `person.read` / `person.write` | Ver / registrar ciudadanos |
| `service.read` / `service.configure` | Ver / crear y publicar servicios |
| `procedure.read` / `procedure.work` / `procedure.decide` | Ver / trabajar / resolver trámites |
| `invoice.issue` / `invoice.void` / `invoice.reprint` | Emitir / anular / reimprimir |
| `payment.register` / `payment.refund` | Cobrar / devolver |
| `cash.session.open` / `cash.session.close` | Abrir / cerrar caja |
| `document.register` / `document.certify` | Registrar / certificar documentos |
| `fee.override` | Aplicar descuentos |
| `department.manage` / `user.manage` / `role.manage` | Administrar la organización |
| `config.manage` | Cambiar la configuración |
| `backup.run` / `backup.restore` | Copias / restauración |
| `audit.read` | Consultar la auditoría |
| `report.view` | Ver reportes |

## Cómo repartirlos

**Cajero** — `payment.register`, `cash.session.*`, `invoice.issue`,
`invoice.reprint`, `person.read`, `procedure.read`.

**Analista** — `procedure.work`, `person.*`, `document.register`,
`service.read`.

**Jefe de departamento** — lo del analista más `procedure.decide`,
`invoice.void`, `payment.refund`, `report.view`.

**Administrador** — todo.

:::warning Que no todo el mundo sea administrador
El reparto no es burocracia: es lo que hace que la auditoría signifique algo. Si
todas las cuentas pueden anular facturas, saber *quién* anuló una deja de acotar
nada. Y si todos comparten una cuenta, no hay nada que saber.
:::

## Separaciones que conviene mantener

- **Quien cobra no debería anular.** `payment.register` sin `invoice.void`.
- **Quien restaura debería ser una sola persona.** `backup.restore` reemplaza la
  base de datos entera.
- **`config.manage` cambia lo que sale impreso** en cada factura.

## Contraseñas

Mínimo configurable en **Configuración → Seguridad** (12 por defecto). Tras
varios intentos fallidos la cuenta se bloquea un rato; los intentos quedan
registrados. Ver [Seguridad](./seguridad.md).
