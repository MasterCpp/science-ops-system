# Issue 017: 部署文档、操作说明和验收清单

Status: `ready-for-agent`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `README.md`
- `STATUS.md`
- `docs/adr/006-deployment-baseline.md`
- `docs/testing/001-test-plan.md`

## What to build

在所有 V1 功能切片完成后，补齐 Docker Compose 部署文档、系统操作说明、测试账号说明和验收清单，形成可交付资料。

## Acceptance criteria

- [ ] 部署文档说明 Docker Compose 启动 MySQL、Spring Boot 和 Nginx。
- [ ] 部署文档说明环境变量、端口、数据库连接、文件存储挂载和启动顺序。
- [ ] 部署文档说明 Flyway 迁移如何执行。
- [ ] 部署文档说明 MySQL 数据和本地文件存储如何备份。
- [ ] 操作说明覆盖后台登录、活动管理、报名管理、签到管理、志愿者管理、来访报备、问卷、照片、导出、日志和账号管理。
- [ ] 手机 H5 操作说明覆盖报名、签到、志愿者申请、志愿者签到/签退、问卷填写。
- [ ] 提供测试账号说明，包括超级管理员、活动管理员、志愿者管理员。
- [ ] 验收清单覆盖 PRD、测试计划和主要业务闭环。
- [ ] README 和 STATUS 更新到交付前状态。
- [ ] 不引入未实现功能的说明。

## Blocked by

- `.scratch/issues/001-project-scaffold-and-dev-baseline.md`
- `.scratch/issues/002-database-migrations-and-base-entities.md`
- `.scratch/issues/003-admin-auth-jwt-rbac-baseline.md`
- `.scratch/issues/004-admin-activity-lifecycle.md`
- `.scratch/issues/005-activity-process-custom-fields-public-detail.md`
- `.scratch/issues/006-audience-registration-flow.md`
- `.scratch/issues/007-audience-check-in-flow.md`
- `.scratch/issues/008-volunteer-position-application-review-flow.md`
- `.scratch/issues/009-volunteer-attendance-service-hours-flow.md`
- `.scratch/issues/010-visitor-report-management-flow.md`
- `.scratch/issues/011-activity-files-photos-archive-flow.md`
- `.scratch/issues/012-survey-configuration-flow.md`
- `.scratch/issues/013-survey-response-statistics-export-flow.md`
- `.scratch/issues/014-dashboard-and-activity-summary.md`
- `.scratch/issues/015-operation-log-audit-query.md`
- `.scratch/issues/016-admin-account-role-management.md`

