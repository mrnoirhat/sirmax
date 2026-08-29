// SPDX-License-Identifier: AGPL-3.0-or-later

/** Shared, single-source-of-truth site constants for the SIRMAX landing. */
export const site = {
  name: "SIRMAX",
  fullName: "Sistema Integral de Registros Municipales y Administración eXtensible",
  tagline: "La gestión municipal, simplificada.",
  description:
    "Plataforma open source, local-first y orientada a municipios para gestionar servicios, " +
    "trámites, registros ciudadanos, facturación, pagos, caja, impresión de facturas, auditoría y " +
    "copias de seguridad. Diseñada para ayuntamientos de República Dominicana.",
  url: "https://sirmax.vercel.app",
  /** The version the download buttons link to. Bump on every release. */
  version: "1.0.0",
  repo: "https://github.com/mrnoirhat/sirmax",
  releases: "https://github.com/mrnoirhat/sirmax/releases",
  issues: "https://github.com/mrnoirhat/sirmax/issues",
  discussions: "https://github.com/mrnoirhat/sirmax/discussions",
  contributing: "https://github.com/mrnoirhat/sirmax/blob/main/CONTRIBUTING.md",
  roadmap: "https://github.com/mrnoirhat/sirmax/blob/main/ROADMAP.md",
  // The documentation is a separate Docusaurus site, not a route of this one:
  // linking to "/docs" would 404 on the deployed landing.
  docs: "https://sirmax-docs.vercel.app",
  // The same documentation is mirrored on GitHub Pages, for anyone who would
  // rather read it where the code lives.
  docsMirror: "https://mrnoirhat.github.io/sirmax",
  // Always the newest release, so the landing never advertises a stale version.
  download: "https://github.com/mrnoirhat/sirmax/releases/latest",
  locale: "es-DO",
} as const;

/** Mandatory primary navigation (master prompt §5). */
export const primaryNav = [
  { label: "Producto", href: "#producto" },
  { label: "Características", href: "#caracteristicas" },
  { label: "Documentación", href: site.docs },
  { label: "Comunidad", href: "#comunidad" },
  { label: "GitHub", href: site.repo },
  { label: "Descargar", href: "#descargar" },
] as const;
