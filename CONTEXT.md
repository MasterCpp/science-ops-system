# Science Operations System Context

## Project Goal

Build a science activity operations management system for managing science outreach activities, registrations, check-ins, volunteer work, visitor reports, surveys, photos, exports, and activity archives.

The project is also a learning project. The goal is to experience a realistic enterprise-style development flow from zero: requirement clarification, PRD, architecture decisions, database design, API design, issue breakdown, implementation, testing, deployment documentation, and handoff.

## Version 1 Boundary

Version 1 follows the standard custom Web system plan, referred to as "方案 B".

The first version includes:

- Back-office activity management
- Mobile H5 registration and check-in pages
- Per-activity registration links and check-in QR codes
- Audience registration, check-in, and data export
- Volunteer position publishing, registration, review, check-in, check-out, and service-hour statistics
- Visitor report management
- Activity plan and process management
- Satisfaction survey configuration and statistics
- Activity photo upload and archive
- Registration, check-in, volunteer, survey, and activity summary exports
- Source code, database structure, deployment documentation, operation instructions, and test account handoff

The first version explicitly does not include:

- WeChat mini program
- Official account automatic publishing
- Forced private cloud binding
- SMS verification unless later added by requirement
- Complex low-code form designer

## Default Technical Direction

- Back-office frontend: Vue 3 + Element Plus
- Mobile frontend: Vue 3 H5 or React H5
- Backend: Spring Boot
- Database: MySQL 8
- File storage: server local storage by default
- Deployment: Docker Compose + MySQL + Spring Boot + Nginx
- Future extension targets: mini program, official account notifications, SMS reminders, object storage

Backend, repository layout, persistence, authentication/authorization, file storage, and deployment baseline have been locked through ADRs under `docs/adr/`.

## Confirmed Business Defaults

- Back-office roles: super admin, activity admin, volunteer admin
- Mobile side: no account login in v1; users enter through activity links or QR codes
- Activity lifecycle: draft, registration open, in progress, ended, archived
- Audience registration: submit means success
- Check-in QR code: fixed QR code per activity
- Volunteer flow: volunteer applies for a position, admin reviews, service hours are calculated from check-in/check-out time
- Registration form: fixed base fields plus a small number of custom fields
- Survey: custom questions are supported
- Export: Excel exports plus a simple activity summary data page
- Visitor report: independent visitor report module
- Activity plan/process: rich text plus attachments; process uses timeline entries
- Photo storage: local server storage, archived by activity

## Requirement Status

The main V1 requirement decisions have been confirmed in `docs/requirements/需求澄清.md`.

The first PRD is available at `docs/prd/001-科普运营系统-v1.md`.

Before implementation, the project still needs database design, API design, test plan, deployment documents, and implementation issue breakdown.

## Project Memory Rule

Future conversations should treat repository documents as the source of truth. If chat history conflicts with repository documents, update the relevant document after confirming the change.
