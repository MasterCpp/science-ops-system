# Issue 008: 志愿者岗位和报名申请闭环

Status: `ready-for-agent`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `docs/api/001-api-design.md`
- `docs/prototype/002-admin-wireframes.md`
- `docs/prototype/003-mobile-wireframes.md`
- `docs/testing/001-test-plan.md`

## What to build

实现志愿者岗位管理、手机端岗位列表、志愿者申请、后台审核和志愿者基础数据导出。

## Acceptance criteria

- [ ] 允许有权限的管理员创建、编辑、删除志愿者岗位。
- [ ] 岗位包含名称、说明、人数上限、服务开始时间和服务结束时间。
- [ ] 手机端岗位列表展示岗位说明、服务时间、容量和已通过人数。
- [ ] 志愿者可以提交姓名、手机号、单位/学校、年龄段、可服务时间说明、经验说明和备注完成申请。
- [ ] 同一活动下同一手机号只能申请一个岗位。
- [ ] 岗位满员时不能继续申请。
- [ ] 新申请状态为 `PENDING`。
- [ ] 管理员可以审核通过、拒绝或取消申请。
- [ ] 只有审核通过的申请占用岗位容量。
- [ ] 志愿者数据导出包含已确认列。
- [ ] 覆盖岗位 CRUD、申请成功、重复申请、岗位满员、审核通过、审核拒绝、取消和导出的测试。

## Blocked by

- `.scratch/issues/004-admin-activity-lifecycle.md`

