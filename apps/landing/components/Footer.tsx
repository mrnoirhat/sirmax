// SPDX-License-Identifier: AGPL-3.0-or-later
import { site } from "@/lib/site";

export function Footer() {
  return (
    <footer className="site-footer" id="comunidad">
      <div className="container">
        <div className="cols">
          <div>
            <h4>{site.name}</h4>
            <p>{site.fullName}</p>
            <p>{site.tagline}</p>
          </div>
          <div>
            <h4>Proyecto</h4>
            <ul>
              <li>
                <a href={site.repo} target="_blank" rel="noreferrer">
                  GitHub
                </a>
              </li>
              <li>
                <a href={site.issues} target="_blank" rel="noreferrer">
                  Issues
                </a>
              </li>
              <li>
                <a href={site.discussions} target="_blank" rel="noreferrer">
                  Discussions
                </a>
              </li>
              <li>
                <a href={site.roadmap} target="_blank" rel="noreferrer">
                  Roadmap
                </a>
              </li>
            </ul>
          </div>
          <div>
            <h4>Recursos</h4>
            <ul>
              <li>
                <a href={site.docs}>Documentación</a>
              </li>
              <li>
                <a href={site.releases} target="_blank" rel="noreferrer">
                  Descargas
                </a>
              </li>
              <li>
                <a href={site.contributing} target="_blank" rel="noreferrer">
                  Cómo contribuir
                </a>
              </li>
            </ul>
          </div>
          <div>
            <h4>Licencia</h4>
            <ul>
              <li>Código: AGPL-3.0-or-later</li>
              <li>
                <a
                  href="https://www.gnu.org/licenses/agpl-3.0.html"
                  target="_blank"
                  rel="noreferrer"
                >
                  Texto de la licencia
                </a>
              </li>
            </ul>
          </div>
        </div>

        <p className="legal">
          SIRMAX ofrece flujos administrativos configurables. Los requisitos legales, fiscales,
          tributarios, archivísticos y regulatorios de cada municipio deben ser revisados y
          configurados por personal cualificado antes de su uso en producción. El nombre y el logo
          SIRMAX están reservados (ver TRADEMARK_POLICY.md).
        </p>
        <p>© {new Date().getFullYear()} Comunidad SIRMAX.</p>
      </div>
    </footer>
  );
}
