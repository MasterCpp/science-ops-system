# ADR 003: Persistence and Database Migrations

Status: Accepted

Date: 2026-06-05

## Context

The backend stack is Spring Boot. The system needs to persist activities, registrations, check-ins, volunteer positions, volunteer applications, visitor reports, surveys, files, photos, operation logs, and back-office accounts.

The database is MySQL 8. The project should practice enterprise-style database development, so schema changes should be versioned and repeatable instead of being applied manually.

The candidate persistence approaches were MyBatis-Plus, JPA/Hibernate, and plain MyBatis.

## Decision

Use MyBatis-Plus with MySQL 8 and Flyway.

- MyBatis-Plus is the default persistence/data mapper approach for the Spring Boot backend.
- MySQL 8 is the V1 relational database.
- Flyway manages database schema migrations.
- Database design documents under `docs/database/` must stay aligned with Flyway migration scripts once implementation begins.

## Rationale

- MyBatis and MyBatis-Plus are common in domestic enterprise back-office systems.
- MyBatis-Plus reduces repetitive CRUD code while keeping SQL and table design easy to understand.
- The project has many business tables and reporting/export queries, where explicit database thinking is useful.
- Flyway gives the project a repeatable migration history and avoids relying on manual SQL execution.
- This combination is practical for learning enterprise backend development with Spring Boot.

## Consequences

- Backend data access should be implemented through MyBatis-Plus mappers/services, with custom SQL where business queries require it.
- Schema changes must be represented as Flyway migration files after implementation starts.
- Local development and deployment documentation must describe how migrations run.
- Database design should use MySQL-compatible types and constraints.
- Tests should verify important business behavior at service/API level rather than only mapper CRUD behavior.

## Out of Scope

This ADR does not decide:

- Exact table schema
- Naming convention for tables and columns
- Flyway migration filename convention
- Logical delete strategy
- ID generation strategy
- Transaction boundary details

