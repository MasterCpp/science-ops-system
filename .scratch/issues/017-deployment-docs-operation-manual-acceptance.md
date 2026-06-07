# Issue 017: Deployment Docs, Operation Manual, and Acceptance Checklist
Status: `completed`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `README.md`
- `STATUS.md`
- `docs/adr/006-deployment-baseline.md`
- `docs/testing/001-test-plan.md`

## What was built

Completed V1 delivery documents after all implementation slices were finished:

- Docker Compose deployment guide
- Operation manual
- Test account notes
- V1 acceptance checklist
- README and STATUS handoff updates

## Acceptance criteria

- [x] Deployment docs explain Docker Compose startup for MySQL, Spring Boot, and Nginx.
- [x] Deployment docs explain environment variables, ports, database connection, file-storage mount, and startup order.
- [x] Deployment docs explain how Flyway migrations run.
- [x] Deployment docs explain how to back up MySQL data and local file storage.
- [x] Operation manual covers admin login, activity management, registration management, check-in management, volunteer management, visitor reports, surveys, photos/files, exports, operation logs, and account management.
- [x] Mobile H5 operation notes cover registration, check-in, volunteer application, volunteer check-in/check-out, and survey submission.
- [x] Test account notes cover super admin, activity admin, volunteer admin, and disabled admin.
- [x] Acceptance checklist covers PRD, test plan, and major business loops.
- [x] README and STATUS are updated to V1 handoff state.
- [x] Docs do not claim unsupported V1 features such as mini program, SMS, object storage, automatic backups, or native `.xlsx` export.

## Implemented files

- `docs/deployment/001-docker-compose-deployment.md`
- `docs/operations/001-operation-manual.md`
- `docs/acceptance/001-v1-acceptance-checklist.md`
- `README.md`
- `STATUS.md`

## Verification

- `mvn -f server/pom.xml test`
  - Result: `53 tests, 0 failures, 0 errors`
- `npm.cmd run build:admin`
  - Result: build succeeded
  - Notes: Vite reported a large-chunk warning and third-party comment warnings, but produced the production build.
- `npm.cmd run build:mobile`
  - Result: build succeeded

## Blocked by

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
