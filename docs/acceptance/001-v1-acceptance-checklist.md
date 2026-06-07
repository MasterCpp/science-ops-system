# V1 Acceptance Checklist

## Purpose

Use this checklist before handoff to confirm the delivered V1 matches the PRD, test plan, and implemented business workflows.

## Source Documents

- `docs/prd/001-科普运营系统-v1.md`
- `docs/testing/001-test-plan.md`
- `docs/api/001-api-design.md`
- `docs/database/001-er-design.md`
- `.scratch/issues/001-project-scaffold-and-dev-baseline.md` through `.scratch/issues/017-deployment-docs-operation-manual-acceptance.md`

## Technical Acceptance

- [ ] Repository contains admin web, mobile web, backend, docs, deployment config, and local issue tracker.
- [ ] Backend starts successfully.
- [ ] `/api/health` returns success.
- [ ] Flyway creates the V1 schema from `V1__init_schema.sql`.
- [ ] Backend tests pass with `mvn -f server/pom.xml test`.
- [ ] Admin frontend builds with `npm.cmd run build:admin`.
- [ ] Mobile frontend builds with `npm.cmd run build:mobile`.
- [ ] Docker Compose starts MySQL, backend, and Nginx.
- [ ] Nginx serves `/admin/` and `/mobile/`.
- [ ] Nginx proxies `/api/` to the backend.

## Deployment Acceptance

- [ ] `.env.example` lists deployment variables.
- [ ] MySQL database, user, password, root password, and port are documented.
- [ ] Backend port, JWT settings, and datasource settings are documented.
- [ ] Local file storage mount is documented.
- [ ] Startup order is documented.
- [ ] Flyway migration behavior is documented.
- [ ] MySQL and file-storage backup/restore steps are documented.

## Test Accounts

- [ ] `superadmin / password123` can log in and has super-admin access.
- [ ] `activityadmin / password123` can log in and has activity operations access.
- [ ] `volunteeradmin / password123` can log in and has volunteer operations access.
- [ ] `disabledadmin / password123` cannot log in.
- [ ] Delivery owner has changed default passwords before real use.

## Admin Workflows

- [ ] Super admin can create, edit, unarchive, and delete activities.
- [ ] Activity admin can create and manage activity lifecycle except delete/unarchive.
- [ ] Activity process items can be created, sorted, updated, and deleted.
- [ ] Registration custom fields can be created, sorted, updated, and deleted.
- [ ] Activity detail shows summary metrics.
- [ ] Dashboard summary, upcoming activities, and pending volunteer application data are available according to role permissions.

## Mobile H5 Workflows

- [ ] Public mobile activity detail renders.
- [ ] Audience registration succeeds when the activity is open and capacity/deadline rules allow it.
- [ ] Audience registration rejects duplicate phone, late registration, and capacity overflow.
- [ ] Audience check-in succeeds for a valid registration while the activity is in progress.
- [ ] Audience check-in rejects missing/cancelled registrations and duplicate active check-in.
- [ ] Volunteer positions are visible on mobile.
- [ ] Volunteer application succeeds for an available position.
- [ ] Volunteer application rejects duplicate same-activity phone and full positions.
- [ ] Volunteer attendance supports status lookup, check-in, and check-out.
- [ ] Survey eligibility, detail, and response submission work for checked-in registrations.

## Back-Office Data Workflows

- [ ] Admin registration list, backfill, cancel, and export work.
- [ ] Admin check-in list, manual check-in, revoke, and export work.
- [ ] Volunteer position CRUD works.
- [ ] Volunteer application list, approve, reject, cancel, and export work.
- [ ] Volunteer attendance list, manual check-in, manual check-out, adjustment, and revoke work.
- [ ] Visitor report create, list, detail, update, delete, and export work.
- [ ] Survey create, update, publish, close, question/option management, statistics, raw response list, and export work.
- [ ] File upload, list, preview, download, delete, and photo ZIP work.

## Audit and Account Workflows

- [ ] Operation logs are created for configured sensitive admin operations.
- [ ] Super admin can filter and view operation log details.
- [ ] Activity admin and volunteer admin cannot view operation logs.
- [ ] Super admin can list, create, edit, enable/disable, reset password, and assign roles to admin accounts.
- [ ] Activity admin and volunteer admin cannot access account management.
- [ ] Role and permission lists are visible to super admin.

## Documentation Acceptance

- [ ] `README.md` points to the current delivery docs.
- [ ] `STATUS.md` reflects Issue 017 completion.
- [ ] Deployment guide exists.
- [ ] Operation manual exists.
- [ ] Acceptance checklist exists.
- [ ] Documentation does not claim unsupported V1 features such as mini program, SMS, object storage, automatic backup, or native `.xlsx` export.

## Known V1 Limitations

- Frontend apps are early V1 interfaces and placeholders in some management areas.
- Exports are UTF-8 CSV or ZIP, not native `.xlsx`.
- File storage is local server storage, not object storage.
- Backup steps are documented/manual; automatic backup is not implemented.
- HTTPS, domain setup, CI/CD, and production certificate management are not included.
