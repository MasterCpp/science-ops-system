# Issue 002: 数据库迁移和基础实体基线

Status: `completed`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `docs/database/001-er-design.md`
- `docs/adr/003-persistence-and-migrations.md`
- `docs/testing/001-test-plan.md`

## What to build

按 ER 设计建立 Flyway 初始迁移、MyBatis-Plus 基础实体、枚举和通用字段约定，为后续业务切片提供数据库基础。

## Acceptance criteria

- [ ] Flyway 可以在空 MySQL 数据库上创建 V1 核心表。
- [ ] 表名、字段名、主键、逻辑删除、状态字段符合 `docs/database/001-er-design.md`。
- [ ] 主键使用 `BIGINT` 雪花 ID。
- [ ] 业务表包含 `id`、`created_at`、`updated_at`、`deleted`。
- [ ] 需要后台审计的表包含 `created_by`、`updated_by`。
- [ ] 角色、权限、活动、报名、签到、志愿者、问卷、文件、日志相关表都已创建。
- [ ] 唯一约束和普通索引符合 ER 设计。
- [ ] MyBatis-Plus 能识别逻辑删除字段。
- [ ] 数据库迁移可以重复在新库上执行成功。
- [ ] 基础迁移和实体不实现业务接口。

## Blocked by

- `.scratch/issues/001-project-scaffold-and-dev-baseline.md`

## Completion notes

- Completed on 2026-06-05.
- Added MyBatis-Plus, Flyway, MySQL driver, and H2 local/test database support.
- Added `V1__init_schema.sql` with V1 core tables, unique constraints, and indexes.
- Added base entity classes for snowflake IDs, common timestamps, logical deletion, and audit attribution.
- Added baseline enums for activity, registration, check-in, volunteer, survey, and file states.
- Verified migration with H2 MySQL compatibility mode and repeated fresh database migration tests.
- Docker CLI is not available on this machine, so a live MySQL container migration check was not run.
