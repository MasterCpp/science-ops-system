# Science Operations System

Science Operations System is a V1 web system for managing science outreach activities, registrations, check-ins, volunteers, visitor reports, surveys, files/photos, exports, operation logs, and admin accounts.

This repository is also a learning project that practices an enterprise-style flow from requirement clarification through PRD, ADRs, database design, API design, issue slicing, implementation, testing, deployment documentation, and handoff.

## Current Status

V1 implementation issues `001` through `017` are complete.

Read [STATUS.md](STATUS.md) for the current project state, completed modules, verification notes, and known V1 limitations.

## Delivery Documents

- [Deployment guide](docs/deployment/001-docker-compose-deployment.md)
- [Operation manual](docs/operations/001-operation-manual.md)
- [V1 acceptance checklist](docs/acceptance/001-v1-acceptance-checklist.md)
- [Project status](STATUS.md)
- [V1 PRD](docs/prd/001-科普运营系统-v1.md)
- [API design](docs/api/001-api-design.md)
- [Database design](docs/database/001-er-design.md)
- [Test plan](docs/testing/001-test-plan.md)

## V1 Scope

Implemented:

- Back-office management APIs
- Mobile H5 activity, registration, check-in, volunteer, and survey APIs/pages
- Activity lifecycle management
- Activity process items and registration custom fields
- Audience registration and check-in
- Volunteer positions, applications, review, attendance, and service-hour statistics
- Visitor report management
- Survey configuration, submission, statistics, raw response list, and export
- Activity file/photo upload, preview, download, delete, and photo ZIP archive
- Dashboard and activity summary metrics
- Operation logs
- Admin account, role, and permission management
- UTF-8 CSV exports that Excel can open
- Docker Compose deployment baseline

Not included in V1:

- WeChat mini program
- Official account automatic publishing
- SMS verification
- Complex low-code form designer
- Object storage
- Automatic database backup
- Native `.xlsx` export
- Formal volunteer service certificate template
- Fixed Logo/header/seal export templates
- HTTPS/domain/CI/CD production setup

## Technical Direction

- Monorepo layout with separated frontend/backend units
- Admin web: Vue 3 + Element Plus baseline
- Mobile web: Vue 3 H5 baseline
- Backend: Spring Boot
- Persistence: MyBatis-Plus + MySQL 8
- Migrations: Flyway
- Auth: Spring Security + JWT + RBAC
- File storage: local server storage plus MySQL metadata
- Deployment: Docker Compose + MySQL + Spring Boot + Nginx

## Directory Layout

```text
apps/admin-web/   # Back-office frontend
apps/mobile-web/  # Mobile H5 frontend
server/           # Spring Boot API service
docs/             # Requirements, ADRs, API/database/test/delivery docs
deploy/nginx/     # Nginx deployment config
.scratch/issues/  # Local Markdown issue tracker
```

## Local Development

PowerShell may block `npm.ps1` on this machine, so use `npm.cmd`.

Install frontend dependencies:

```powershell
npm.cmd install
```

Run backend tests:

```powershell
mvn -f server/pom.xml test
```

Run admin web:

```powershell
npm.cmd run dev:admin
```

Open:

```text
http://localhost:5173/admin/
```

Run mobile H5:

```powershell
npm.cmd run dev:mobile
```

Open:

```text
http://localhost:5174/mobile/
```

Run backend:

```powershell
mvn -f server/pom.xml spring-boot:run
```

Health check:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

## Docker Compose Deployment

Copy `.env.example` to `.env` if local overrides are needed.

Build frontend assets:

```powershell
npm.cmd run build:admin
npm.cmd run build:mobile
```

Start services:

```powershell
docker compose up --build -d
```

Compose services:

- `mysql`: MySQL 8 database
- `server`: Spring Boot API, with local file storage mounted at `/app/storage`
- `nginx`: serves `/admin/`, `/mobile/`, and proxies `/api/`

Default Nginx entry:

```text
http://localhost:8088/admin/
http://localhost:8088/mobile/
http://localhost:8088/api/health
```

See [deployment guide](docs/deployment/001-docker-compose-deployment.md) for environment variables, ports, storage mount, startup order, Flyway, backup, and restore notes.

## Seeded Admin Accounts

Seeded accounts are created only when `admin_user` is empty.

Default password:

```text
password123
```

| Username | Role | Intended access |
| --- | --- | --- |
| `superadmin` | `SUPER_ADMIN` | Full access, account management, operation logs |
| `activityadmin` | `ACTIVITY_ADMIN` | Activities, registration, check-in, visitor reports, surveys, files |
| `volunteeradmin` | `VOLUNTEER_ADMIN` | Volunteer module |
| `disabledadmin` | `SUPER_ADMIN` | Disabled account for login rejection tests |

Change default passwords before real use.

## Important Agent Rule

For future agent work, read [AGENTS.md](AGENTS.md), [STATUS.md](STATUS.md), and [CONTEXT.md](CONTEXT.md) before changing requirements, issues, docs, or code.
