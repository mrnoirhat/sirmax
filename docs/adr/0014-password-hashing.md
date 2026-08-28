# 0014 — Hashing de contraseñas: PBKDF2 ahora, Argon2id después

- **Estado:** Aceptado
- **Fecha:** 2026-08-28
- **Decisores:** Ingeniería principal SIRMAX
- **Fase relacionada:** 3 (baseline), 10 (endurecimiento)

## Contexto

SIRMAX autentica a operadoras con usuario y contraseña (master prompt §43). Hace falta un hashing
de contraseñas con sal, resistente a fuerza bruta, ya en la Fase 3. Argon2id es el estándar
recomendado hoy, pero su implementación en la JVM requiere una dependencia de terceros
(`de.mkammerer:argon2-jvm` — LGPL-3.0, con binario nativo; o `password4j` — Apache-2.0, puro Java) y
la revisión de licencia/inventario correspondiente. La Fase 3 debe cerrar sin bloquearse en eso.

## Decisión

- **Fase 3:** `Pbkdf2PasswordHasher` usando **solo el JDK** (`PBKDF2WithHmacSHA256`): sal aleatoria
  de 16 bytes por hash, **210 000 iteraciones**, clave derivada de 256 bits, comparación en tiempo
  constante (`MessageDigest.isEqual`). Sin dependencias nuevas.
- **Formato almacenado:** `pbkdf2-sha256$<iteraciones>$<sal base64>$<dk base64>`, y en la columna
  `app_user.password_algo` la etiqueta `PBKDF2-HMAC-SHA256`.
- **`PasswordHasher` es un puerto.** El dominio guarda un `PasswordHash` (algoritmo + valor
  codificado) y nunca ve texto plano; el texto plano se pasa como `char[]` y se borra tras usarse.
- **Fase 10:** se añade `Argon2idPasswordHasher` como adaptador alternativo. Como cada hash lleva su
  etiqueta de algoritmo, las cuentas existentes siguen funcionando y se puede **re-hashear al
  iniciar sesión** (verificar con el algoritmo antiguo, volver a hashear con el nuevo).
- Las iteraciones de PBKDF2 se revisan periódicamente y se suben; el número está en el hash, así que
  subirlo no rompe cuentas antiguas.

## Consecuencias

**Positivas**
- Cero dependencias nuevas y cero revisión de licencia para cerrar la Fase 3.
- Migración a Argon2id sin romper cuentas, gracias a la etiqueta de algoritmo por hash.
- PBKDF2-HMAC-SHA256 con 210k iteraciones es una configuración aceptable y ampliamente auditada.

**Negativas / costes**
- PBKDF2 es más débil que Argon2id frente a ataques con hardware especializado (GPU/ASIC). Aceptable
  para un sistema local-first en un equipo municipal, y temporal hasta la Fase 10.
- 210k iteraciones añaden ~decenas de ms por verificación; imperceptible en el login, pero los tests
  usan un conteo bajo mediante un constructor de paquete.

## Alternativas consideradas

- **Argon2id ya en Fase 3** — mejor propiedad de seguridad, pero introduce dependencia + revisión de
  licencia + (en `argon2-jvm`) binario nativo por plataforma en el empaquetado. Se pospone a Fase 10.
- **bcrypt** — sólido, pero también dependencia de terceros y límite de 72 bytes de contraseña.
- **scrypt** — dependencia de terceros; sin ventaja clara sobre Argon2id como objetivo final.

## Referencias

- Master prompt §43 "Local security", §10 "Phase 10 — Security".
- OWASP Password Storage Cheat Sheet (PBKDF2-HMAC-SHA256 ≥ 210 000 iteraciones).
