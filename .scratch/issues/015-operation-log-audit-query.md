# Issue 015: 操作日志和审计查询

Status: `ready-for-agent`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `docs/api/001-api-design.md`
- `docs/database/001-er-design.md`
- `docs/prototype/002-admin-wireframes.md`
- `docs/testing/001-test-plan.md`

## What to build

实现关键后台操作的操作日志记录、日志列表、筛选和详情查看，仅超级管理员可访问。

## Acceptance criteria

- [ ] 活动创建、修改、删除写入操作日志。
- [ ] 活动发布、开始、结束、归档写入操作日志。
- [ ] 导出操作写入操作日志。
- [ ] 报名补录和取消写入操作日志。
- [ ] 签到补签和撤销写入操作日志。
- [ ] 志愿者审核写入操作日志。
- [ ] 照片/附件删除写入操作日志。
- [ ] 操作日志包含管理员 ID、用户名、角色、动作、目标类型、目标 ID、目标摘要、IP、User-Agent、详情和时间。
- [ ] 超级管理员可以查看日志列表和详情。
- [ ] 活动管理员和志愿者管理员不能查看操作日志。
- [ ] 日志列表支持管理员、动作、目标类型和时间范围筛选。
- [ ] V1 不提供操作日志删除能力。
- [ ] 覆盖日志写入、筛选、详情和权限拒绝测试。

## Blocked by

- `.scratch/issues/003-admin-auth-jwt-rbac-baseline.md`
- `.scratch/issues/004-admin-activity-lifecycle.md`

