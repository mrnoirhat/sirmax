# backend/

Reserved boundary for a **future SIRMAX API / cloud service**. There is **no running service here for
1.0** (see [ADR 0005](../docs/adr/0005-modular-domain-architecture.md) and
[`ARCHITECTURE.md` §11](../ARCHITECTURE.md)).

## Intent

The desktop client's `sirmax-domain` and `sirmax-application` modules deliberately have no dependency
on JavaFX, JDBC or Google APIs. A future HTTP API would reuse those two layers unchanged and provide
its own infrastructure adapters (server persistence, auth, transport) in this directory.

## Rules until then

- Do not move business rules out of `apps/desktop/sirmax-domain` / `sirmax-application` "in advance".
- Do not add a server dependency to the desktop build.
- When the API work starts, it gets its own ADR and its own CI workflow.
