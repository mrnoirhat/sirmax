# Copias de seguridad y restauración de SIRMAX

SIRMAX es local-first. La aplicación debe seguir siendo útil sin internet, y las funciones que
dependen de la red (Google Drive, actualizaciones, futura sincronización) **fallan de forma elegante
y nunca corrompen el trabajo local**.

## 1. Estrategia de backup

```text
Snapshot consistente de SQLite   (VACUUM INTO / API de backup, nunca copia en caliente)
        ↓
Validación                       (PRAGMA integrity_check + comprobación de esquema)
        ↓
Compresión
        ↓
Cifrado (opcional, recomendado)  (clave del municipio; secreto local seguro)
        ↓
Hash de integridad               (SHA-256 del artefacto final)
        ↓
Copia local                      (carpeta de backups fuera de los binarios)
        ↓
Copia opcional en Google Drive   (la cuenta de Google del usuario es la dueña del destino)
```

Cada backup registra: fecha/hora, versión de esquema, versión de la app, tamaño, hash, si está
cifrado y si se subió a Drive. Todo ello queda además en el **log de auditoría**.

## 2. Contenido de un backup

- La base de datos SQLite completa.
- Metadatos: versión de esquema y de aplicación, perfil de institución mínimo para identificar el
  origen, marca de tiempo, hash.
- (Según política) adjuntos/documentos escaneados referenciados por la base, o su ruta si se
  gestionan aparte.

## 3. Historial y validación

- La UI muestra un historial de backups con estado (`OK`, `INVÁLIDO`, `SUBIDO`, `SOLO_LOCAL`).
- Un backup marcado inválido no se puede usar para restaurar sin una anulación explícita y auditada.
- Verificación periódica del hash de los backups locales recientes.

## 4. Google Drive (opcional)

- Autenticación con **Google OAuth**; los tokens se guardan como secreto local seguro (se evalúa
  Windows Credential Manager / DPAPI), **nunca** en el repositorio ni en la base en claro.
- Carpeta de destino dedicada dentro del Drive del usuario.
- Programación automática configurable (p. ej. diaria al cerrar caja).
- **Nunca** se suben datos sensibles a un servicio externo de forma silenciosa: la subida a Drive es
  una decisión explícita del municipio y queda auditada.
- Sin conexión: el backup local se realiza igual; la subida queda pendiente y se reintenta.

## 5. Restauración segura

Orden **obligatorio**:

```text
1. Crear un backup de emergencia del estado actual.
2. Validar el backup destino (hash + integrity_check + versión de esquema compatible).
3. Pedir confirmación explícita a la persona operadora (con resumen legible: fecha, origen, tamaño).
4. Restaurar.
5. Ejecutar chequeos de integridad.
6. Reconstruir índices/caché si es necesario.
7. Registrar la restauración en el log de auditoría (quién, cuándo, desde qué backup, motivo).
```

Si la versión de esquema del backup es anterior, tras restaurar se ejecuta la cadena de migraciones
y se vuelve a validar.

## 6. Escenario de fallo (aceptación)

```text
El backup empieza → falla →
  aviso comprensible para la persona usuaria (sin jerga técnica) →
  la base de datos actual permanece intacta →
  opción de reintentar →
  el error técnico se registra en los logs →
  evento de auditoría
```

## 7. Qué NO hacer

- No copiar el fichero `.sqlite` mientras la aplicación escribe.
- No borrar el backup de emergencia hasta confirmar que la restauración fue correcta.
- No subir a Drive sin consentimiento configurado del municipio.
- No guardar tokens ni claves de cifrado en texto plano.
