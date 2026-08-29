// SPDX-License-Identifier: AGPL-3.0-or-later
import Image from "next/image";
import { Nav } from "@/components/Nav";
import { Footer } from "@/components/Footer";
import { Section, FeatureGrid } from "@/components/Section";
import { site } from "@/lib/site";

export default function HomePage() {
  return (
    <>
      <Nav />
      <main id="contenido">
        {/* ── Hero ── */}
        <section id="producto" className="hero">
          <div className="container">
            <p className="eyebrow">{site.fullName}</p>
            <h1>{site.tagline}</h1>
            <p className="lead">
              Plataforma <strong>open source</strong> y <strong>local-first</strong> para
              ayuntamientos: un solo lugar para servicios, trámites, registros ciudadanos,
              facturación, pagos, caja, impresión de facturas, documentos oficiales, auditoría y
              copias de seguridad.
            </p>
            <div className="hero-actions">
              <a className="btn btn-primary" href="#descargar">
                Descargar SIRMAX
              </a>
              <a
                className="btn btn-github"
                href={site.repo}
                target="_blank"
                rel="noreferrer"
              >
                VER PROYECTO EN GITHUB
              </a>
              <a className="btn btn-ghost" href={site.docs}>
                Leer la documentación
              </a>
            </div>
          </div>
        </section>

        {/* ── Screenshots ── */}
        <Section
          id="capturas"
          eyebrow="Cómo se ve"
          title="Navegación por tareas, no por menús"
          lead="La pantalla de inicio pregunta qué necesitas hacer. Ningún operador debería tener
            que traducir «cobrar una certificación» a un recorrido por submenús."
        >
          <figure className="shot">
            <Image
              src="/screenshots/sirmax-shell.png"
              alt="Pantalla de inicio de SIRMAX con navegación por tareas y las acciones más
                frecuentes de un ayuntamiento"
              width={1200}
              height={780}
              priority
            />
            <figcaption>
              Inicio: las tareas que un mostrador municipal hace todos los días, sin submenús.
            </figcaption>
          </figure>
          <figure className="shot">
            <Image
              src="/screenshots/sirmax-primer-arranque.png"
              alt="Pantalla de configuración inicial de SIRMAX, donde se crea el municipio y la
                cuenta administradora"
              width={1200}
              height={780}
            />
            <figcaption>
              Primer arranque: se crea el municipio y la cuenta administradora. Nada más que
              instalar.
            </figcaption>
          </figure>
        </Section>

        {/* ── Problem ── */}
        <Section
          id="problema"
          eyebrow="El problema"
          title="Procesos fragmentados, papel y sistemas desconectados"
          lead="Los ayuntamientos trabajan con formularios manuales, expedientes físicos y varias
            herramientas que no se hablan entre sí. El resultado: filas, errores, poca trazabilidad
            y dependencia de personal técnico para tareas simples."
          alt
        />

        {/* ── Solution ── */}
        <Section
          id="solucion"
          eyebrow="La solución"
          title="Una columna vertebral municipal compartida"
          lead="SIRMAX no es una colección de formularios: es una plataforma configurable con un
            núcleo común de trámite, documento, finanzas, flujo de trabajo, auditoría y reportes al
            que se conectan todos los módulos."
        >
          <p className="pipeline">
            Ciudadano → Solicitud → Trámite → Requisitos → Documentos → Revisión/Inspección →
            Decisión → Tasa → Factura → Pago → Recibo/Documento oficial → Entrega → Auditoría →
            Archivo
          </p>
          <p className="section-lead" style={{ marginTop: "1rem" }}>
            No todo trámite se paga: un proceso puede ser gratuito, con tasa, con tasa condicional o
            de pago externo.
          </p>
        </Section>

        {/* ── Core features ── */}
        <Section
          id="caracteristicas"
          eyebrow="Características"
          title="Lo que incluye"
          alt
        >
          <FeatureGrid
            items={[
              {
                title: "Ciudadanos y organizaciones",
                body: "Ficha maestra central con detección de duplicados; la persona no se reescribe en cada trámite.",
              },
              {
                title: "Catálogo configurable de servicios",
                body: "Un administrador define un servicio nuevo —requisitos, formulario, flujo, tasas, plantillas— sin desarrollador cuando es razonable.",
              },
              {
                title: "Trámites y expedientes",
                body: "Estructura compartida con requisitos, tareas, revisiones, inspecciones, decisiones, SLA e historial.",
              },
              {
                title: "Motor de requisitos",
                body: "Checklist visible: el operador nunca adivina por qué un trámite no puede avanzar.",
              },
              {
                title: "Motor de tasas versionable",
                body: "Importe fijo, por cantidad, área, duración, categoría o tramos; con vigencia y sin reescribir el histórico.",
              },
              {
                title: "Usuarios, roles y permisos",
                body: "RBAC, sesión con expiración y bloqueo, y auditoría inmutable de lo relevante.",
              },
            ]}
          />
        </Section>

        {/* ── Municipal services ── */}
        <Section
          id="servicios"
          eyebrow="Servicios municipales"
          title="Módulos especializados sobre el mismo núcleo"
          lead="Cada módulo aporta sus entidades y reutiliza trámite, documentos, finanzas y
            auditoría del núcleo — sin crear una arquitectura paralela."
        >
          <FeatureGrid
            items={[
              { title: "Registro de Documentos / Conservaduría", body: "Libro, folio, número de registro, copias certificadas y cadena de custodia." },
              { title: "Certificaciones y cartas", body: "Plantillas, variables, numeración, firma y política de reimpresión/anulación." },
              { title: "Planeamiento Urbano / Construcción", body: "Proyecto, planos, etapas de revisión, inspección, decisión y permiso." },
              { title: "Propiedad / Catastro", body: "Parcela, titularidad, referencias legales, contratos y arrendamientos." },
              { title: "Cementerios", body: "Cementerio → sección → manzana → nicho, con concesiones, inhumaciones y disponibilidad." },
              { title: "Mercados y espacios comerciales", body: "Casillas, comerciantes, acuerdos de ocupación, morosidad e inspección." },
              { title: "Espacio público y movilidad", body: "Permisos de uso de vía, cierres parciales, vehículos y ventanas horarias." },
              { title: "Solicitudes y quejas", body: "Casos no financieros: canal, categoría, asignación, seguimiento, SLA y cierre." },
            ]}
          />
        </Section>

        {/* ── Procedures and records ── */}
        <Section
          id="tramites"
          eyebrow="Trámites y registros"
          title="Del mostrador al documento oficial"
          lead="Buscar persona → elegir servicio → completar solicitud → validar requisitos →
            cobrar si aplica → generar recibo/documento → finalizar. Con el mínimo de clics."
          alt
        />

        {/* ── Billing and payments ── */}
        <Section
          id="facturacion"
          eyebrow="Facturación y pagos"
          title="Facturación de primera clase"
          lead="Servicio → cargo → factura → pago → recibo, sin salir de la aplicación. Factura con
            ciclo DRAFT → ISSUED → PARTIALLY_PAID → PAID → VOIDED → REFUNDED, numeración segura ante
            concurrencia, pagos parciales, reembolsos, anulaciones, sesión de caja y conciliación."
        >
          <p className="section-lead" style={{ marginTop: "1rem" }}>
            Dinero con representación decimal exacta — nunca coma flotante. Snapshot financiero
            histórico: un cambio de logo o dirección no reescribe facturas antiguas.
          </p>
        </Section>

        {/* ── Invoice printing ── */}
        <Section
          id="impresion"
          eyebrow="Impresión de facturas"
          title="Dos modelos de impresión, de verdad"
          lead="Botón Imprimir integrado con Windows, sin exportar imágenes a mano."
          alt
        >
          <div className="grid">
            <article className="card">
              <h3>Modelo A — angosta / mostrador</h3>
              <p>
                Perfiles de 58 mm, 80 mm y ancho configurable. Optimizada para impresoras de impacto
                monocromas; no es una Letter encogida.
              </p>
            </article>
            <article className="card">
              <h3>Modelo B — oficina US Letter</h3>
              <p>
                Factura profesional en 8.5 × 11&quot; con cabecera institucional, bloque de identidad,
                datos del cliente, tabla de detalle, totales, bloque de pago y pie. Arquitectura
                lista para A4.
              </p>
            </article>
          </div>
          <p className="section-lead" style={{ marginTop: "1.25rem" }}>
            Marca institucional (logo, colores, RNC configurable, QR/verificación), PDF real y
            reimpresión autorizada que no duplica factura ni pago y queda auditada.
          </p>
        </Section>

        {/* ── Offline / local-first ── */}
        <Section
          id="offline"
          eyebrow="Local-first"
          title="Sin internet no es un error"
          lead="El trabajo local funciona siempre. Las funciones que dependen de la red —copia en
            Google Drive, actualizaciones, futura sincronización— fallan de forma elegante y nunca
            corrompen los datos locales."
        />

        {/* ── Security / audit ── */}
        <Section
          id="seguridad"
          eyebrow="Seguridad y auditoría"
          title="Local no significa inseguro"
          lead="Autenticación, hashing con sal, expiración/bloqueo de sesión, RBAC, validación de
            ficheros y almacenamiento seguro de secretos. La auditoría registra quién, cuándo, qué,
            objeto, valores antes/después, motivo y sesión — y es inmutable desde la UI."
          alt
        />

        {/* ── Backups ── */}
        <Section
          id="backups"
          eyebrow="Copias de seguridad"
          title="Backup local y, opcionalmente, en tu Google Drive"
          lead="Snapshot consistente → validación → compresión → cifrado → hash de integridad →
            copia local → copia opcional en Drive (tu cuenta es la dueña). Restauración segura con
            backup de emergencia previo, confirmación y registro en auditoría."
        />

        {/* ── Architecture ── */}
        <Section
          id="arquitectura"
          eyebrow="Arquitectura"
          title="Escritorio Windows, Java + JavaFX, SQLite"
          lead="Capas UI → Aplicación → Dominio → Infraestructura. El dominio es Java puro; la UI no
            ejecuta SQL. Preparada para una futura API/nube sin reescribir las reglas de negocio."
          alt
        >
          <FeatureGrid
            items={[
              { title: "Escritorio", body: "Java 25 LTS · JavaFX · SQLite · Gradle · jpackage (sin instalar Java aparte)." },
              { title: "Web", body: "Landing en Next.js optimizada para Vercel; documentación en Docusaurus." },
              { title: "Monorepo", body: "Un repositorio: escritorio, landing, documentación, base de datos y CI/CD." },
            ]}
          />
        </Section>

        {/* ── Screenshots ── */}
        <Section
          id="capturas"
          eyebrow="Capturas"
          title="Cómo se ve"
          lead="El shell y el design system están en construcción (Fase 2). Aquí irán capturas
            reales del recorrido de mostrador, la facturación y la impresión."
        >
          <div className="placeholder-shots" aria-hidden="true">
            <div className="placeholder-shot">Inicio · ¿Qué necesitas hacer?</div>
            <div className="placeholder-shot">Trámite · checklist de requisitos</div>
            <div className="placeholder-shot">Cobro · factura y recibo</div>
          </div>
        </Section>

        {/* ── Roadmap ── */}
        <Section
          id="roadmap"
          eyebrow="Roadmap"
          title="Construcción por fases, en abierto"
          lead="El proyecto avanza en 15 fases (0–14), del repositorio y la arquitectura hasta la
            release 1.0. El estado vive en ROADMAP.md."
          alt
        >
          <p className="hero-actions">
            <a className="btn btn-ghost" href={site.roadmap} target="_blank" rel="noreferrer">
              Ver el roadmap completo
            </a>
          </p>
        </Section>

        {/* ── Open source / community ── */}
        <Section
          id="open-source"
          eyebrow="Open source"
          title="Hecho para colaborar"
          lead="Código bajo AGPL-3.0-or-later. Cualquier persona puede leer, modificar, adaptar y
            contribuir. La AGPL no obliga a enviar tus cambios al repositorio original."
        >
          <div className="hero-actions">
            <a className="btn btn-primary" href={site.repo} target="_blank" rel="noreferrer">
              VER PROYECTO EN GITHUB
            </a>
            <a className="btn btn-ghost" href={site.contributing} target="_blank" rel="noreferrer">
              Guía de contribución
            </a>
            <a className="btn btn-ghost" href={site.discussions} target="_blank" rel="noreferrer">
              Discussions
            </a>
          </div>
        </Section>

        {/* ── Documentation ── */}
        <Section
          id="documentacion"
          eyebrow="Documentación"
          title="Para operadoras y para quien desarrolla"
          lead="Guía de usuario en lenguaje claro con ejemplos, y documentación técnica de
            arquitectura, dominio, base de datos, pruebas y despliegue."
          alt
        >
          <p className="hero-actions">
            <a className="btn btn-primary" href={site.docs}>
              Abrir la documentación
            </a>
          </p>
        </Section>

        {/* ── Download ── */}
        <Section
          id="descargar"
          eyebrow="Descargar"
          title="SIRMAX para Windows"
          lead="Los instaladores se publican en las releases de GitHub a partir de la Fase 11. El
            instalador incluye el runtime: no necesitas instalar Java."
        >
          <p className="hero-actions">
            <a className="btn btn-primary" href={site.releases} target="_blank" rel="noreferrer">
              Ver releases en GitHub
            </a>
          </p>
        </Section>

        {/* ── FAQ ── */}
        <Section id="faq" eyebrow="FAQ" title="Preguntas frecuentes" alt>
          <details className="faq">
            <summary>¿SIRMAX funciona sin internet?</summary>
            <p>
              Sí. Es local-first: todo el trabajo de mostrador funciona sin conexión. Solo el backup
              en Google Drive y futuras integraciones necesitan red.
            </p>
          </details>
          <details className="faq">
            <summary>¿Necesito instalar Java o una base de datos?</summary>
            <p>
              No. El instalador de Windows incluye el runtime y usa SQLite embebido: un solo fichero
              por instalación.
            </p>
          </details>
          <details className="faq">
            <summary>¿Puedo adaptar los servicios y las tarifas de mi municipio?</summary>
            <p>
              Sí. Los servicios, requisitos, flujos y tasas son configurables y versionables; los
              trámites antiguos se interpretan con la versión de regla vigente en su momento.
            </p>
          </details>
          <details className="faq">
            <summary>¿SIRMAX garantiza cumplimiento legal o tributario?</summary>
            <p>
              No automáticamente. Ofrece flujos administrativos configurables; los requisitos legales
              y fiscales de cada municipio deben revisarse y configurarse por personal cualificado.
            </p>
          </details>
          <details className="faq">
            <summary>¿Con qué licencia se publica?</summary>
            <p>
              El código bajo GNU AGPL-3.0-or-later. El nombre y el logo SIRMAX están reservados
              (política de marca).
            </p>
          </details>
        </Section>
      </main>
      <Footer />
    </>
  );
}
