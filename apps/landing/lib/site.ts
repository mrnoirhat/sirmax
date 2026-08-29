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
  url: "https://sirmax.org",
  repo: "https://github.com/mrnoirhat/sirmax",
  releases: "https://github.com/mrnoirhat/sirmax/releases",
  issues: "https://github.com/mrnoirhat/sirmax/issues",
  discussions: "https://github.com/mrnoirhat/sirmax/discussions",
  contributing: "https://github.com/mrnoirhat/sirmax/blob/main/CONTRIBUTING.md",
  roadmap: "https://github.com/mrnoirhat/sirmax/blob/main/ROADMAP.md",
  // The documentation is a separate Docusaurus site on GitHub Pages, not a route
  // of this one. Linking to "/docs" would 404 on the deployed landing.
  docs: "https://mrnoirhat.github.io/sirmax/",
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
