# AGENTS.md - navikt/ung-deltakelse-opplyser

## Repository Overview

Ung Deltakelse Opplyser er en Kotlin-basert JVM-tjeneste med multi-module Maven-oppsett (`app` og `kontrakt`) for behandling av domene- og kontraktslogikk.

## Tech Stack

- Kotlin
- Maven (wrapper)
- Shell
- Dockerfile

## Build & Test Commands

```bash
./mvnw compile          # Build all modules
./mvnw test             # Run all tests
./mvnw -pl app test     # Run tests for app module
```

## Code Standards

- Follow Kotlin coding conventions and existing project patterns
- Keep code clear and explicit over clever abstractions
- Add or update tests for behavior changes

## Boundaries

### Always

- Follow established package and module structure
- Run relevant tests before creating PRs
- Keep configuration and environment handling explicit and safe

### Ask First

- Changes to authentication/authorization flows
- Changes to production deploy config under `nais/`
- Introducing new runtime dependencies

### Never

- Commit secrets, tokens, or credentials
- Disable security controls to make tests pass
- Modify unrelated files as part of a focused change
