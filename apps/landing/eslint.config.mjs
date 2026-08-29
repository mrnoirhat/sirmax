// SPDX-License-Identifier: AGPL-3.0-or-later
//
// ESLint 10 only reads flat config; `.eslintrc.json` is no longer loaded at all,
// and `next lint` was removed in Next 16 — so the lint script calls eslint
// directly and this file is the whole configuration.
//
// eslint-config-next 16 ships native flat config, so it is imported rather than
// wrapped in FlatCompat: the compatibility layer chokes on its shape.
import coreWebVitals from "eslint-config-next/core-web-vitals";
import typescript from "eslint-config-next/typescript";

const config = [
  {
    // Build output is generated, not authored; linting it reports thousands of
    // problems nobody can act on.
    ignores: [".next/**", "out/**", "node_modules/**"],
  },
  ...coreWebVitals,
  ...typescript,
];

export default config;
