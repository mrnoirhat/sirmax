// SPDX-License-Identifier: AGPL-3.0-or-later
import { themes as prismThemes } from "prism-react-renderer";
import type { Config } from "@docusaurus/types";
import type * as Preset from "@docusaurus/preset-classic";

const REPO = "https://github.com/mrnoirhat/sirmax";

const config: Config = {
  title: "SIRMAX",
  tagline: "La gestión municipal, simplificada.",
  // favicon: "img/favicon.ico", // added with the real asset in Phase 12

  url: "https://docs.sirmax.org",
  baseUrl: "/",

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
        { href: "https://sirmax.org", label: "Sitio web", position: "right" },
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
