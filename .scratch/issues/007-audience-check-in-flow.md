# Issue 007: 观众签到闭环

Status: `ready-for-agent`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `docs/api/001-api-design.md`
- `docs/prototype/002-admin-wireframes.md`
- `docs/prototype/003-mobile-wireframes.md`
- `docs/testing/001-test-plan.md`

## What to build

实现固定签到入口、手机端观众签到、后台签到列表、手动补签、撤销签到和签到表导出。

## Acceptance criteria

- [ ] 活动详情页能提供固定签到二维码或签到链接。
- [ ] 已报名且未取消的观众可以在活动进行中通过手机号签到。
- [ ] 签到成功后展示姓名和签到时间。
- [ ] 重复签到不会新增有效记录，并返回 `ALREADY_CHECKED_IN`。
- [ ] 未报名手机号签到失败。
- [ ] 已取消报名的手机号签到失败。
- [ ] 活动未开始或已结束时，移动端签到失败并返回 `INVALID_STATE`。
- [ ] 后台可以查看签到列表。
- [ ] 后台可以手动补签。
- [ ] 后台可以撤销签到。
- [ ] 撤销后不计入有效签到人数。
- [ ] 签到表 Excel 导出包含已确认列。
- [ ] 覆盖成功签到、重复签到、未报名、报名取消、活动状态错误、补签、撤销和导出的测试。

## Blocked by

- `.scratch/issues/006-audience-registration-flow.md`

