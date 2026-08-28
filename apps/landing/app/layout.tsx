// SPDX-License-Identifier: AGPL-3.0-or-later
import type { Metadata, Viewport } from "next";
import { site } from "@/lib/site";
import "./globals.css";

export const metadata: Metadata = {
  metadataBase: new URL(site.url),
  title: {
    default: `${site.name} — ${site.tagline}`,
    template: `%s · ${site.name}`,
  },
  description: site.description,
  applicationName: site.name,
  keywords: [
    "municipio",
    "ayuntamiento",
    "gestión municipal",
    "trámites",
    "facturación municipal",
    "open source",
    "República Dominicana",
    "local-first",
  ],
  authors: [{ name: "Comunidad SIRMAX", url: site.repo }],
  alternates: { canonical: "/" },
  openGraph: {
    type: "website",
    locale: "es_DO",
    url: site.url,
    siteName: site.name,
    title: `${site.name} — ${site.tagline}`,
    description: site.description,
  },
  twitter: {
    card: "summary_large_image",
    title: `${site.name} — ${site.tagline}`,
    description: site.description,
  },
  robots: { index: true, follow: true },
  category: "government",
};

export const viewport: Viewport = {
  themeColor: "#1f5fa6",
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  const jsonLd = {
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    name: site.name,
    alternateName: site.fullName,
    applicationCategory: "BusinessApplication",
    operatingSystem: "Windows",
    offers: { "@type": "Offer", price: "0", priceCurrency: "USD" },
    license: "https://www.gnu.org/licenses/agpl-3.0.html",
    url: site.url,
    sameAs: [site.repo],
    description: site.description,
  };

  return (
    <html lang="es">
      <body>
        <a className="skip-link" href="#contenido">
          Saltar al contenido
        </a>
        {children}
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
        />
      </body>
    </html>
  );
}
