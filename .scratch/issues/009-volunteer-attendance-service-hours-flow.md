# Issue 009: 志愿者签到、签退和服务时长闭环

Status: `completed`

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

- [x] 志愿者审核通过后才能签到。
- [x] 未审核通过的志愿者签到返回 `NOT_APPROVED`。
- [x] 志愿者签到时创建考勤记录，不在审核通过时预创建空考勤。
- [x] 已签到志愿者可以签退。
- [x] 服务时长默认按签退时间减签到时间计算，单位分钟。
- [x] 重复签到被拒绝。
- [x] 重复签退被拒绝。
- [x] 后台可以手动补签。
- [x] 后台可以手动补签退。
- [x] 后台可以修正服务时长并填写原因。
- [x] 后台可以撤销考勤。
- [x] 撤销后的考勤不计入有效服务时长。
- [x] 覆盖审核前签到、签到、签退、重复操作、手动补录、时长修正和撤销的测试。

## Blocked by

- `.scratch/issues/008-volunteer-position-application-review-flow.md`

## Completion notes

Completed on 2026-06-07.

- Added public volunteer attendance status lookup, check-in, and check-out APIs.
- Added mobile H5 volunteer attendance mode for phone lookup, check-in, and check-out.
- Added admin volunteer attendance list, manual check-in, manual check-out, service-minute adjustment with reason, and revocation APIs.
- Service minutes default to check-out time minus check-in time; adjusted minutes override effective service minutes; revoked attendance counts as zero effective minutes.
- Approval no longer implies attendance creation; attendance is created only at check-in.
- Covered approval gating, duplicate operations, check-out, manual operations, adjustment, revocation, and list behavior in integration tests.
