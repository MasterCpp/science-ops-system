# ADR 006: Deployment Baseline

Status: Accepted

Date: 2026-06-05

## Context

The system uses a separated frontend/backend architecture with:

- Back-office web application
- Mobile H5 web application
- Spring Boot backend API
- MySQL database
- Local file storage

The project should practice an enterprise-style deployment flow while staying practical for local development and V1 delivery.

## Decision

Use Docker Compose + MySQL + Spring Boot + Nginx as the V1 deployment baseline.

Target deployment units:

- MySQL container
- Spring Boot backend container
- Nginx container

Nginx responsibilities:

- Serve built `admin-web` static files.
- Serve built `mobile-web` static files.
- Reverse proxy `/api` requests to the Spring Boot backend.

Backend responsibilities:

- Run the Spring Boot API service.
- Connect to MySQL.
- Run Flyway migrations on startup.
- Read and write files through a mounted local storage directory.

The local storage directory must be mounted into the backend container.

## Rationale

- Docker Compose is simple enough for V1 while still reflecting realistic deployment concerns.
- Nginx is a common choice for serving frontend static assets and reverse proxying API traffic.
- Running MySQL in Compose makes local development and test deployment reproducible.
- Mounting the file storage directory keeps uploaded files outside the application image.
- Running Flyway with backend startup keeps database schema migration repeatable.

## Consequences

- Deployment documentation must cover environment variables, storage mount path, MySQL credentials, exposed ports, and startup order.
- Frontend builds must output static files that Nginx can serve.
- Backend configuration must support containerized database and storage paths.
- Database migrations must be safe to run at backend startup.
- Backups must include MySQL data and the mounted storage directory.

## Out of Scope

This ADR does not decide:

- Exact Dockerfile contents
- Exact Compose service names
- HTTPS certificate management
- CI/CD pipeline
- Production server operating system
- Domain name configuration
- Blue-green or rolling deployment

