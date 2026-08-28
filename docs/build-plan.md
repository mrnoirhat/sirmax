# Plan de build de SIRMAX

Cómo se construye el producto, en qué orden y con qué criterio de "hecho". Complementa
[`ROADMAP.md`](../ROADMAP.md) (estado) y [`RELEASE.md`](../RELEASE.md) (promoción de ramas).

## 1. Regla de oro

Se construye **por fases**, con compilación y pruebas **después de cada fase**, manteniendo la
documentación al día y todos los flujos críticos ejecutables de extremo a extremo. Nunca un
_one-shot_ gigante. El repositorio debe quedar siempre en un estado en el que otra persona puede
continuar de inmediato.

## 2. Orden de fases y "Definition of Done" por fase

| Fase | Entregable | Hecho cuando… |
| ---: | --- | --- |
| **0** | Discovery + arquitectura (este documento, ADRs, glosario, dominio, ERD, módulos, UX) | Los docs existen y son coherentes; CI skeleton definido |
| **1** | Fundación del repo: monorepo, Gradle base, Next.js base, Docusaurus base, GitHub Actions, licencias, README, docs de contribución | `./gradlew build` compila los módulos vacíos; `npm run web:build` compila; los 4 workflows corren |
| **2** | Shell de escritorio + Design System | El shell navega, tiene tema/tipografía/componentes/estados/atajos; smoke test de UI en verde |
| **3** | Dominio central + base de datos | Migración baseline aplica en base nueva y actualizada; entidades núcleo + auditoría con pruebas |
| **4** | Motor configurable de servicios | Se publica una `ServiceDefinitionVersion`; requisitos y workflow se evalúan con pruebas |
| **5** | Ciudadano + front-office | Recorrido `buscar persona → servicio → trámite → requisitos → avanzar workflow` funciona E2E |
| **6** | Facturación, pagos y caja + **2 plantillas de impresión** + Windows printing + PDF + marca | Escenarios F (recibo angosto) y G (factura Letter); pruebas de numeración/totales/pagos parciales/reembolsos/anulaciones/reimpresión/PDF/branding/snapshot/permisos/auditoría |
| **7** | Módulos municipales (prioridad: Registro Civil/Documentos → Certificaciones → Urbano → Catastro → Cementerio → Mercados → Permisos → Movilidad → Operaciones/Residuos) | Cada módulo se enchufa al núcleo (trámite/documentos/finanzas/auditoría), no crea arquitectura paralela |
| **8** | Documentos/PDF/impresión (plantillas oficiales, preview, perfiles, QR, reimpresión auditada) | Escenarios A–E generan su documento; reimpresión no duplica |
| **9** | Backup/recuperación/Google Drive | Backup local con validación+compresión+cifrado+hash; restauración segura; escenario de fallo de backup; flujo OAuth de Drive |
| **10** | Seguridad/auditoría/fiabilidad | Hashing, sesión, RBAC, auditoría inmutable, log seguro, validación de ficheros, secretos, chequeos de dependencias, pruebas de recuperación |
| **11** | Empaquetado Windows | Instalador `.msi/.exe` con runtime embebido; instala/actualiza sin perder datos; desinstala limpio |
| **12** | Landing + Docs productivos | Landing lista para Vercel (SEO, OG, sitemap, robots, CWV); Docusaurus desplegable; enlaces cruzados |
| **13** | Hardening | Auditorías de regresión, UX, rendimiento, impresión, backup/restore, migraciones, accesibilidad, documentación |
| **14** | Release 1.0 | Pasa el Release Gate de `RELEASE.md`; artefactos generados |

Una funcionalidad está **completa** solo con: modelo de dominio + caso de uso + persistencia +
validación + autorización + auditoría + manejo de errores + estados (loading/empty/error/success) +
pruebas + documentación + accesibilidad + UX consistente + migración si cambia la BD + integración en
el recorrido real + compatibilidad de empaquetado cuando aplica.

## 3. Ritmo de trabajo (por cambio)

```text
pull experiment → leer ADR/docs → inspeccionar código existente → rama feature/* →
cambio pequeño y coherente → pruebas → corregir → actualizar docs →
commit Conventional Commits → PR a experiment
```

Promoción `experiment → testing` cuando hay un hito coherente. Estabilizar `testing`. Promoción
`testing → main` para release (Release Gate).

## 4. MVP — probar el bucle completo, no una pila de módulos

```text
Ciudadano → Servicio → Trámite → Validación de requisitos → Tasa → Factura → Pago →
Impresión → Documento oficial → Auditoría → Backup
```

Y un proceso **gratuito**:

```text
Ciudadano → Solicitud → Asignación → Resolución → Cierre → Auditoría
```

Después, los módulos especializados se enchufan a ese bucle.

## 5. CI (esqueleto Fase 1)

| Workflow | Dispara en (paths) | Pasos |
| --- | --- | --- |
| `desktop.yml` | `apps/desktop/**`, `database/**`, `.github/workflows/desktop.yml` | setup-java 25 · `./gradlew build` · pruebas · análisis estático · smoke de empaquetado |
| `landing.yml` | `apps/landing/**`, `package-lock.json` | setup-node · `npm ci` · lint · typecheck · `next build` |
| `docs.yml` | `apps/docs/**`, `package-lock.json` | setup-node · `npm ci` · `docusaurus build` · validación de enlaces |
| `security.yml` | push/PR + programado semanal | auditoría de dependencias · escaneo de secretos · SAST (CodeQL) donde sea práctico |

`testing` y `main` requieren los 4 en verde + revisión.

## 6. Riesgos y mitigaciones

| Riesgo | Mitigación |
| --- | --- |
| Impresión en impresoras de impacto reales varía mucho | Perfiles de impresora configurables; plantilla angosta diseñada para monocromo de baja resolución; pruebas con PDF de referencia + validación manual en Fase 6/8/13 |
| `jpackage`/`jlink` multiplataforma es delicado | Aislar en Fase 11; CI de empaquetado desde el principio como _smoke_ |
| Sobre-configuración del motor de servicios | Plantillas semilla + valores por defecto + editor guiado |
| Divergencia de las 3 ramas | Merges de promoción disciplinados; nada de commits directos a `testing`/`main` |
| Reglas legales jurisdiccionales | No inventar; hacer configurable y marcar para verificación legal |
| Toolchains mixtas (JDK + Node) | Workflows separados con filtros de ruta; `DEVELOPMENT.md` claro |
