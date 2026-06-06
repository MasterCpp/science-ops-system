# Science Operations System

科普运营系统是一个用于练习企业级项目从 0 到 1 流程的 Web 项目。系统目标是管理科普活动的创建、报名、签到、志愿者、来访报备、满意度问卷、照片归档、数据导出和活动总结。

当前已进入实现阶段。仓库包含项目上下文、需求、PRD、架构决策、数据库设计、原型、API 设计，以及可本地启动的前后端基线、数据库迁移基线和后台认证/RBAC 基线。

## Current Status

Read `STATUS.md` first for the current project phase, completed documents, next step, and known open work.

## Where To Start

For humans:

1. `README.md`
2. `STATUS.md`
3. `CONTEXT.md`
4. `docs/prd/001-科普运营系统-v1.md`
5. `docs/database/001-er-design.md`
6. `docs/prototype/001-page-map.md`
7. `docs/api/001-api-design.md`

For AI agents:

1. `AGENTS.md`
2. `STATUS.md`
3. `CONTEXT.md`
4. `docs/agents/issue-tracker.md`
5. `docs/agents/triage-labels.md`
6. `docs/agents/domain.md`
7. The current task's PRD, ADR, issue, or design document

## Project Scope

V1 includes:

- Back-office management system
- Mobile H5 registration and check-in pages
- Activity lifecycle management
- Audience registration and check-in
- Volunteer positions, applications, review, attendance, and service-hour statistics
- Visitor report management
- Activity plan and process management
- Satisfaction survey configuration, submission, statistics, and export
- Activity photos and attachments
- Excel exports and activity summary data
- Operation logs and RBAC permissions

V1 does not include:

- WeChat mini program
- Official account automatic publishing
- SMS verification
- Complex low-code form designer
- Object storage
- Automatic database backup
- Formal volunteer service certificate template
- Fixed Logo/header/seal export templates

## Technical Direction

- Repository layout: frontend/backend separated monorepo
- Admin web: Vue 3 + Element Plus
- Mobile web: H5 frontend
- Backend: Spring Boot
- Persistence: MyBatis-Plus + MySQL 8
- Migrations: Flyway
- Auth: Spring Security + JWT + RBAC
- File storage: local server storage + MySQL metadata
- Deployment baseline: Docker Compose + MySQL + Spring Boot + Nginx

## Local Development Baseline

### Directory Layout

```text
apps/admin-web/   # Back-office placeholder app
apps/mobile-web/  # Mobile H5 placeholder app
server/           # Spring Boot API service
docs/             # Project documents
.scratch/         # Local issue tracker
deploy/nginx/     # Nginx placeholder deployment config
```

### Ports

| Unit | Local port | Command |
| --- | --- | --- |
| Admin web | `5173` | `npm.cmd run dev:admin` |
| Mobile H5 | `5174` | `npm.cmd run dev:mobile` |
| Spring Boot API | `8080` | `mvn -f server/pom.xml spring-boot:run` |
| Nginx compose entry | `8088` | `docker compose up` |
| MySQL compose service | `3306` | `docker compose up` |

PowerShell blocks `npm.ps1` on this machine, so use `npm.cmd` from PowerShell.

### First Setup

```powershell
npm.cmd install
mvn -f server/pom.xml test
```

### Run Locally

Admin web:

```powershell
npm.cmd run dev:admin
```

Open `http://localhost:5173/admin/`.

Mobile H5:

```powershell
npm.cmd run dev:mobile
```

Open `http://localhost:5174/mobile/`.

Backend:

```powershell
mvn -f server/pom.xml spring-boot:run
```

Health check:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

### Docker Compose Skeleton

Copy `.env.example` to `.env` if local overrides are needed. The current Compose baseline includes:

- `mysql`: MySQL 8 database service.
- `server`: Spring Boot backend container with local file storage mounted at `/app/storage`.
- `nginx`: static file and reverse proxy placeholder for `/admin/`, `/mobile/`, and `/api/`.

Build frontend assets before using the Nginx static routes:

```powershell
npm.cmd run build:admin
npm.cmd run build:mobile
docker compose up --build
```

### Admin Auth Baseline

Backend startup seeds local RBAC baseline data when `admin_user` is empty.

Default password for seeded accounts:

```text
password123
```

| Username | Role | Intended access |
| --- | --- | --- |
| `superadmin` | `SUPER_ADMIN` | Account management and operation logs included |
| `activityadmin` | `ACTIVITY_ADMIN` | Activity operations, registration, check-in, visitor reports, surveys, files |
| `volunteeradmin` | `VOLUNTEER_ADMIN` | Volunteer operations only |
| `disabledadmin` | `SUPER_ADMIN` | Disabled account for login rejection tests |

Auth endpoints:

```text
POST /api/admin/auth/login
GET  /api/admin/auth/me
```

JWT configuration:

```text
JWT_SECRET
JWT_TTL_MINUTES
```

### Admin Activity Lifecycle Baseline

Protected activity endpoints are available under:

```text
GET    /api/admin/activities
POST   /api/admin/activities
GET    /api/admin/activities/{activityId}
PUT    /api/admin/activities/{activityId}
DELETE /api/admin/activities/{activityId}
POST   /api/admin/activities/{activityId}/publish
POST   /api/admin/activities/{activityId}/start
POST   /api/admin/activities/{activityId}/end
POST   /api/admin/activities/{activityId}/archive
POST   /api/admin/activities/{activityId}/unarchive
```

Implemented lifecycle:

```text
DRAFT -> REGISTRATION_OPEN -> IN_PROGRESS -> ENDED -> ARCHIVED
```

Archived activities are read-only by default. Super admin can unarchive and delete activities. Activity admin can manage activities but cannot delete them.

### Activity Structure and Public Detail Baseline

Protected activity structure endpoints:

```text
GET    /api/admin/activities/{activityId}/process-items
POST   /api/admin/activities/{activityId}/process-items
PUT    /api/admin/activities/{activityId}/process-items/{itemId}
DELETE /api/admin/activities/{activityId}/process-items/{itemId}
GET    /api/admin/activities/{activityId}/custom-fields
POST   /api/admin/activities/{activityId}/custom-fields
PUT    /api/admin/activities/{activityId}/custom-fields/{fieldId}
DELETE /api/admin/activities/{activityId}/custom-fields/{fieldId}
```

Public mobile activity detail endpoint:

```text
GET /api/mobile/activities/{activityId}
```

The mobile H5 app reads the public detail endpoint and renders activity information, remaining capacity, process items, base registration fields, configured custom fields, and unavailable registration states.

## Important Documents

- `AGENTS.md`: rules for AI agents working in this repo
- `STATUS.md`: current phase and next actions
- `CONTEXT.md`: long-lived project context
- `docs/requirements/需求澄清.md`: confirmed requirement clarification
- `docs/prd/001-科普运营系统-v1.md`: V1 PRD
- `docs/adr/`: accepted architecture decision records
- `docs/database/001-er-design.md`: V1 ER/database design
- `docs/prototype/`: page map and low-fidelity wireframes
- `docs/api/001-api-design.md`: first-pass API design
- `.scratch/issues/`: local Markdown issue tracker

## Working Rule

Follow `.scratch/issues/` dependency order. Authentication, RBAC, activity lifecycle APIs, activity process items, registration custom fields, and public mobile activity detail are available. Registration submission, check-in, volunteer, survey, export, and other business workflows should still be implemented only when their corresponding issue is started.
