# Project Status

Last updated: 2026-06-07

## Current Phase

V1 implementation and delivery documentation are complete.

The project has completed requirement clarification, PRD, core ADRs, ER/database design, page prototypes, first-pass API design, V1 test plan, implementation issue breakdown, implementation issues 001 through 016, and the Issue 017 deployment documentation, operation manual, and acceptance checklist handoff package.

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
- Issue 007 audience check-in:
  - fixed public check-in link in the mobile activity detail API
  - public mobile audience check-in API and H5 check-in mode
  - duplicate check-in, unregistered phone, cancelled registration, and invalid activity state errors
  - admin check-in list and manual check-in APIs
  - admin check-in revoke API with active-count release
  - check-in export as Excel-openable UTF-8 CSV
- Issue 008 volunteer position and application review:
  - admin volunteer position create, list, update, and logical delete APIs
  - public mobile volunteer position list API
  - public mobile volunteer application submit API and H5 volunteer application mode
  - duplicate same-activity phone and full-position business-rule errors
  - admin volunteer application list, approve, reject, and cancel APIs
  - approved-only position capacity counting
  - volunteer application export as Excel-openable UTF-8 CSV
- Issue 009 volunteer attendance and service hours:
  - public mobile volunteer attendance status lookup API
  - public mobile volunteer check-in/check-out APIs and H5 attendance mode
  - approved-only check-in with `NOT_APPROVED` business-rule error
  - duplicate check-in/check-out business-rule errors
  - admin volunteer attendance list, manual check-in, and manual check-out APIs
  - admin service-minute adjustment with reason and attendance revocation APIs
  - default service minutes from check-out minus check-in, adjusted effective minutes, and revoked effective minutes as zero
- Issue 010 visitor report management:
  - admin visitor report create, list, detail, update, and logical delete APIs
  - optional activity linkage with existing-activity validation
  - keyword, linked activity, and visit-date range filters
  - visitor-report permission enforcement for list, write, delete, and export operations
  - visitor report export as Excel-openable UTF-8 CSV with confirmed columns
- Issue 011 activity files and photo archive:
  - local file storage rooted at `science-ops.storage.local-path`
  - admin activity file upload, list, preview, download, delete, and photo ZIP APIs
  - cover, attachment, and photo category validation with category-specific size limits
  - `UNSUPPORTED_FILE_TYPE` and `FILE_TOO_LARGE` business-rule errors
  - file metadata persistence in `file_asset`
  - logical deletion hiding normal lists and excluding deleted photos from ZIP downloads
- Issue 012 survey configuration:
  - admin survey create, detail, update, publish, and close APIs
  - one V1 survey per activity enforcement
  - survey title and description management
  - single-choice, multiple-choice, rating, and text question management
  - choice-question option management
  - nested survey detail with sorted questions and sorted options for admin editing
- Issue 013 survey response, statistics, and export:
  - public mobile survey eligibility, detail, and response submission APIs
  - checked-in registration and published-survey eligibility enforcement
  - single-choice, multiple-choice, rating, and text answer persistence
  - duplicate response prevention with `DUPLICATE_SUBMISSION`
  - unchecked registration rejection with `NOT_CHECKED_IN`
  - submitted-survey configuration edit locking
  - admin survey statistics, raw response list, and Excel-openable UTF-8 CSV export APIs
- Issue 014 dashboard and activity summary:
  - admin activity detail summary metrics for registration, check-in, volunteer, survey, and photo data
  - check-in rate, survey average rating, and total effective volunteer service minutes
  - role-aware dashboard summary endpoint
  - dashboard upcoming activity endpoint
  - dashboard pending volunteer application endpoint
  - exclusion of cancelled registrations, revoked check-ins, revoked volunteer attendance, and deleted photos
- Issue 015 operation log audit query:
  - append-only operation log repository and service
  - super-admin-only operation log list and detail APIs
  - filters by admin user, action, target type, and created-time range
  - audit writes for activity lifecycle, exports, registration backfill/cancel, check-in backfill/revoke, volunteer review/attendance, and file deletion
  - request metadata capture for IP and User-Agent
- Issue 016 admin account and role management:
  - super-admin-only admin user list, detail, create, update, password reset, and role assignment APIs
  - account list filters by keyword, status, and role
  - role and permission lookup APIs
  - BCrypt password writes for created and reset accounts
  - disabled accounts rejected at login through existing auth flow
- Issue 017 delivery documentation:
  - Docker Compose deployment guide
  - operation manual covering admin and mobile H5 workflows
  - seeded test account handoff notes
  - V1 acceptance checklist
  - README and STATUS handoff updates

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
- `docs/deployment/001-docker-compose-deployment.md`
- `docs/operations/001-operation-manual.md`
- `docs/acceptance/001-v1-acceptance-checklist.md`
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

V1 issue implementation is complete. Recommended handoff actions:

```text
1. Review docs/acceptance/001-v1-acceptance-checklist.md with the delivery owner.
2. Change seeded default passwords before real use.
3. Commit and push the Issue 017 documentation changes.
```

## Not Started

- No remaining V1 implementation issue is open.
- Optional future work: richer production-grade admin frontend screens, native `.xlsx` exports, automatic backups, HTTPS/domain setup, CI/CD, object storage, SMS, mini program, and certificate templates.

## Known Notes

- `docs/api/001-api-design.md` is still marked `Draft`.
- `docs/prototype/*.md` files are low-fidelity planning docs, not final visual designs.
- Issue 001 intentionally contains placeholder pages and `/api/health` only; business functionality starts in later issues.
- Issue 002 verified Flyway migrations with H2 MySQL compatibility mode. Docker CLI is not available on this machine, so live MySQL container verification has not been run.
- Issue 003 seeds development admin accounts only when `admin_user` is empty. Default local password is `password123`.
- Issue 006 implements audience registration and a CSV export that Excel can open. Native `.xlsx` export can be added later if delivery requires it.
- Issue 007 implements audience check-in and a CSV export that Excel can open. Native `.xlsx` export can be added later if delivery requires it.
- Issue 008 implements volunteer position/application review and a CSV export that Excel can open. Native `.xlsx` export can be added later if delivery requires it.
- Issue 009 implements volunteer check-in, check-out, service-hour calculation, manual corrections, and revocation. Issue 008 approval does not create attendance; attendance is created on check-in.
- Issue 010 implements visitor report management and a CSV export that Excel can open. Native `.xlsx` export can be added later if delivery requires it.
- Issue 011 implements local file storage and photo ZIP downloads. Deployment documentation should define backup expectations for `science-ops.storage.local-path`.
- Issue 012 implements survey configuration only. Public survey response, statistics, and export are handled by Issue 013.
- Issue 013 implements survey response, statistics, and export. Native `.xlsx` export can be added later if delivery requires it.
- Issue 014 adds dashboard read APIs and activity detail summary metrics. Dashboard metric visibility follows the admin role's permissions.
- Issue 015 implements append-only operation logs and super-admin query APIs. There is intentionally no log deletion API in V1.
- Issue 016 implements account management APIs for super admin only. Account deletion is not included in V1; accounts are enabled or disabled by status.
- Issue 017 completes delivery documentation and acceptance checklist. Backend tests and both frontend builds passed on 2026-06-07.
- Admin frontend build currently emits a large-chunk warning and third-party Rollup comment warnings, but the production build succeeds.
