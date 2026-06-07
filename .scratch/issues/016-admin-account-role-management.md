# Issue 016: Admin Account and Role Management
Status: `completed`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `docs/adr/004-authentication-and-authorization.md`
- `docs/api/001-api-design.md`
- `docs/prototype/002-admin-wireframes.md`
- `docs/testing/001-test-plan.md`

## What was built

Implemented super-admin-only APIs for managing back-office admin accounts, account status, password reset, role assignment, and role/permission lookup.

## Acceptance criteria

- [x] Super admin can view the admin account list.
- [x] Account list supports keyword, status, and role filters.
- [x] Super admin can create admin accounts.
- [x] Super admin can edit display name, phone, and status.
- [x] Super admin can enable and disable accounts through status updates.
- [x] Super admin can reset account passwords.
- [x] Super admin can assign roles to accounts.
- [x] Super admin can view role and permission lists.
- [x] Activity admin cannot access account management.
- [x] Volunteer admin cannot access account management.
- [x] Disabled accounts cannot log in.
- [x] Tests cover account create, edit, enable/disable, password reset, role assignment, permission rejection, and disabled-login rejection.

## Implemented files

- `server/src/main/java/com/example/scienceops/admin/adminuser/`
- `server/src/test/java/com/example/scienceops/AdminUserManagementFlowTests.java`

## Verification

- `mvn -f server/pom.xml test`
- Result: `53 tests, 0 failures, 0 errors`

## Blocked by

- `.scratch/issues/003-admin-auth-jwt-rbac-baseline.md`
