# Issue 003: 后台登录、JWT 和 RBAC 权限基线

Status: `completed`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `docs/adr/004-authentication-and-authorization.md`
- `docs/api/001-api-design.md`
- `docs/testing/001-test-plan.md`
- `docs/database/001-er-design.md`

## What to build

实现后台管理员账号密码登录、JWT 鉴权、RBAC 权限基础、当前用户信息接口和受保护 API 基线。

## Acceptance criteria

- [ ] 后台账号可以使用用户名和密码登录。
- [ ] 密码以安全哈希形式存储，不能明文保存。
- [ ] 登录成功返回 JWT 和当前用户角色/权限。
- [ ] `/api/admin/auth/me` 能返回当前用户信息、角色和权限。
- [ ] 未登录访问后台受保护接口返回 `UNAUTHORIZED`。
- [ ] 无权限访问接口返回 `FORBIDDEN`。
- [ ] 超级管理员可以访问账号管理和操作日志相关权限。
- [ ] 活动管理员不能访问账号管理和操作日志相关权限。
- [ ] 志愿者管理员不能访问报名、问卷、来访报备和账号管理相关权限。
- [ ] 覆盖登录成功、登录失败、禁用账号、未登录、无权限、角色访问控制的测试。

## Blocked by

- `.scratch/issues/001-project-scaffold-and-dev-baseline.md`
- `.scratch/issues/002-database-migrations-and-base-entities.md`

## Completion notes

- Completed on 2026-06-06.
- Added Spring Security stateless JWT auth for `/api/admin/**`.
- Added BCrypt password hashing and startup seed data for local admin users, roles, and permissions.
- Added `POST /api/admin/auth/login` and `GET /api/admin/auth/me`.
- Added RBAC probe endpoints to validate account, operation-log, registration, survey, visitor-report, and file permissions without implementing business workflows.
- Verified login success, invalid password, disabled account, missing/invalid JWT, and role permission matrix with backend tests.
