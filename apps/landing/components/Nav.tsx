// SPDX-License-Identifier: AGPL-3.0-or-later
import { primaryNav, site } from "@/lib/site";

export function Nav() {
  return (
    <header className="site-header">
      <nav className="container nav" aria-label="Principal">
        <a className="brand" href="#producto">
          {site.name}
        </a>
        <ul>
          {primaryNav.map((item) => (
            <li key={item.label}>
              <a href={item.href}>{item.label}</a>
            </li>
          ))}
        </ul>
        <a
          className="nav-cta"
          href={site.repo}
          target="_blank"
          rel="noreferrer"
        >
          VER PROYECTO EN GITHUB
        </a>
      </nav>
    </header>
  );
}
