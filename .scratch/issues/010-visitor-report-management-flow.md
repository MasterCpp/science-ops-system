# Issue 010: 来访报备管理闭环

Status: `completed`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `docs/api/001-api-design.md`
- `docs/prototype/002-admin-wireframes.md`
- `docs/testing/001-test-plan.md`

## What to build

实现来访报备的后台创建、查询、编辑、删除、可选关联活动和 Excel 导出。

## Acceptance criteria

- [x] 活动管理员可以创建来访报备。
- [x] 来访报备字段包括来访单位、联系人、手机号、来访人数、来访日期、来访事由、关联活动和备注。
- [x] 来访报备可以关联活动。
- [x] 来访报备可以不关联活动。
- [x] 来访报备列表支持关键词、关联活动和来访日期筛选。
- [x] 有权限的管理员可以编辑和删除来访报备。
- [x] 志愿者管理员不能管理来访报备。
- [x] 来访报备 Excel 导出包含已确认列。
- [x] 覆盖创建、无关联活动、有关联活动、编辑、删除、权限拒绝和导出的测试。

## Blocked by

- `.scratch/issues/003-admin-auth-jwt-rbac-baseline.md`
- `.scratch/issues/004-admin-activity-lifecycle.md`

## Completion notes

Completed on 2026-06-07.

- Added admin visitor report create, list, detail, update, delete, and export APIs.
- Visitor reports can be linked to an existing activity or created without an activity.
- List and export support keyword, activity, and visit-date range filters.
- Export returns an Excel-openable UTF-8 CSV with the confirmed visitor report columns.
- `visitor-report:manage` is enforced on all visitor report endpoints, so volunteer admins are denied.
- Covered linked/unlinked create, filters, update, delete, permission denial, and export in integration tests.
