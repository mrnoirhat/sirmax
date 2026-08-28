# Architecture Decision Records (ADR)

Este directorio registra las decisiones de arquitectura significativas de SIRMAX. Cada ADR es
inmutable una vez aceptado: si una decisión cambia, se crea un ADR nuevo que **supersede** al
anterior y se actualiza el estado.

## Formato

Usa [`0000-adr-template.md`](./0000-adr-template.md) como plantilla:

```bash
cp docs/adr/0000-adr-template.md docs/adr/00NN-titulo-corto.md
```

Estados posibles: `Propuesto`, `Aceptado`, `Rechazado`, `Obsoleto`, `Supersedido por 00NN`.

## Índice

| # | Título | Estado |
| ---: | --- | --- |
| [0001](./0001-java-25.md) | Java 25 LTS como baseline del escritorio y el dominio | Aceptado |
| [0002](./0002-javafx-desktop.md) | JavaFX para la UI de escritorio (no Electron) | Aceptado |
| [0003](./0003-sqlite-local-first.md) | SQLite como base de datos embebida local-first | Aceptado |
| [0004](./0004-gradle.md) | Gradle (Kotlin DSL) para el build del escritorio | Aceptado |
| [0005](./0005-modular-domain-architecture.md) | Arquitectura Java modular y por capas | Aceptado |
| [0006](./0006-service-definition-engine.md) | Motor de definición de servicios configurable | Aceptado |
| [0007](./0007-workflow-engine.md) | Motor de flujo de trabajo pragmático, no genérico | Aceptado |
| [0008](./0008-fee-engine.md) | Motor de tasas versionable, separado de la facturación | Aceptado |
| [0009](./0009-google-drive-backup.md) | Backup opcional en Google Drive con OAuth del usuario | Aceptado |
| [0010](./0010-agplv3-or-later.md) | Licencia AGPL-3.0-or-later para el código | Aceptado |
| [0011](./0011-monorepo.md) | Monorepo único (escritorio + landing + docs) | Aceptado |
| [0012](./0012-three-branch-release-flow.md) | Flujo de release con tres ramas permanentes | Aceptado |
| [0013](./0013-ui-programmatic-javafx.md) | UI: JavaFX programático (sin FXML) y navegación/i18n sin framework | Aceptado |
