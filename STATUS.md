# Project Status

Last updated: 2026-06-06

## Current Phase

Implementation started.

The project has completed requirement clarification, PRD, core ADRs, ER/database design, page prototypes, first-pass API design, V1 test plan, implementation issue breakdown, the Issue 001 runnable project scaffold baseline, the Issue 002 database migration/base entity baseline, the Issue 003 admin auth/JWT/RBAC baseline, the Issue 004 admin activity lifecycle backend baseline, the Issue 005 activity process/custom-field/public-detail baseline, and the Issue 006 audience registration baseline.

## Completed

- Project memory structure
- Agent rules
- Requirement clarification
- PRD
- Core architecture decisions
- ER/database design
- Prototype page map
- Admin low-fidelity wireframes
- Mobile H5 low-fidelity wireframes
- First-pass API design
- V1 test plan
- Implementation issue breakdown
- Human-facing `README.md`
- Project progress `STATUS.md`
- Issue 001 project scaffold:
  - `apps/admin-web/`
  - `apps/mobile-web/`
  - `server/`
  - root `package.json`
  - `docker-compose.yml`
  - `deploy/nginx/default.conf`
- Issue 002 database baseline:
  - MyBatis-Plus dependency and logical delete configuration
  - Flyway migration baseline
  - MySQL driver and H2 local/test support
  - `server/src/main/resources/db/migration/V1__init_schema.sql`
  - base entity and audit entity classes
  - baseline status/type enums
- Issue 003 admin auth/RBAC baseline:
  - Spring Security stateless JWT configuration
  - BCrypt password hashing
  - seeded local admin users, roles, and permissions
  - `POST /api/admin/auth/login`
  - `GET /api/admin/auth/me`
  - protected RBAC probe endpoints for account, operation-log, registration, survey, visitor-report, and file permissions
- Issue 004 admin activity lifecycle:
  - activity create, list, detail, update, and logical delete APIs
  - keyword/status/start-time filtering with pagination
  - lifecycle transitions from draft through archive
  - archived read-only enforcement
  - super-admin-only unarchive and delete controls
  - in-progress capacity and registration deadline edit lock
- Issue 005 activity process, custom fields, and public detail:
  - admin process item create, list, update, sort, and delete APIs
  - admin registration custom field create, list, update, sort, delete, and field-key uniqueness APIs
  - public mobile activity detail API with remaining capacity and registration availability state
  - mobile H5 activity detail page rendering public info, process items, and registration fields
- Issue 006 audience registration:
  - public mobile audience registration submission API
  - mobile H5 registration submit and success state
  - duplicate phone, deadline, and capacity business-rule errors
  - admin registration list and backfill APIs
  - admin registration cancel API with capacity release
  - registration export as Excel-openable UTF-8 CSV

## Key Decisions

- V1 is a Web system with back-office management and mobile H5 pages.
- Architecture is frontend/backend separated.
- Repository layout is a monorepo:
  - `apps/admin-web/`
  - `apps/mobile-web/`
  - `server/`
  - `docs/`
  - `.scratch/`
- Backend stack is Spring Boot.
- Persistence is MyBatis-Plus + MySQL 8.
- Database migrations use Flyway.
- Auth is Spring Security + JWT + RBAC.
- Mobile audience and volunteer flows are accountless in V1.
- File bytes use local server storage; metadata is stored in MySQL.
- Deployment baseline is Docker Compose + MySQL + Spring Boot + Nginx.

## Current Documents

- `AGENTS.md`
- `CONTEXT.md`
- `docs/requirements/需求澄清.md`
- `docs/prd/001-科普运营系统-v1.md`
- `docs/adr/001-backend-stack.md`
- `docs/adr/002-repository-layout.md`
- `docs/adr/003-persistence-and-migrations.md`
- `docs/adr/004-authentication-and-authorization.md`
- `docs/adr/005-file-storage.md`
- `docs/adr/006-deployment-baseline.md`
- `docs/database/001-er-design.md`
- `docs/prototype/001-page-map.md`
- `docs/prototype/002-admin-wireframes.md`
- `docs/prototype/003-mobile-wireframes.md`
- `docs/api/001-api-design.md`
- `docs/testing/001-test-plan.md`
- `.scratch/issues/000-prd-science-ops-system-v1.md`
- `.scratch/issues/001-project-scaffold-and-dev-baseline.md`
- `.scratch/issues/002-database-migrations-and-base-entities.md`
- `.scratch/issues/003-admin-auth-jwt-rbac-baseline.md`
- `.scratch/issues/004-admin-activity-lifecycle.md`
- `.scratch/issues/005-activity-process-custom-fields-public-detail.md`
- `.scratch/issues/006-audience-registration-flow.md`
- `.scratch/issues/007-audience-check-in-flow.md`
- `.scratch/issues/008-volunteer-position-application-review-flow.md`
- `.scratch/issues/009-volunteer-attendance-service-hours-flow.md`
- `.scratch/issues/010-visitor-report-management-flow.md`
- `.scratch/issues/011-activity-files-photos-archive-flow.md`
- `.scratch/issues/012-survey-configuration-flow.md`
- `.scratch/issues/013-survey-response-statistics-export-flow.md`
- `.scratch/issues/014-dashboard-and-activity-summary.md`
- `.scratch/issues/015-operation-log-audit-query.md`
- `.scratch/issues/016-admin-account-role-management.md`
- `.scratch/issues/017-deployment-docs-operation-manual-acceptance.md`

## Next Step

Continue implementation from:

```text
.scratch/issues/007-audience-check-in-flow.md
```

Follow dependency order in `.scratch/issues/`.

Before implementing an issue, read:

- `docs/prd/001-科普运营系统-v1.md`
- `docs/database/001-er-design.md`
- `docs/prototype/*.md`
- `docs/api/001-api-design.md`
- `docs/testing/001-test-plan.md`
- The issue file being implemented

## Not Started

- Deployment documentation
- Operation manual
- Acceptance checklist
- Remaining frontend business screens

## Known Notes

- `docs/api/001-api-design.md` is still marked `Draft`.
- `docs/prototype/*.md` files are low-fidelity planning docs, not final visual designs.
- Issue 001 intentionally contains placeholder pages and `/api/health` only; business functionality starts in later issues.
- Issue 002 verified Flyway migrations with H2 MySQL compatibility mode. Docker CLI is not available on this machine, so live MySQL container verification has not been run.
- Issue 003 seeds development admin accounts only when `admin_user` is empty. Default local password is `password123`.
- Issue 006 implements audience registration and a CSV export that Excel can open. Native `.xlsx` export can be added later if delivery requires it.
