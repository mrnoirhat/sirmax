# scripts/

Cross-cutting helper scripts for build, release and maintenance tasks that don't belong to a single
app. Keep them small, documented at the top, and POSIX `sh` or PowerShell (`.ps1`) — see
[`.editorconfig`](../.editorconfig) for line-ending rules.

Nothing here yet. Planned entries as phases land:

| Script | Purpose | Phase |
| --- | --- | --- |
| `bootstrap-wrapper.sh` | Generate the Gradle wrapper jar | 1 |
| `new-adr.sh` | Scaffold a numbered ADR from the template | 1 |
| `check-branch.sh` | Verify the current branch follows the naming/promotion rules | 1 |
| `release.ps1` | Assemble Windows artifacts and checksums from `main` | 11 |
