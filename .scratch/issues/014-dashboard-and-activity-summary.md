# Issue 014: Dashboard and activity summary
Status: `completed`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `docs/api/001-api-design.md`
- `docs/prototype/001-page-map.md`
- `docs/prototype/002-admin-wireframes.md`
- `docs/testing/001-test-plan.md`

## What to build

Implement activity detail summary cards and back-office dashboard metrics that summarize registration, check-in, volunteer, survey, and photo data.

## Acceptance criteria

- [x] Activity detail shows registered attendee count.
- [x] Activity detail shows actual checked-in count.
- [x] Activity detail shows check-in rate.
- [x] Activity detail shows volunteer application count.
- [x] Activity detail shows approved volunteer count.
- [x] Activity detail shows total volunteer service minutes.
- [x] Activity detail shows survey response count.
- [x] Activity detail shows average satisfaction rating.
- [x] Activity detail shows photo count.
- [x] Dashboard shows role-visible summary metrics.
- [x] Dashboard shows upcoming activities.
- [x] Dashboard shows pending volunteer application entry.
- [x] Metrics exclude cancelled registrations, revoked check-ins, revoked volunteer attendance, and deleted photos.
- [x] Tests cover activity summary and role visibility.

## Completion notes

- Completed on 2026-06-07.
- Added cross-module summary fields to admin activity detail responses.
- Added dashboard summary, upcoming activity, and pending volunteer application endpoints.
- Dashboard metrics are permission-aware and return zero or empty module data outside the current admin role's visible scope.
- Added integration tests for per-activity summary exclusions and dashboard role visibility.

## Blocked by

- `.scratch/issues/006-audience-registration-flow.md`
- `.scratch/issues/007-audience-check-in-flow.md`
- `.scratch/issues/009-volunteer-attendance-service-hours-flow.md`
- `.scratch/issues/011-activity-files-photos-archive-flow.md`
- `.scratch/issues/013-survey-response-statistics-export-flow.md`
