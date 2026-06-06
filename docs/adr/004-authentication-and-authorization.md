# ADR 004: Authentication and Authorization

Status: Accepted

Date: 2026-06-05

## Context

The system has two different access modes:

- Back-office admins use account/password login.
- Mobile audience and volunteer users do not have accounts in V1 and enter through activity links or QR codes.

The confirmed back-office roles are:

- Super admin
- Activity admin
- Volunteer admin

The backend uses Spring Boot. The system needs secure password handling, frontend/backend separated authentication, role-based permissions, and operation logs that identify the acting admin.

## Decision

Use Spring Security + JWT + RBAC.

- Back-office admins log in with username/password.
- Passwords must be stored using a secure one-way hash.
- Successful back-office login returns a JWT.
- Back-office API requests must include the JWT.
- The backend authorizes protected operations through RBAC permissions mapped to the confirmed role matrix.
- Mobile audience and volunteer V1 flows remain accountless and do not use JWT.
- Public mobile endpoints must validate activity state, registration status, phone uniqueness, check-in eligibility, volunteer approval, and survey eligibility according to business rules.
- Operation logs must record the acting admin identity for authenticated back-office actions.

## Rationale

- Spring Security is the standard security foundation for Spring Boot applications.
- JWT fits the separated frontend/backend architecture.
- RBAC matches the confirmed three-role permission model.
- Keeping mobile users accountless preserves the V1 requirement for simple H5 access through links and QR codes.
- Separating back-office authentication from mobile public flows keeps the user experience simple while protecting administrative operations.

## Consequences

- Back-office APIs must be classified as protected by default unless explicitly public.
- Mobile public APIs require business-rule validation because they do not rely on login identity.
- Permission checks must be tested against the confirmed role matrix.
- Operation logs must include admin id, role, operation type, target object, timestamp, and request metadata where available.
- API design must distinguish protected admin endpoints from public mobile endpoints.

## Out of Scope

This ADR does not decide:

- JWT expiration time
- Refresh token strategy
- Exact password hashing algorithm
- Permission annotation style
- Public link token format
- Rate limiting or captcha
- Single sign-on

