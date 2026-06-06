# ADR 002: Repository Layout

Status: Accepted

Date: 2026-06-05

## Context

The project will use a separated frontend/backend architecture:

- Back-office frontend is an independent web application.
- Mobile H5 frontend is an independent web application.
- Backend is an independent Spring Boot API service.
- Frontends communicate with the backend through HTTP APIs.
- Pages are not rendered by backend server-side templates.

This architectural separation does not require separate Git repositories. For a learning project and a V1 internal business system, the repository layout should keep documentation, ADRs, issues, frontend apps, backend service, database design, and deployment files easy to discover from one project root.

## Decision

Use a single repository monorepo layout while keeping frontend and backend applications separated.

Target layout:

```text
science-ops-system/
  apps/
    admin-web/
    mobile-web/
  server/
  docs/
  .scratch/
```

- `apps/admin-web/` will contain the Vue 3 + Element Plus back-office application.
- `apps/mobile-web/` will contain the mobile H5 application.
- `server/` will contain the Spring Boot backend API service.
- `docs/` remains the source for PRD, ADRs, API design, database design, deployment docs, and other project documents.
- `.scratch/issues/` remains the local Markdown issue tracker.

## Rationale

- The architecture remains frontend/backend separated.
- A monorepo keeps project memory, documents, issues, API design, and implementation in one place for future agent conversations.
- Local development and Docker Compose can be documented and managed from one root.
- V1 does not need the coordination overhead of separate repositories.
- This layout still allows independent builds and deployments for admin web, mobile web, and backend service.

## Consequences

- Future implementation issues should specify which app or service they affect.
- Shared API contracts should be documented under `docs/api/` before frontend/backend implementation.
- Deployment docs should describe each independently deployable unit even though they live in one repository.
- If the project grows into separate teams or independent release cycles, a future ADR can split repositories.

## Out of Scope

This ADR does not decide:

- Exact frontend package manager
- Exact Spring Boot project structure
- API path conventions
- Docker Compose file structure
- CI/CD pipeline

