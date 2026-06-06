# Issue 013: 满意度问卷填写、统计和导出闭环

Status: `ready-for-agent`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `docs/api/001-api-design.md`
- `docs/database/001-er-design.md`
- `docs/prototype/002-admin-wireframes.md`
- `docs/prototype/003-mobile-wireframes.md`
- `docs/testing/001-test-plan.md`

## What to build

实现移动端签到后填写问卷、一次提交、答案保存、后台统计、原始明细和问卷结果导出。

## Acceptance criteria

- [ ] 手机端可以校验手机号是否具备问卷填写资格。
- [ ] 未报名手机号不能填写问卷。
- [ ] 未签到报名不能填写问卷，并返回 `NOT_CHECKED_IN`。
- [ ] 未发布或已关闭问卷不能提交。
- [ ] 已签到观众可以提交单选、多选、评分和文本答案。
- [ ] 同一报名同一问卷只能提交一次。
- [ ] 重复提交返回 `DUPLICATE_SUBMISSION`。
- [ ] 提交后的问卷不能编辑。
- [ ] 后台可以查看问卷提交数量和平均评分。
- [ ] 后台可以查看题目级统计和原始明细。
- [ ] 问卷结果 Excel 导出包含已确认列。
- [ ] 覆盖资格校验、成功提交、重复提交、统计、原始明细和导出的测试。

## Blocked by

- `.scratch/issues/007-audience-check-in-flow.md`
- `.scratch/issues/012-survey-configuration-flow.md`

