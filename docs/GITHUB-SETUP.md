<!-- SPDX-License-Identifier: AGPL-3.0-or-later -->
# Configuración de GitHub

Cuatro cosas que **solo puede hacer el dueño del repositorio**, porque exigen
permisos de administración que el `GITHUB_TOKEN` de Actions no tiene.

Todo lo demás está automatizado.

---

## 0. Autenticarse una vez

```bash
gh auth login --hostname github.com --git-protocol https --web
```

Elige el scope `admin:repo_hook` / `repo` cuando lo pida; los pasos siguientes
lo necesitan.

---

## 1. Proteger `main`

`main` solo debe contener lo que pasó el Release Gate, y el flujo de tres ramas
([`RELEASE.md`](../RELEASE.md)) depende de que nadie empuje directo.

```bash
gh api -X PUT repos/mrnoirhat/sirmax/branches/main/protection \
  --input docs/branch-protection-main.json
```

Qué activa, y por qué cada cosa:

| Regla | Motivo |
| --- | --- |
| Pull request obligatorio | Impide el `push` directo que el flujo de tres ramas prohíbe. |
| Los cuatro checks en verde | Desktop, Landing, Docs y Security. Un release que no compila no llega a `main`. |
| Checks actualizados con la rama | Evita el merge que pasa por separado y rompe al juntarse. |
| Sin `force push` ni borrado | El historial de `main` es el registro de qué se publicó. |
| Se aplica a administradores | Una protección que el dueño se salta no protege de la prisa del dueño, que es de quien hay que protegerse. |

## 2. Publicar la documentación en GitHub Pages

```bash
gh api -X POST repos/mrnoirhat/sirmax/pages \
  -f 'source[branch]=main' -f 'build_type=workflow'
gh variable set PUBLISH_DOCS --body true --repo mrnoirhat/sirmax
```

Hasta que existan, la documentación se sigue construyendo y validando en cada
push — el workflow simplemente no intenta publicar. Se hizo así a propósito: un
build en rojo porque nadie visitó una página de ajustes diría que el código está
roto cuando no lo está.

El espejo queda en <https://mrnoirhat.github.io/sirmax>. El sitio principal está
en Vercel y **no** depende de esto.

## 3. Cerrar los Dependabot pendientes

Las doce actualizaciones aplicables ya están en `main`, así que Dependabot cierra
solo esos PRs en su siguiente pasada. Para no esperar:

```bash
gh pr list --repo mrnoirhat/sirmax --json number,title --jq '.[].number' \
  | xargs -I{} gh pr close {} --repo mrnoirhat/sirmax \
      --comment "Aplicado en experiment y promovido a main."
```

Dos quedan **abiertos a propósito** — TypeScript 7 y ESLint 10 — porque no son
compatibles con Docusaurus 3.10 ni con `eslint-config-next` 16. Ver el CHANGELOG
de 1.0.1. Ciérralos solo cuando esas dependencias se actualicen.

## 4. Vercel

Dos proyectos, uno por sitio. Con los `vercel.json` que hay en el repositorio,
lo único que hay que fijar es el directorio raíz:

| Proyecto | Root Directory | URL |
| --- | --- | --- |
| `sirmax` | `apps/landing` | <https://sirmax.vercel.app> |
| `sirmax-docs` | `apps/docs` | <https://sirmax-docs.vercel.app> |

Deja activado *Include files outside the Root Directory* — ambos son workspaces
de npm y necesitan el `package-lock.json` de la raíz.

---

## Lo que **no** hace falta configurar

- **Releases.** Salen solos al empujar un tag `vX.Y.Z`: el workflow compila el
  MSI en un runner con WiX, verifica que el runtime quedó dentro, calcula las
  sumas SHA-256 y publica todo.
- **CI.** Los cuatro workflows corren en cada push a las ramas permanentes.
- **Firma del MSI.** Requiere un certificado del ayuntamiento. Sin firmarlo,
  Windows SmartScreen avisará la primera vez; es esperable en software libre sin
  certificado y no impide instalar.
