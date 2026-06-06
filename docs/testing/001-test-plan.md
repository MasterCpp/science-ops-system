# Test Plan V1

Status: Draft

Date: 2026-06-05

## Purpose

This document defines the V1 testing plan before implementation issue breakdown.

It is based on:

- `docs/prd/001-科普运营系统-v1.md`
- `docs/database/001-er-design.md`
- `docs/prototype/001-page-map.md`
- `docs/prototype/002-admin-wireframes.md`
- `docs/prototype/003-mobile-wireframes.md`
- `docs/api/001-api-design.md`

Testing should focus on externally visible behavior and business rules, not implementation details.

## Test Layers

### Backend API and Service Tests

Primary coverage should be backend service/API behavior:

- Request validation
- Permission enforcement
- State transitions
- Database persistence effects
- Business-rule failures
- Export output shape
- Operation log creation

### Frontend Acceptance Scenarios

Frontend tests should verify critical user journeys:

- Page renders required data
- Form validation and submission behavior
- Role-aware action visibility
- Correct handling of API success and failure states

### Manual Verification

Manual verification is acceptable for early V1 planning and should cover:

- File preview/download
- Excel export downloads
- Photo ZIP download
- Mobile H5 flows on narrow viewport

## Common Acceptance Rules

- API errors use the common response envelope from `docs/api/001-api-design.md`.
- IDs are returned as JSON strings.
- List endpoints that are not reference lists support pagination.
- Protected admin endpoints reject unauthenticated requests.
- Protected admin endpoints reject roles without permission.
- Public mobile endpoints do not require JWT but enforce business rules.
- State-changing admin operations create operation logs when listed in requirements.

## Admin Auth and RBAC

Test scenarios:

1. Login succeeds with valid username and password.
2. Login fails with invalid password.
3. Disabled admin cannot log in.
4. `/api/admin/auth/me` returns current user, roles, and permissions.
5. Admin endpoints reject missing JWT.
6. Admin endpoints reject invalid JWT.
7. Super admin can manage accounts.
8. Activity admin cannot manage accounts.
9. Volunteer admin cannot manage accounts.
10. Volunteer admin cannot manage registration, check-in, survey, visitor, file, or account operations outside confirmed permissions.
11. Super admin can view operation logs.
12. Activity admin and volunteer admin cannot view operation logs.

## Activity Lifecycle

Test scenarios:

1. Activity can be created as draft with standard fields.
2. Draft activity allows all fields to be edited.
3. Draft activity can be published to registration open.
4. Registration-open activity can move to in progress.
5. In-progress activity can move to ended.
6. Ended activity can move to archived.
7. Archived activity is read-only by default.
8. Super admin can unarchive an archived activity.
9. Activity admin cannot delete an activity.
10. Super admin can delete an activity.
11. Editing registration fields, capacity, or deadline while registration is open creates an operation log.
12. In-progress activity rejects registration field and capacity edits.
13. Ended activity only allows photos, attachments, and summary-related updates.

## Activity Process and Custom Fields

Test scenarios:

1. Activity process items can be added, edited, sorted, and deleted.
2. Activity custom fields can be added, edited, sorted, and deleted before registration-sensitive locking applies.
3. Custom field keys are unique within one activity.
4. Custom field definitions are returned by the public activity detail endpoint.
5. Registration custom field values are saved with field key and label snapshots.

## Audience Registration

Test scenarios:

1. Public activity detail shows registration availability.
2. Registration succeeds when activity is registration open, before deadline, and capacity is available.
3. Registration fails after deadline with `DEADLINE_PASSED`.
4. Registration fails when capacity would be exceeded with `CAPACITY_FULL`.
5. Duplicate phone for the same activity fails with `DUPLICATE_SUBMISSION`.
6. Same phone can register for different activities.
7. Capacity calculation uses non-cancelled `attendee_count`.
8. Admin can backfill registration after deadline.
9. Admin backfill still enforces duplicate phone.
10. Admin can cancel registration.
11. Cancelled registration no longer consumes capacity.
12. Registration export includes confirmed columns.

## Audience Check-In

Test scenarios:

1. Public check-in succeeds for registered, non-cancelled attendee while activity is in progress.
2. Public check-in fails when registration is not found.
3. Public check-in fails for cancelled registration.
4. Public check-in fails when activity is not in progress.
5. Duplicate active check-in returns `ALREADY_CHECKED_IN`.
6. Admin can manually check in a registration.
7. Manual check-in records method/manual fields.
8. Admin can revoke check-in.
9. Revoked check-in is not counted as active check-in.
10. Check-in export includes confirmed columns.

## Volunteer Positions

Test scenarios:

1. Permitted admin can create volunteer position.
2. Position requires name, capacity, service start time, and service end time.
3. Position approved count is calculated from approved applications only.
4. Position list is visible in mobile H5.
5. Full positions are marked unavailable in mobile H5.

## Volunteer Applications

Test scenarios:

1. Public volunteer application succeeds for available position.
2. Same phone cannot apply to multiple positions in the same activity.
3. Application fails for full position when approved count reaches capacity.
4. New application starts as `PENDING`.
5. Permitted admin can approve application.
6. Approving application fails with `CAPACITY_FULL` if capacity has been reached.
7. Permitted admin can reject application with review note.
8. Permitted admin can cancel application.
9. Volunteer application export includes confirmed columns.

## Volunteer Attendance

Test scenarios:

1. Volunteer check-in fails before approval with `NOT_APPROVED`.
2. Approved volunteer can check in.
3. Volunteer attendance record is created on check-in, not approval.
4. Checked-in volunteer can check out.
5. Service minutes default to check-out time minus check-in time.
6. Duplicate check-in is rejected.
7. Duplicate check-out is rejected.
8. Admin can manually check in volunteer.
9. Admin can manually check out volunteer.
10. Admin can adjust service minutes with reason.
11. Admin can revoke volunteer attendance.
12. Revoked attendance is not counted as active service.

## Visitor Reports

Test scenarios:

1. Activity admin can create visitor report.
2. Visitor report may be linked to an activity.
3. Visitor report may be created without linked activity.
4. Visitor report can be edited and deleted by permitted roles.
5. Volunteer admin cannot manage visitor reports.
6. Visitor report export includes confirmed columns.

## Survey

Test scenarios:

1. Activity can have only one V1 survey.
2. Survey supports single choice, multiple choice, rating, and text questions.
3. Single/multiple choice questions support options.
4. Survey can be published.
5. Survey can be closed.
6. Public survey eligibility fails when phone has no registration.
7. Public survey eligibility fails when registration is not checked in.
8. Public survey eligibility fails when survey is not published.
9. Public survey submission succeeds for checked-in registration.
10. Duplicate survey response fails with `DUPLICATE_SUBMISSION`.
11. Submitted survey cannot be edited in V1.
12. Survey statistics include response count and average rating.
13. Survey export includes confirmed columns.

## Files and Photos

Test scenarios:

1. Cover upload accepts supported image types.
2. Attachment upload accepts PDF, Word, Excel, PPT, and images up to 20MB.
3. Photo upload accepts JPG, JPEG, PNG, and WEBP up to 10MB.
4. Unsupported file type returns `UNSUPPORTED_FILE_TYPE`.
5. Oversized file returns `FILE_TOO_LARGE`.
6. File metadata is saved in `file_asset`.
7. Activity cover can be referenced by `activity.cover_file_id`.
8. Admin can preview supported photo files.
9. Admin can download files.
10. Admin can delete files.
11. Deleted files are not shown in normal file lists.
12. Activity photo ZIP contains non-deleted photos.

## Exports

Test scenarios:

1. Registration export applies the same filters and permissions as registration list.
2. Check-in export applies the same filters and permissions as check-in list.
3. Volunteer export applies volunteer permissions.
4. Visitor report export applies visitor-report permissions.
5. Survey export applies survey permissions.
6. Export actions create operation logs.
7. Exported columns match confirmed PRD and requirement documents.

## Operation Logs

Test scenarios:

1. Successful admin login creates an operation log if enabled for login.
2. Activity create/update/delete creates logs.
3. Activity publish/start/end/archive creates logs.
4. Export actions create logs.
5. Registration backfill/cancel creates logs.
6. Check-in backfill/revoke creates logs.
7. Volunteer review creates logs.
8. Photo/attachment delete creates logs.
9. Log list supports filters by admin, action, target type, and time range.
10. Operation logs cannot be deleted in V1.

## Frontend Acceptance Scenarios

### Admin Web

1. Login page shows username/password fields and handles failed login.
2. Sidebar changes according to role permissions.
3. Activity list supports keyword, status, and time filters.
4. Activity create/edit page shows all confirmed fields.
5. Activity detail shows summary cards and related tabs.
6. Registration page supports backfill, cancel, and export for permitted roles.
7. Check-in page supports manual check-in, revoke, and export for permitted roles.
8. Volunteer pages support positions, applications, attendance, and export.
9. Survey editor supports question and option management.
10. File page supports upload, preview, delete, and ZIP download.
11. Operation log page is visible only to super admin.

### Mobile H5

1. Activity detail renders on mobile viewport.
2. Registration form handles open, full, deadline-passed, and already-registered states.
3. Check-in page handles success, already checked in, not found, and invalid activity state.
4. Volunteer position list shows availability.
5. Volunteer application handles success, duplicate application, and full position states.
6. Volunteer attendance page handles pending, rejected, approved, checked-in, checked-out, and revoked states.
7. Survey page handles not checked in, already submitted, closed, and submit success states.

## Not Covered in V1

- WeChat mini program tests.
- Official account publishing tests.
- SMS verification tests.
- Object storage tests.
- Phone masking tests.
- Automatic database backup tests.
- Volunteer service certificate template tests.
- Fixed Logo/header/seal export template tests.

## Next Step

Use this test plan to split local Markdown implementation issues under `.scratch/issues/`.

Each implementation issue should include:

- Related PRD/API/database/prototype documents.
- Scope.
- Acceptance criteria copied or adapted from this test plan.
- Dependencies.

