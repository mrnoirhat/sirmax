# Desarrollo de SIRMAX

Guía para preparar el entorno, compilar, probar y trabajar día a día. Complementa
[`CONTRIBUTING.md`](./CONTRIBUTING.md) y [`ARCHITECTURE.md`](./ARCHITECTURE.md).

## 1. Requisitos

| Herramienta | Versión | Para qué |
| --- | --- | --- |
| **JDK** | **25** (Temurin/Adoptium recomendado) | `apps/desktop`, `backend` |
| **Node.js** | **≥ 20.11** (LTS) | `apps/landing`, `apps/docs` |
| **npm** | ≥ 10 (viene con Node) | workspaces web |
| **Git** | ≥ 2.40 | control de versiones |
| **Gradle** | _no hace falta instalarlo_ | se usa el wrapper `./gradlew` |

Comprobación rápida:

```bash
java -version     # debe decir 25
node -v           # v20.11+ o v22+
git --version
```

En Windows: `JAVA_HOME` debe apuntar al JDK 25 y `%JAVA_HOME%\bin` estar en el `PATH`.

## 2. Primer arranque

```bash
git clone https://github.com/mrnoirhat/sirmax.git
cd sirmax
git checkout experiment

# Web (desde la raíz del repo): instala landing + docs vía workspaces
npm install

# Escritorio
cd apps/desktop
./gradlew build            # Windows: gradlew.bat build
```

> El wrapper de Gradle (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) se añade en la Fase 1 con
> `gradle wrapper --gradle-version <x>`. Hasta entonces necesitas un Gradle local para el bootstrap.

## 3. Comandos habituales

### Escritorio (`apps/desktop/`)

```bash
./gradlew build                 # compila todos los módulos + pruebas
./gradlew test                  # solo pruebas
./gradlew :sirmax-app:run       # lanza el shell de la aplicación (JavaFX)
./gradlew spotlessApply         # formatea (cuando esté configurado)
./gradlew check                 # pruebas + análisis estático
./gradlew :sirmax-app:jpackage  # instalador Windows (Fase 11; requiere Windows)
```

### Web (desde la raíz del repo)

```bash
npm run landing:dev     # http://localhost:3000
npm run landing:build   # build de producción de la landing
npm run landing:lint
npm run docs:dev        # http://localhost:3001 (Docusaurus)
npm run docs:build
npm run web:build       # landing + docs
```

## 4. Estructura de módulos del escritorio

```text
apps/desktop/
├── settings.gradle.kts          # declara los módulos
├── build.gradle.kts             # configuración común (subprojects)
├── gradle/libs.versions.toml    # catálogo de versiones
├── sirmax-shared/               # Money, Result, identidad, i18n keys
├── sirmax-domain/               # entidades, agregados, invariantes (Java puro)
├── sirmax-application/          # casos de uso + puertos (interfaces)
├── sirmax-infrastructure/       # adaptadores: SQLite, ficheros, PDF, impresión, Drive
├── sirmax-ui/                   # JavaFX: shell, vistas, componentes
└── sirmax-app/                  # composition root + main + jpackage
```

Reglas de dependencia entre capas: ver [`ARCHITECTURE.md` §3](./ARCHITECTURE.md#3-capas-appsdesktop).
Se validan con una prueba de arquitectura.

## 5. Datos locales en desarrollo

- La base SQLite de desarrollo se crea bajo `apps/desktop/local-run/` (ignorada por Git).
- Nunca comitees `*.sqlite`, `*.db`, `/data/`, `/backups/` ni `.env`.
- Datos de ejemplo: solo semillas **claramente etiquetadas** como demo; nunca histórico financiero
  falso presentado como real.

## 6. Flujo diario

Ver [`CONTRIBUTING.md` §3](./CONTRIBUTING.md#3-flujo-diario). Resumen:

```text
pull experiment → leer ADR → inspeccionar código → rama feature/* → cambio pequeño →
pruebas → docs → commit (Conventional Commits) → PR a experiment
```

## 7. Pruebas

| Tipo | Dónde | Herramienta prevista |
| --- | --- | --- |
| Unitarias de dominio/aplicación | `src/test/java` de cada módulo | JUnit 5 + AssertJ |
| Arquitectura (límites de capas) | módulo dedicado | ArchUnit |
| Migraciones (base nueva + actualizada) | `sirmax-infrastructure` | JUnit 5 + SQLite temporal |
| Facturación (numeración, totales, pagos parciales, reembolsos, reimpresión, PDF, impresión) | `sirmax-application` / `sirmax-infrastructure` | JUnit 5 |
| Smoke de UI | `sirmax-ui` | TestFX (headless en CI) |
| Landing | `apps/landing` | lint + typecheck + build |
| Docs | `apps/docs` | build de Docusaurus + validación de enlaces |

## 8. CI

Workflows en [`.github/workflows/`](./.github/workflows/):

- `desktop.yml` — compila Java, pruebas, análisis estático, smoke de empaquetado.
- `landing.yml` — install, lint, typecheck, build.
- `docs.yml` — install, build de Docusaurus, validación de enlaces.
- `security.yml` — auditoría de dependencias, escaneo de secretos, SAST donde sea práctico.

`testing` y `main` requieren todos los checks en verde + revisión.

## 9. Problemas frecuentes

| Síntoma | Causa probable | Solución |
| --- | --- | --- |
| `Unsupported class file major version` | JDK != 25 | Ajusta `JAVA_HOME` al JDK 25 |
| JavaFX no arranca | faltan módulos nativos | usa `:sirmax-app:run` (el plugin de JavaFX resuelve las libs) |
| `npm install` falla en `apps/docs` | Node < 20.11 | actualiza Node a LTS |
| Gradle no encontrado en el bootstrap | wrapper aún no generado | instala Gradle local o genera el wrapper |
