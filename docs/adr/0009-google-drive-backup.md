# 0009 — Backup opcional en Google Drive con OAuth del usuario

- **Estado:** Aceptado
- **Fecha:** 2026-08-27
- **Decisores:** Ingeniería principal SIRMAX
- **Fase relacionada:** 9

## Contexto

SIRMAX debe seguir siendo útil sin internet. Además del backup local obligatorio, el master prompt
pide una copia **opcional** en Google Drive, propiedad de la cuenta de Google del usuario, sin subir
datos sensibles de forma silenciosa.

## Decisión

El backup en la nube es una función **opcional y explícita**:

- Autenticación con **Google OAuth 2.0** (flujo de aplicación de escritorio, `PKCE`, _loopback_
  local). El usuario concede acceso solo a una carpeta de aplicación / carpeta dedicada de su Drive.
- Los tokens (`refresh_token`) se guardan como **secreto local seguro** (se evalúa Windows Credential
  Manager / DPAPI), nunca en el repositorio ni en la base en claro.
- El artefacto que se sube es el mismo pipeline del backup local: snapshot → validación → compresión
  → cifrado (recomendado) → hash. Drive recibe un blob ya cifrado + un fichero de metadatos.
- Programación configurable (p. ej. al cerrar caja / diaria). Sin conexión: el backup local se hace
  igual y la subida queda pendiente con reintentos.
- Toda subida/activación/desactivación queda **auditada**. La primera vez se pide consentimiento
  explícito del municipio con texto claro sobre qué se envía.
- El `client_secret` de OAuth de la build oficial se trata como configuración de distribución, no se
  comitea; los forks configuran el suyo.

## Consecuencias

**Positivas**
- Recuperación ante desastre sin infraestructura propia del proyecto.
- El municipio mantiene el control y la propiedad de los datos.

**Negativas / costes**
- Complejidad de OAuth de escritorio y de almacenamiento seguro de tokens.
- Dependencia de la API de Google Drive y de sus cambios.
- Verificación de la app de Google para _scopes_ sensibles (proceso administrativo).

## Alternativas consideradas

- **Solo backup local** — insuficiente ante robo/incendio/fallo de disco.
- **Almacenamiento gestionado por el proyecto (S3, etc.)** — implica que el proyecto custodie datos
  ciudadanos; contrario al principio de propiedad del municipio y a "local-first".
- **Otros proveedores (OneDrive, Dropbox)** — posibles a futuro mediante un puerto `CloudBackupPort`;
  Drive primero por prevalencia y por el master prompt.

## Referencias

- Master prompt §0.4/§1.5, §41, §42, §43, Fase 9.
