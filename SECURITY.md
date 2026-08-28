# Política de Seguridad de SIRMAX

SIRMAX gestiona datos de ciudadanos, expedientes administrativos y transacciones financieras
municipales. La seguridad es un requisito de producto, no un extra.

## Versiones con soporte

El proyecto está en fase de construcción previa a la 1.0. Hasta la primera release estable, solo la
rama `main` recibe correcciones de seguridad. A partir de la 1.0 se documentará aquí la matriz de
versiones soportadas.

| Versión | Soporte de seguridad |
| --- | --- |
| `main` (pre-1.0) | ✅ |
| `testing`, `experiment` | ⚠️ solo si el fallo también está en `main` |

## Reportar una vulnerabilidad

**No abras un issue público para vulnerabilidades.**

1. Usa **GitHub Security Advisories** del repositorio: pestaña _Security → Report a vulnerability_
   (divulgación privada coordinada).
2. Alternativamente, contacta de forma privada a las personas responsables del proyecto indicadas en
   [`SUPPORT.md`](./SUPPORT.md).

Incluye, si es posible:

- descripción del problema y su impacto;
- pasos para reproducirlo o prueba de concepto;
- versión / rama / commit afectado;
- configuración relevante (SO, impresora, modo de backup, etc.).

### Qué esperar

- **Acuse de recibo:** en un plazo objetivo de 5 días hábiles.
- **Evaluación inicial:** en un plazo objetivo de 10 días hábiles.
- **Corrección coordinada:** se acuerda una fecha de divulgación con quien reporta.
- Se dará crédito en las notas de versión salvo que se solicite lo contrario.

## Alcance

En alcance:

- La aplicación de escritorio (`apps/desktop`) y su empaquetado para Windows.
- El manejo local de credenciales y secretos (p. ej. tokens de Google Drive).
- El cifrado e integridad de las copias de seguridad.
- La numeración de facturas, la integridad financiera y la inmutabilidad de la auditoría.
- La landing (`apps/landing`) y la documentación (`apps/docs`) en cuanto a XSS, fugas de datos o
  cadena de suministro.

Fuera de alcance:

- Configuraciones inseguras hechas por la persona operadora en contra de la documentación.
- Ataques que requieren acceso físico ya privilegiado al equipo Windows del municipio.
- Vulnerabilidades en dependencias sin ruta de explotación demostrable en SIRMAX (repórtalas igual;
  se evaluarán).

## Prácticas del proyecto

- Sin secretos en el repositorio (escaneo de secretos en CI).
- Hash de contraseñas con sal donde se use autenticación por contraseña.
- Sesión con expiración/bloqueo y control de acceso por roles (RBAC).
- Validación de archivos importados.
- Auditoría inmutable desde la UI normal.
- Auditoría de dependencias y SAST donde sea práctico (ver [`.github/workflows/security.yml`](./.github/workflows/security.yml)).
- Almacenamiento seguro de secretos locales (se evalúa Windows Credential Manager / DPAPI).
