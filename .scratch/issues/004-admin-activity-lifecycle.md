# Issue 004: 后台活动创建、编辑和生命周期管理

Status: `completed`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `docs/prd/001-科普运营系统-v1.md`
- `docs/api/001-api-design.md`
- `docs/prototype/002-admin-wireframes.md`
- `docs/testing/001-test-plan.md`

## What to build

实现后台活动列表、创建、编辑、详情基础信息和状态流转，让管理员可以管理活动生命周期。

## Acceptance criteria

- [ ] 活动可以创建为草稿，字段包括标题、封面、简介、时间、地点、容量、报名截止、负责人、联系电话、方案、状态。
- [ ] 草稿活动允许编辑所有字段。
- [ ] 活动列表支持关键词、状态和时间范围筛选。
- [ ] 活动详情展示基础信息和状态。
- [ ] 草稿活动可以发布为报名中。
- [ ] 报名中活动可以变为进行中。
- [ ] 进行中活动可以变为已结束。
- [ ] 已结束活动可以归档。
- [ ] 已归档活动默认只读。
- [ ] 超级管理员可以解除归档。
- [ ] 活动管理员不能删除活动。
- [ ] 超级管理员可以删除活动。
- [ ] 进行中活动拒绝修改报名字段和容量。
- [ ] 覆盖活动创建、编辑、状态流转、归档只读、权限控制的测试。

## Blocked by

- `.scratch/issues/002-database-migrations-and-base-entities.md`
- `.scratch/issues/003-admin-auth-jwt-rbac-baseline.md`

## Completion notes

- Implemented backend admin activity APIs for create, list, detail, update, delete, and lifecycle transitions.
- Activity creation always starts as `DRAFT`.
- List filters support `keyword`, `status`, `startFrom`, `startTo`, `page`, and `pageSize`.
- Status transitions are enforced as `DRAFT -> REGISTRATION_OPEN -> IN_PROGRESS -> ENDED -> ARCHIVED`.
- Archived activities reject normal updates.
- Super admin can unarchive and delete; activity admin cannot delete.
- In-progress activities reject capacity and registration deadline changes.
- Added backend API tests covering create/edit/list/lifecycle/archive/permission behavior.
