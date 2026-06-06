# Issue 001: 项目骨架和本地开发基线

Status: `completed`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `README.md`
- `STATUS.md`
- `AGENTS.md`
- `docs/adr/001-backend-stack.md`
- `docs/adr/002-repository-layout.md`
- `docs/adr/006-deployment-baseline.md`

## What to build

初始化 monorepo 项目骨架，让后台前端、手机 H5、Spring Boot 后端、文档和本地开发配置处在同一个项目根目录下。这个切片只建立可运行的空应用基线，不实现业务功能。

Target layout:

```text
apps/admin-web/
apps/mobile-web/
server/
docs/
.scratch/
```

## Acceptance criteria

- [ ] 项目根目录包含 `apps/admin-web/`、`apps/mobile-web/`、`server/`、`docs/`、`.scratch/`。
- [ ] 后台前端应用可以本地启动并显示基础占位页。
- [ ] 手机 H5 应用可以本地启动并显示基础占位页。
- [ ] Spring Boot 后端可以本地启动并提供健康检查接口。
- [ ] 本地开发配置清楚记录端口、环境变量和启动命令。
- [ ] Docker Compose 骨架包含 MySQL、Spring Boot、Nginx 的规划或占位配置。
- [ ] README 或开发文档说明如何启动当前空项目。
- [ ] 不实现活动、报名、签到等业务功能。

## Blocked by

None - can start immediately

## Completion notes

- Completed on 2026-06-05.
- Admin web, mobile H5, Spring Boot health endpoint, local development commands, and Docker Compose baseline were implemented.
- Business modules remain intentionally out of scope for this issue.
