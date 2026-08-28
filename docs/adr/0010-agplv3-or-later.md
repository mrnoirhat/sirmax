# 0010 — Licencia AGPL-3.0-or-later para el código

- **Estado:** Aceptado
- **Fecha:** 2026-08-27
- **Decisores:** Ingeniería principal SIRMAX
- **Fase relacionada:** 0–1

## Contexto

El master prompt fija **GNU Affero General Public License v3.0 or later** (`AGPL-3.0-or-later`) para
el código del proyecto, salvo que una dependencia obligue a una frontera compatible. La marca y la
documentación se gobiernan por separado.

## Decisión

- El código de SIRMAX se licencia bajo **AGPL-3.0-or-later**. `LICENSE` contiene el texto oficial de
  la AGPLv3.
- Cada archivo fuente relevante lleva una cabecera SPDX: `// SPDX-License-Identifier: AGPL-3.0-or-later`.
- Se mantiene un inventario de licencias de dependencias en `THIRD_PARTY_LICENSES.md` y se rechazan
  dependencias incompatibles (p. ej. SSPL, BUSL sin excepción aplicable, "non-commercial").
- La marca y el logo **SIRMAX** se reservan vía `TRADEMARK_POLICY.md` (independiente de la licencia
  de código).
- La documentación puede adoptar una licencia de documentación libre distinta si las personas
  mantenedoras lo deciden; se documentaría explícitamente.
- **Aclaración de marketing:** la AGPL **no** obliga a las personas contribuyentes a enviar sus
  cambios al repositorio original; establece condiciones de copyleft para el uso y la distribución
  del software cubierto, incluido el escenario de servicio en red. No se afirmará lo contrario.

## Consecuencias

**Positivas**
- Copyleft fuerte: las mejoras distribuidas u operadas como servicio permanecen libres.
- Alineado con el mandato del master prompt y con software de administración pública.

**Negativas / costes**
- Algunas empresas evitan integrar AGPL en productos propietarios (efecto buscado, no un defecto).
- Hay que vigilar la compatibilidad de cada dependencia (p. ej. evitar iText comercial; elegir libs
  PDF compatibles).
- Un futuro `backend/` como servicio en red queda cubierto por la cláusula de red de la AGPL (debe
  ofrecer su código).

## Alternativas consideradas

- **GPL-3.0-only** — no cubre el caso de "software como servicio" sin distribución de binarios.
- **Apache-2.0 / MIT** — permisivas; permitirían forks propietarios cerrados, no deseado aquí.
- **AGPL-3.0-only** — se prefiere `-or-later` para poder adoptar versiones futuras de la licencia.

## Referencias

- Master prompt §8, §60, §74.
