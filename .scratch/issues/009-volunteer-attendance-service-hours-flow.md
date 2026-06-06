# Issue 009: 志愿者签到、签退和服务时长闭环

Status: `ready-for-agent`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `docs/api/001-api-design.md`
- `docs/prototype/002-admin-wireframes.md`
- `docs/prototype/003-mobile-wireframes.md`
- `docs/testing/001-test-plan.md`

## What to build

实现志愿者审核通过后的签到、签退、服务时长计算、后台手动补签/补签退、服务时长修正和撤销。

## Acceptance criteria

- [ ] 志愿者审核通过后才能签到。
- [ ] 未审核通过的志愿者签到返回 `NOT_APPROVED`。
- [ ] 志愿者签到时创建考勤记录，不在审核通过时预创建空考勤。
- [ ] 已签到志愿者可以签退。
- [ ] 服务时长默认按签退时间减签到时间计算，单位分钟。
- [ ] 重复签到被拒绝。
- [ ] 重复签退被拒绝。
- [ ] 后台可以手动补签。
- [ ] 后台可以手动补签退。
- [ ] 后台可以修正服务时长并填写原因。
- [ ] 后台可以撤销考勤。
- [ ] 撤销后的考勤不计入有效服务时长。
- [ ] 覆盖审核前签到、签到、签退、重复操作、手动补录、时长修正和撤销的测试。

## Blocked by

- `.scratch/issues/008-volunteer-position-application-review-flow.md`

