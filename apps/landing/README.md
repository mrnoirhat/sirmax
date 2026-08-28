# SIRMAX Landing

Public landing page for SIRMAX. **Next.js 15** (App Router) + **React 19** + **TypeScript**, exported
as a fully static site and optimized for **Vercel**.

## Commands

Run from the repo root (npm workspaces) or from this directory:

```bash
npm run landing:dev      # http://localhost:3000
npm run landing:build    # static export to apps/landing/out
npm run landing:lint
```

Direct:

```bash
cd apps/landing
npm run dev
npm run build
npm run typecheck
```

## Notes

- **Static only** (`output: "export"` in `next.config.mjs`): the landing never depends on the
  desktop app being online.
- Mandatory primary nav and CTAs (master prompt §5) live in `lib/site.ts` / `components/Nav.tsx`:
  _Producto, Características, Documentación, Comunidad, GitHub, Descargar_ and the prominent
  **VER PROYECTO EN GITHUB** / **Descargar SIRMAX** buttons.
- SEO: metadata + Open Graph in `app/layout.tsx`, JSON-LD `SoftwareApplication`, `app/sitemap.ts`,
  `app/robots.ts`. Update `site.url` / `site.repo` in `lib/site.ts` when the real domain and repo
  slug are fixed.
- Sections implemented: Hero, Problema, Solución, Características, Servicios municipales, Trámites,
  Facturación, Impresión, Local-first, Seguridad, Backups, Arquitectura, Capturas (placeholder),
  Roadmap, Open source, Documentación, Descargar, FAQ, footer.

## Deploy (Vercel)

Set the project **Root Directory** to `apps/landing`. Build command `next build`, output `out`.
`main` → production, `testing` → preview (see [`../../RELEASE.md`](../../RELEASE.md)).
