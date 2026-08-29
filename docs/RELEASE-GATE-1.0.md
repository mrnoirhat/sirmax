<!-- SPDX-License-Identifier: AGPL-3.0-or-later -->
# Release Gate — SIRMAX 1.0

Verificación de la puerta que [`RELEASE.md`](../RELEASE.md) §5 y el master prompt §13 exigen antes
de que `main` reciba la 1.0.

Cada línea dice **dónde está la evidencia**. Un check sin prueba detrás es una opinión.

_Verificado el 2026-08-29 sobre `experiment`._

---

## Estado

| # | Check obligatorio | Estado | Evidencia |
| --: | --- | :---: | --- |
| 1 | Build de escritorio Java en Windows limpio | ✅ | `./gradlew build` verde; workflow **Desktop** sobre `windows-latest` |
| 2 | Migraciones (base nueva y actualizada) | ✅ | `MigrationRunnerTest` (nueva, actualización incremental, rechazo de orden y de deriva de checksum) · `MigrationAuditTest` (idempotencia, integridad, FK) |
| 3 | Pruebas de facturación | ✅ | `InvoiceTest`, `CashSessionTest`, `MunicipalLoopIT` (parcial, cambio, devolución, anulación, cuadre) |
| 4 | Unicidad de número de factura | ✅ | `ux_invoice_number` + `NumberingSequenceTest` + `MunicipalLoopIT.aServiceCannotBeInvoicedTwice…` |
| 5 | Integridad de pagos | ✅ | `Invoice` congela su historia al emitir; devolución = fila nueva, nunca edición · `MunicipalLoopIT` |
| 6 | Impresión angosta (Modelo A) | ⚠️ | `NarrowReceiptLayoutTest` (58 y 80 mm, sin desbordes, sin truncar) y PDF real. **Falta salida sobre impresora de impacto física** |
| 7 | Impresión US Letter (Modelo B) | ⚠️ | `DocumentPrintingIT` genera el PDF real. **Falta salida sobre impresora de oficina física** |
| 8 | Generación de PDF | ✅ | `DocumentPrintingIT.bothMandatoryFormatsProduceRealPdfs` comprueba la cabecera `%PDF-` |
| 9 | Marca institucional en el documento | ✅ | `DocumentPrintingIT.anIssuedDocumentCarriesItsOwnFrozenBranding` |
| 10 | Reimpresión sin duplicar transacciones | ✅ | `DocumentPrintingIT.reprintingNeverRenumbersAndIsMarkedAsACopy` |
| 11 | Autorización / permisos | ✅ | Cada caso de uso comprueba su permiso; `SecurityHardeningIT`, `FrontOfficeTest`, `AccessibilityAuditTest` |
| 12 | Registros de auditoría | ✅ | `MunicipalLoopIT.everyFinancialStepLeavesAnAuditEntry` · `SecurityHardeningIT` |
| 13 | Backup y restauración validados | ✅ | `SqliteBackupEngineTest`, `BackupRecoveryIT` sobre ficheros reales |
| 14 | Flujo de Google Drive | ⚠️ | Adaptador real sobre la API REST; **no verificable en CI** (requiere credenciales del municipio). `BackupRecoveryIT` comprueba que sin cuenta conectada la subida se **rechaza**, no se simula |
| 15 | Build de landing | ✅ | Workflow **Landing**; `next build` export estático |
| 16 | Build de Docusaurus | ✅ | Workflow **Docs**; `onBrokenLinks: "throw"` |
| 17 | GitHub Actions en verde | ✅ | Los cuatro workflows sobre `experiment` |
| 18 | Documentación al día | ✅ | Guía de usuario escrita contra lo construido · [`HARDENING.md`](./HARDENING.md) · [`PACKAGING.md`](./PACKAGING.md) |
| 19 | Sin secretos comiteados | ✅ | `gitleaks` sobre todo el historial en cada push (workflow **Security**) |
| 20 | Notas de release actualizadas | ✅ | `CHANGELOG.md` |
| 21 | Empaquetado | ✅ | Imagen autocontenida **arrancada en Windows 11 sin Java en el `PATH`**: aplicó las diez migraciones y abrió la ventana. MSI configurado (requiere WiX) |

---

## Los tres ⚠️, sin adornos

Ninguno es un fallo. Los tres son cosas que **no se pueden probar honestamente en
CI**, y decirlo es preferible a marcarlas en verde:

### 6 y 7 — impresión sobre hardware físico

Se verifica que el PDF es correcto y que la plantilla angosta no desborda ni
trunca en 32 ni en 42 columnas. Ninguna prueba automatizada sustituye a poner
papel en una impresora de impacto y mirar el resultado. **Antes de instalar en un
ayuntamiento**: imprimir un recibo y una factura y comprobar márgenes,
legibilidad y corte del rollo.

### 14 — Google Drive

El adaptador hace llamadas REST reales y se equivoca de forma ruidosa cuando algo
falla, pero necesita un client OAuth y un refresh token del municipio. Fabricar
credenciales para que el test pasara sería exactamente la funcionalidad falsa que
§1.2 prohíbe. Lo que sí se prueba: **sin cuenta conectada, activar la subida se
rechaza** en vez de quedar a medio configurar.

---

## Alcance de la 1.0

Lo que esta versión **es**: el bucle municipal completo — ciudadano, trámite,
requisitos, tasa, factura, pago, caja, documento oficial, impresión, auditoría y
copia de seguridad — más los tres modelos compartidos que sostienen los diez
módulos municipales, empaquetado para Windows sin dependencias.

Lo que **no** es, y no pretende ser:

- No es un sistema multiusuario en red. Es local-first y monousuario por diseño.
- No es un portal ciudadano ni una API. Están en el roadmap post-1.0.
- **No declara cumplimiento legal ni fiscal de ninguna jurisdicción** (§60). Es
  una herramienta de gestión; la conformidad la determina cada municipio con su
  asesoría.
