# Issue 007: Audience Check-In Flow

Status: `completed`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `docs/api/001-api-design.md`
- `docs/prototype/002-admin-wireframes.md`
- `docs/prototype/003-mobile-wireframes.md`
- `docs/testing/001-test-plan.md`

## What to build

Implement the fixed audience check-in entrance, public mobile check-in, admin check-in list, manual backfill, revoke, and check-in export.

## Acceptance criteria

- [x] Activity detail provides a fixed check-in link.
- [x] Registered and not-cancelled audience can check in by phone while the activity is `IN_PROGRESS`.
- [x] Successful check-in displays name and check-in time.
- [x] Duplicate check-in does not create a new active record and returns `ALREADY_CHECKED_IN`.
- [x] Unregistered phone check-in fails.
- [x] Cancelled registration phone check-in fails.
- [x] Activity not started or ended returns `INVALID_STATE`.
- [x] Admin can view the check-in list.
- [x] Admin can manually check in.
- [x] Admin can revoke check-in.
- [x] Revoked check-ins are not counted as active check-ins.
- [x] Check-in Excel-openable export contains confirmed columns.
- [x] Tests cover success, duplicate, unregistered, cancelled registration, invalid activity state, manual check-in, revoke, and export.

## Implemented notes

- Public API: `POST /api/mobile/activities/{activityId}/check-ins`.
- Admin APIs:
  - `GET /api/admin/activities/{activityId}/check-ins`
  - `POST /api/admin/activities/{activityId}/check-ins/manual`
  - `POST /api/admin/check-ins/{checkInId}/revoke`
  - `GET /api/admin/activities/{activityId}/check-ins/export`
- Mobile activity detail now includes `registrationLink` and `checkInLink`.
- The mobile H5 app supports check-in mode at `/m/activities/{activityId}/check-in`.
- Export is a UTF-8 CSV file that Excel can open, matching the Issue 006 registration export baseline.

## Verification

- `mvn -q -f C:\Users\L\Desktop\66\science-ops-system\server\pom.xml test`
- `npm.cmd run build:mobile`

## Blocked by

- `.scratch/issues/006-audience-registration-flow.md`
