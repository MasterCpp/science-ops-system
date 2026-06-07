# Issue 015: Operation Log Audit Query
Status: `completed`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `docs/api/001-api-design.md`
- `docs/database/001-er-design.md`
- `docs/prototype/002-admin-wireframes.md`
- `docs/testing/001-test-plan.md`

## What was built

Implemented append-only admin operation logging for key back-office actions, plus super-admin-only operation log list/detail APIs.

## Acceptance criteria

- [x] Activity create, update, and delete write operation logs.
- [x] Activity publish, start, end, archive, and unarchive write operation logs.
- [x] Export actions write operation logs for registration, check-in, volunteer application, visitor report, survey response, and photo ZIP exports.
- [x] Registration backfill and cancel write operation logs.
- [x] Manual audience check-in and check-in revoke write operation logs.
- [x] Volunteer application review actions write operation logs.
- [x] Volunteer attendance manual check-in, manual check-out, adjustment, and revoke write operation logs.
- [x] Photo and attachment deletion writes operation logs.
- [x] Operation logs include admin user ID, username, role code, action, target type, target ID, target summary, IP, User-Agent, detail JSON, and created time.
- [x] Super admin can view operation log list and detail.
- [x] Activity admin and volunteer admin cannot view operation logs.
- [x] Operation log list supports filtering by admin, action, target type, and created-time range.
- [x] V1 provides no operation log deletion API.
- [x] Tests cover log writing, filtering, detail, and permission rejection.

## Implemented files

- `server/src/main/java/com/example/scienceops/operationlog/`
- `server/src/main/java/com/example/scienceops/admin/operationlog/AdminOperationLogController.java`
- `server/src/test/java/com/example/scienceops/OperationLogAuditFlowTests.java`

## Verification

- `mvn -f server/pom.xml test`
- Result: `50 tests, 0 failures, 0 errors`

## Blocked by

- `.scratch/issues/003-admin-auth-jwt-rbac-baseline.md`
- `.scratch/issues/004-admin-activity-lifecycle.md`
