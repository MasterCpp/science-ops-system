# Domain Documentation

This project uses a single-context domain documentation layout.

## Source of Truth

- Root domain context: `CONTEXT.md`
- Architecture decisions: `docs/adr/`
- Requirement clarifications: `docs/requirements/`
- Product requirements: `docs/prd/`

## Consumer Rules

Agents and skills should read `CONTEXT.md` before making domain, architecture, or implementation decisions.

When a decision changes the technical direction or system boundary, create or update an ADR under `docs/adr/`.

When a requirement changes, update the relevant file under `docs/requirements/` or `docs/prd/` before changing issues or code.

There is no `CONTEXT-MAP.md` because this is not currently a multi-context monorepo.

