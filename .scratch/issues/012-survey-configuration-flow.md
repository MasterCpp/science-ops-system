# Issue 012: 满意度问卷配置闭环

Status: `ready-for-agent`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `docs/api/001-api-design.md`
- `docs/database/001-er-design.md`
- `docs/prototype/002-admin-wireframes.md`
- `docs/testing/001-test-plan.md`

## What to build

实现活动满意度问卷配置，包括一活动一问卷、题目、选项、排序、发布和关闭。

## Acceptance criteria

- [ ] 每场活动最多创建一份 V1 问卷。
- [ ] 问卷可以设置标题和说明。
- [ ] 问卷支持单选、多选、评分和文本题。
- [ ] 单选和多选题支持选项管理。
- [ ] 题目支持必填标记。
- [ ] 题目和选项支持排序。
- [ ] 管理员可以发布问卷。
- [ ] 管理员可以关闭问卷。
- [ ] 后台问卷编辑页能展示并编辑题目和选项。
- [ ] 覆盖一活动一问卷、题型、选项、排序、发布和关闭的测试。

## Blocked by

- `.scratch/issues/004-admin-activity-lifecycle.md`

