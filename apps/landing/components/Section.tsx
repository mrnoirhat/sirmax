// SPDX-License-Identifier: AGPL-3.0-or-later
import type { ReactNode } from "react";

type SectionProps = {
  id: string;
  eyebrow?: string;
  title: string;
  lead?: string;
  alt?: boolean;
  children?: ReactNode;
};

export function Section({ id, eyebrow, title, lead, alt, children }: SectionProps) {
  return (
    <section id={id} className={alt ? "alt" : undefined}>
      <div className="container">
        {eyebrow ? <p className="eyebrow">{eyebrow}</p> : null}
        <h2>{title}</h2>
        {lead ? <p className="section-lead">{lead}</p> : null}
        {children}
      </div>
    </section>
  );
}

type Feature = { title: string; body: string };

export function FeatureGrid({ items }: { items: Feature[] }) {
  return (
    <div className="grid">
      {items.map((f) => (
        <article className="card" key={f.title}>
          <h3>{f.title}</h3>
          <p>{f.body}</p>
        </article>
      ))}
    </div>
  );
}
