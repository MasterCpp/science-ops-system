# ADR 001: Backend Stack

Status: Accepted

Date: 2026-06-05

## Context

The Science Operations System V1 is a standard custom Web system with a back-office management UI, mobile H5 pages, MySQL database, local file storage, Excel exports, role-based permissions, operation logs, and Docker/Nginx deployment.

The project is also intended as a learning project for realistic enterprise-style development. The backend stack should support clear layered design, database transactions, authentication and authorization, file upload/download, Excel export, logging, testing, and maintainable deployment.

The main candidates were Spring Boot and NestJS.

## Decision

Use Spring Boot as the backend stack for V1.

The backend should use a conventional layered architecture:

- Controller layer for HTTP API entrypoints
- Application/service layer for business use cases and transactions
- Repository/mapper layer for database access
- Domain or model layer for core entities and business concepts
- Infrastructure components for files, exports, security, logging, and configuration

The exact persistence library, security library, migration tool, and project module layout will be decided in later ADRs before implementation.

## Rationale

- Spring Boot is a common enterprise backend stack in domestic business systems.
- It fits the V1 business requirements: RBAC permissions, MySQL persistence, transactions, Excel export, file upload/download, operation logs, and Docker deployment.
- It is suitable for practicing enterprise-style backend development, including layered architecture, service boundaries, validation, testing, and deployment documentation.
- It keeps the project aligned with common Java backend hiring and portfolio expectations.

NestJS remains a viable alternative for Node full-stack development, but it is not the best default for this project's enterprise backend learning goal.

## Consequences

- The backend implementation will be Java-based.
- API design, database design, test strategy, and deployment documentation should assume a Spring Boot backend unless a later ADR explicitly changes this decision.
- Future ADRs should lock:
  - Repository layout
  - Persistence approach
  - Authentication and authorization approach
  - Database migration approach
  - File storage layout
  - Deployment baseline

## Out of Scope

This ADR does not decide:

- Spring Boot version
- Java version
- Maven vs Gradle
- MyBatis vs JPA
- Spring Security/JWT details
- Monorepo structure
- Frontend framework final layout

