// SPDX-License-Identifier: AGPL-3.0-or-later
import Link from "next/link";

export default function NotFound() {
  return (
    <main className="container" style={{ padding: "6rem 1.25rem" }}>
      <p className="eyebrow">Error 404</p>
      <h1>Página no encontrada</h1>
      <p className="section-lead">La dirección que buscas no existe o se ha movido.</p>
      <p style={{ marginTop: "1.5rem" }}>
        <Link className="btn btn-primary" href="/">
          Volver al inicio
        </Link>
      </p>
    </main>
  );
}
