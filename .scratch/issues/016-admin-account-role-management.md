# Issue 016: 后台账号和角色权限管理页面

Status: `ready-for-agent`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `docs/adr/004-authentication-and-authorization.md`
- `docs/api/001-api-design.md`
- `docs/prototype/002-admin-wireframes.md`
- `docs/testing/001-test-plan.md`

## What to build

实现超级管理员管理后台账号、启停账号、重置密码、分配角色和查看角色权限。

## Acceptance criteria

- [ ] 超级管理员可以查看后台账号列表。
- [ ] 账号列表支持关键词、状态和角色筛选。
- [ ] 超级管理员可以创建后台账号。
- [ ] 超级管理员可以编辑账号显示名、手机号和状态。
- [ ] 超级管理员可以启用和禁用账号。
- [ ] 超级管理员可以重置账号密码。
- [ ] 超级管理员可以给账号分配角色。
- [ ] 超级管理员可以查看角色和权限列表。
- [ ] 活动管理员不能访问账号管理。
- [ ] 志愿者管理员不能访问账号管理。
- [ ] 被禁用账号不能登录。
- [ ] 覆盖账号创建、编辑、启停、重置密码、分配角色、权限拒绝和禁用登录测试。

## Blocked by

- `.scratch/issues/003-admin-auth-jwt-rbac-baseline.md`

