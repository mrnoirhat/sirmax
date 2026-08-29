// SPDX-License-Identifier: AGPL-3.0-or-later
import { themes as prismThemes } from "prism-react-renderer";
import type { Config } from "@docusaurus/types";
import type * as Preset from "@docusaurus/preset-classic";

const REPO = "https://github.com/mrnoirhat/sirmax";
const LANDING = "https://sirmax.vercel.app";

// The documentation is published twice, and the two need different base paths:
// Vercel serves it at the root of its own domain, GitHub Pages serves it under
// /sirmax/. A single hard-coded baseUrl silently breaks every asset URL on
// whichever of the two it was not written for, so the target picks it.
const target = process.env.DOCS_TARGET ?? "vercel";
const deployment =
  target === "github-pages"
    ? { url: "https://mrnoirhat.github.io", baseUrl: "/sirmax/" }
    : { url: "https://sirmax-docs.vercel.app", baseUrl: "/" };

const config: Config = {
  title: "SIRMAX",
  tagline: "La gestión municipal, simplificada.",
  // favicon: "img/favicon.ico", // added with the real asset in Phase 12

  url: deployment.url,
  baseUrl: deployment.baseUrl,

  organizationName: "mrnoirhat",
  projectName: "sirmax",

  onBrokenLinks: "throw",

  i18n: {
    defaultLocale: "es",
    locales: ["es"],
  },

  markdown: {
    hooks: {
      onBrokenMarkdownLinks: "throw",
    },
  },

  // @docusaurus/theme-mermaid is added back in Phase 12 when the docs site gets
  // its own diagrams (the domain diagrams live in the repo's /docs folder for now).

  presets: [
    [
      "classic",
      {
        docs: {
          routeBasePath: "/",
          sidebarPath: "./sidebars.ts",
          editUrl: `${REPO}/tree/experiment/apps/docs/`,
        },
        blog: false,
        theme: {
          customCss: "./src/css/custom.css",
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    colorMode: {
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: "SIRMAX",
      items: [
        { type: "docSidebar", sidebarId: "docs", position: "left", label: "Documentación" },
        { href: LANDING, label: "Sitio web", position: "right" },
        { href: `${REPO}/releases/latest`, label: "Descargar", position: "right" },
        { href: REPO, label: "GitHub", position: "right" },
      ],
    },
    footer: {
      style: "dark",
      links: [
        {
          title: "Documentación",
          items: [
            { label: "Introducción", to: "/" },
            { label: "Instalación", to: "/instalacion" },
            { label: "Primeros pasos", to: "/primeros-pasos" },
          ],
        },
        {
          title: "Proyecto",
          items: [
            { label: "Sitio web", href: LANDING },
            { label: "Descargar SIRMAX", href: `${REPO}/releases/latest` },
            { label: "GitHub", href: REPO },
            { label: "Issues", href: `${REPO}/issues` },
            { label: "Roadmap", href: `${REPO}/blob/main/ROADMAP.md` },
            { label: "Contribuir", href: `${REPO}/blob/main/CONTRIBUTING.md` },
          ],
        },
      ],
      copyright: `© ${new Date().getFullYear()} Comunidad SIRMAX. Código bajo AGPL-3.0-or-later.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ["java", "kotlin", "sql", "bash"],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
