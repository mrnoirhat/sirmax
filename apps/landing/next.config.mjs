// SPDX-License-Identifier: AGPL-3.0-or-later
/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  poweredByHeader: false,
  // The landing is fully static; it never depends on the desktop app being online.
  output: "export",
  images: { unoptimized: true },
  trailingSlash: false,
};

export default nextConfig;
