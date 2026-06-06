# Issue 005: 活动流程、报名自定义字段和公开活动详情

Status: `completed`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `docs/prototype/001-page-map.md`
- `docs/prototype/003-mobile-wireframes.md`
- `docs/api/001-api-design.md`
- `docs/database/001-er-design.md`
- `docs/testing/001-test-plan.md`

## What to build

在活动管理中加入流程时间轴和报名自定义字段，并提供手机端公开活动详情，支撑后续报名页面。

## Acceptance criteria

- [ ] 后台活动编辑页可以添加、编辑、排序、删除流程时间轴条目。
- [ ] 后台活动编辑页可以添加、编辑、排序、删除报名自定义字段。
- [ ] 同一活动下自定义字段 `field_key` 唯一。
- [ ] 公开活动详情接口返回活动标题、封面、简介、时间、地点、剩余容量、报名截止、联系人和自定义字段。
- [ ] 手机 H5 活动详情页展示公开活动信息和报名表字段。
- [ ] 报名截止、容量满、活动非报名中时，手机页面显示不可报名状态。
- [ ] 覆盖流程条目、自定义字段唯一性、公开详情展示和不可报名状态的测试。

## Blocked by

- `.scratch/issues/004-admin-activity-lifecycle.md`

## Completion notes

- Implemented admin process item APIs for add, edit, sort, list, and delete.
- Implemented admin registration custom field APIs for add, edit, sort, list, and delete.
- Enforced active custom field `field_key` uniqueness within one activity.
- Implemented `GET /api/mobile/activities/{activityId}` public detail API.
- Public detail returns activity public fields, remaining capacity, registration deadline, availability state, process items, and custom fields.
- Replaced the mobile H5 placeholder with an activity detail page that renders public information, process items, base registration fields, custom fields, and disabled registration states.
- Added backend tests covering process items, custom field uniqueness, public detail, and unavailable registration states.
