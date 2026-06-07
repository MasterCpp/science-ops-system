# Docker Compose Deployment Guide

## Scope

This guide covers the V1 delivery baseline:

- MySQL 8 database
- Spring Boot backend API
- Nginx serving built admin/mobile frontend assets and proxying `/api`
- Local server file storage mounted outside the backend image

HTTPS certificates, production domain configuration, CI/CD, and automatic backups are outside V1.

## Prerequisites

- Docker and Docker Compose are installed on the deployment host.
- TCP ports for MySQL, backend, and Nginx are available.
- Frontend assets have been built before starting Nginx static routes.

## Environment Variables

Copy `.env.example` to `.env` and adjust values for the deployment environment.

| Variable | Default | Purpose |
| --- | --- | --- |
| `MYSQL_DATABASE` | `science_ops` | MySQL database name |
| `MYSQL_USER` | `science_ops` | MySQL application user |
| `MYSQL_PASSWORD` | `science_ops_dev` | MySQL application password |
| `MYSQL_ROOT_PASSWORD` | `root_dev` | MySQL root password |
| `MYSQL_PORT` | `3306` | Host port mapped to MySQL |
| `SERVER_PORT` | `8080` | Host port mapped to Spring Boot |
| `NGINX_PORT` | `8088` | Host port mapped to Nginx |
| `LOCAL_FILE_STORAGE_PATH` | `./storage` | Host directory mounted to backend file storage |
| `JWT_SECRET` | dev default in `application.yml` | JWT signing secret; set a strong production value |
| `JWT_TTL_MINUTES` | `120` | JWT token lifetime |

The backend container uses:

```text
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/${MYSQL_DATABASE}
SPRING_DATASOURCE_USERNAME=${MYSQL_USER}
SPRING_DATASOURCE_PASSWORD=${MYSQL_PASSWORD}
LOCAL_FILE_STORAGE_PATH=/app/storage
```

## Ports

| Service | Container port | Host port |
| --- | --- | --- |
| MySQL | `3306` | `${MYSQL_PORT:-3306}` |
| Spring Boot API | `8080` | `${SERVER_PORT:-8080}` |
| Nginx | `80` | `${NGINX_PORT:-8088}` |

End-user entry points after Compose startup:

```text
http://<host>:<NGINX_PORT>/admin/
http://<host>:<NGINX_PORT>/mobile/
http://<host>:<NGINX_PORT>/api/health
```

## Startup Order

1. Build frontend assets.
2. Start MySQL.
3. Start Spring Boot after MySQL passes its health check.
4. Start Nginx after the backend service is available.

Docker Compose models this with `depends_on` and the MySQL health check.

## Build Frontend Assets

From the repository root:

```powershell
npm.cmd install
npm.cmd run build:admin
npm.cmd run build:mobile
```

Nginx serves:

```text
apps/admin-web/dist -> /admin/
apps/mobile-web/dist -> /mobile/
```

## Start Services

From the repository root:

```powershell
docker compose up --build -d
```

Check service status:

```powershell
docker compose ps
```

Check backend health through Nginx:

```powershell
Invoke-RestMethod http://localhost:8088/api/health
```

View logs:

```powershell
docker compose logs mysql
docker compose logs server
docker compose logs nginx
```

Stop services:

```powershell
docker compose down
```

## Flyway Migration

Flyway is enabled in `server/src/main/resources/application.yml`:

```text
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

Migrations run automatically when the Spring Boot backend starts. The current schema baseline is:

```text
server/src/main/resources/db/migration/V1__init_schema.sql
```

Deployment check:

1. Start Compose.
2. Confirm backend startup succeeds.
3. Confirm `flyway_schema_history` exists in MySQL.
4. Confirm `/api/health` returns success.

Do not edit an already-applied Flyway migration in a deployed environment. Add a new versioned migration instead.

## File Storage Mount

Uploaded files are stored under the backend setting:

```text
science-ops.storage.local-path=${LOCAL_FILE_STORAGE_PATH:./storage}
```

In Compose, the host path `${LOCAL_FILE_STORAGE_PATH:-./storage}` is mounted into the backend container at:

```text
/app/storage
```

Backup and restore must treat this directory as production data. If the storage directory is lost, file metadata may remain in MySQL but previews/downloads will fail.

## Backup

Back up both MySQL data and local file storage.

MySQL dump example:

```powershell
docker compose exec mysql mysqldump -u root -p science_ops > backup-science-ops.sql
```

File storage backup example:

```powershell
Compress-Archive -Path .\storage\* -DestinationPath .\backup-storage.zip
```

Recommended backup set:

- MySQL dump
- `LOCAL_FILE_STORAGE_PATH` directory
- `.env` values used for deployment
- Git commit hash of the deployed version

## Restore

1. Stop application writes.
2. Restore MySQL from the dump.
3. Restore the file storage directory to `LOCAL_FILE_STORAGE_PATH`.
4. Start Compose.
5. Verify `/api/health`, admin login, file preview/download, and a representative export.

## Default Test Accounts

Seeded accounts are created only when `admin_user` is empty.

| Username | Password | Role |
| --- | --- | --- |
| `superadmin` | `password123` | `SUPER_ADMIN` |
| `activityadmin` | `password123` | `ACTIVITY_ADMIN` |
| `volunteeradmin` | `password123` | `VOLUNTEER_ADMIN` |
| `disabledadmin` | `password123` | disabled `SUPER_ADMIN` |

Change seeded passwords before real use.
