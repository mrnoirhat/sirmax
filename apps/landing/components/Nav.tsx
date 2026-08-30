// SPDX-License-Identifier: AGPL-3.0-or-later
import Image from "next/image";
import { primaryNav, site } from "@/lib/site";

export function Nav() {
  return (
    <header className="site-header">
      <nav className="container nav" aria-label="Principal">
        <a className="brand" href="#producto">
          {/* The mark is decorative here: the wordmark beside it already names
              the site, and a screen reader announcing both just stutters. */}
          <Image src="/logo.svg" alt="" width={28} height={28} aria-hidden priority />
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
