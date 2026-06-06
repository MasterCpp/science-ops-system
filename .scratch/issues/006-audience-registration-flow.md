# Issue 006: 观众报名闭环

Status: `completed`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `docs/api/001-api-design.md`
- `docs/prototype/002-admin-wireframes.md`
- `docs/prototype/003-mobile-wireframes.md`
- `docs/testing/001-test-plan.md`

## What to build

实现手机端观众报名、后台报名管理、补录、取消和报名表导出，形成观众报名完整闭环。

## Acceptance criteria

- [ ] 移动端活动详情页能展示活动标题、时间、地点、剩余名额、报名截止时间和报名表字段。
- [ ] 用户可以提交姓名、手机号、报名人数、单位/学校、年龄段、备注和自定义字段完成报名。
- [ ] 报名成功后进入报名成功页，并展示活动名称、姓名、手机号和报名人数。
- [ ] 同一活动下同一手机号重复报名会失败，并返回 `DUPLICATE_SUBMISSION`。
- [ ] 超过报名截止时间后报名失败，并返回 `DEADLINE_PASSED`。
- [ ] 报名人数超过剩余容量时失败，并返回 `CAPACITY_FULL`。
- [ ] 后台活动报名列表能看到报名记录和自定义字段值。
- [ ] 后台管理员可以补录报名。
- [ ] 后台管理员可以取消报名。
- [ ] 取消报名后该报名不再占用容量。
- [ ] 报名表 Excel 导出包含已确认列。
- [ ] 覆盖报名成功、重复手机号、截止时间、容量不足、后台补录、后台取消和导出的测试。

## Blocked by

- `.scratch/issues/005-activity-process-custom-fields-public-detail.md`

## Completion notes

- Implemented public mobile registration submission: `POST /api/mobile/activities/{activityId}/registrations`.
- Mobile registration saves base fields and configured custom field values.
- Mobile H5 activity page now submits the registration form and shows a registration success state.
- Public registration rejects duplicate active phone numbers with `DUPLICATE_SUBMISSION`.
- Public registration rejects late submissions with `DEADLINE_PASSED`.
- Public and admin registration reject over-capacity submissions with `CAPACITY_FULL`.
- Implemented admin registration list and backfill APIs.
- Implemented admin registration cancellation; cancelled registrations no longer consume capacity.
- Implemented registration export as a UTF-8 CSV file that Excel can open.
- Added backend tests for success, duplicate phone, deadline, capacity, admin list/backfill/cancel, capacity release, and export.
