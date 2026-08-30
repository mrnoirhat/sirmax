---
title: "Desarrollo"
sidebar_position: 6
description: "Cómo está construido SIRMAX y cómo trabajar en él."
---

# Desarrollo

## Puesta en marcha

Hace falta **JDK 25** y **Node 22+**. Gradle y las dependencias las trae el
wrapper.

```bash
git clone https://github.com/mrnoirhat/sirmax.git
cd sirmax
npm install
cd apps/desktop && ./gradlew build
```

`build` compila los siete módulos y pasa la suite completa. Para arrancar la
aplicación:

```bash
./gradlew :sirmax-app:run
```

## El repositorio

```text
apps/desktop/    aplicación de escritorio (Gradle, 7 módulos)
apps/landing/    sitio público (Next.js)
apps/docs/       esta documentación (Docusaurus)
database/        migraciones SQL numeradas
docs/            ADRs y documentos de proceso
brand/           el logotipo, en SVG
tools/           scripts de firma
```

## Por dónde seguir

- **[Arquitectura](./arquitectura.md)** — las capas y por qué apuntan como apuntan.
- **[Modelo de dominio](./modelo-de-dominio.md)** — las entidades y sus reglas.
- **[Base de datos](./base-de-datos.md)** — esquema y migraciones.
- **[Motor de flujo](./motor-de-flujo.md)** y **[Motor de tasas](./motor-de-tasas.md)** — los dos motores configurables.
- **[Pruebas](./pruebas.md)** — qué se prueba y cómo.
- **[Integraciones](./integraciones.md)** e **[API futura](./api-futura.md)**.
- **[Releases](./releases.md)** y **[Contribución](./contribucion.md)**.

## Las decisiones ya tomadas

Están en los [ADR](https://github.com/mrnoirhat/sirmax/blob/main/docs/adr): Java 25, JavaFX, SQLite,
Gradle, arquitectura modular, motor de servicios, motor de flujo, motor de tasas
y copia en Drive. Léelos antes de proponer cambiarlas — llevan escrito el motivo.
