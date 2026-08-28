// SPDX-License-Identifier: AGPL-3.0-or-later
import type { SidebarsConfig } from "@docusaurus/plugin-content-docs";

/**
 * Documentation structure (master prompt §6 / §71). Two audiences: operators (plain, practical) and
 * developers (architecture, domain, database, tests, build, CI/CD, contribution).
 *
 * Most pages are stubs in Phase 1; content is written alongside the features in Phases 6 and 12.
 */
const sidebars: SidebarsConfig = {
  docs: [
    "intro",
    "instalacion",
    "primeros-pasos",
    {
      type: "category",
      label: "Guía de usuario",
      link: { type: "doc", id: "guia-usuario/index" },
      items: [
        "guia-usuario/ciudadanos",
        "guia-usuario/servicios",
        "guia-usuario/tramites",
        "guia-usuario/registro-de-documentos",
        "guia-usuario/facturacion",
        "guia-usuario/pagos",
        "guia-usuario/caja",
        "guia-usuario/impresion",
        "guia-usuario/documentos-oficiales",
        "guia-usuario/reportes",
        "guia-usuario/usuarios-y-permisos",
        "guia-usuario/configuracion",
        "guia-usuario/backup",
        "guia-usuario/restauracion",
        "guia-usuario/seguridad",
        "guia-usuario/solucion-de-problemas",
      ],
    },
    {
      type: "category",
      label: "Administración",
      link: { type: "doc", id: "administracion/index" },
      items: ["administracion/institucion", "administracion/reglas-de-negocio"],
    },
    {
      type: "category",
      label: "Desarrollo",
      link: { type: "doc", id: "desarrollo/index" },
      items: [
        "desarrollo/arquitectura",
        "desarrollo/modelo-de-dominio",
        "desarrollo/base-de-datos",
        "desarrollo/motor-de-flujo",
        "desarrollo/motor-de-tasas",
        "desarrollo/pruebas",
        "desarrollo/integraciones",
        "desarrollo/api-futura",
        "desarrollo/releases",
        "desarrollo/contribucion",
      ],
    },
    "roadmap",
    "licencia",
  ],
};

export default sidebars;
