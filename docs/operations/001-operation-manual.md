# Operation Manual

## Scope

This manual covers the implemented V1 workflows. It does not describe mini programs, SMS verification, official account publishing, object storage, automatic backups, certificate templates, or native `.xlsx` export.

## Test Accounts

| Username | Password | Role | Access |
| --- | --- | --- | --- |
| `superadmin` | `password123` | `SUPER_ADMIN` | All admin modules, account management, operation logs, activity delete/unarchive |
| `activityadmin` | `password123` | `ACTIVITY_ADMIN` | Activities, registration, check-in, visitor reports, surveys, files |
| `volunteeradmin` | `password123` | `VOLUNTEER_ADMIN` | Volunteer positions, applications, attendance, and volunteer export |
| `disabledadmin` | `password123` | `SUPER_ADMIN` | Disabled login test account |

## Admin Login

1. Open `/admin/`.
2. Log in with a back-office account.
3. Use the returned JWT for protected API calls.
4. Disabled accounts cannot log in.

API:

```text
POST /api/admin/auth/login
GET  /api/admin/auth/me
```

## Activity Management

Use the admin activity APIs to create and manage activity lifecycle.

Lifecycle:

```text
DRAFT -> REGISTRATION_OPEN -> IN_PROGRESS -> ENDED -> ARCHIVED
```

Core actions:

- Create activity as draft.
- Edit activity while allowed by lifecycle rules.
- Publish registration.
- Start activity.
- End activity.
- Archive activity.
- Super admin can unarchive and delete.

APIs:

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

## Activity Process and Registration Fields

Activity process items define the public timeline. Custom fields define extra registration questions for one activity.

APIs:

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

## Audience Registration

Mobile users register from the public activity page. Admins can backfill or cancel registrations.

Rules:

- Public registration requires `REGISTRATION_OPEN`.
- Duplicate active phone for the same activity is rejected.
- Deadline and capacity are enforced.
- Cancelled registrations no longer consume capacity.

APIs:

```text
POST /api/mobile/activities/{activityId}/registrations
GET  /api/admin/activities/{activityId}/registrations
POST /api/admin/activities/{activityId}/registrations
POST /api/admin/registrations/{registrationId}/cancel
GET  /api/admin/activities/{activityId}/registrations/export
```

## Audience Check-In

Mobile users check in through the fixed activity check-in link or QR code. Admins can manually check in and revoke check-ins.

Rules:

- Public check-in requires `IN_PROGRESS`.
- Registration must exist and not be cancelled.
- Duplicate active check-in returns `ALREADY_CHECKED_IN`.
- Revoked check-ins are not counted as active.

APIs:

```text
POST /api/mobile/activities/{activityId}/check-ins
GET  /api/admin/activities/{activityId}/check-ins
POST /api/admin/activities/{activityId}/check-ins/manual
POST /api/admin/check-ins/{checkInId}/revoke
GET  /api/admin/activities/{activityId}/check-ins/export
```

## Volunteer Management

Volunteer admin manages activity positions, reviews applications, and tracks attendance/service hours.

Rules:

- A phone can apply to only one position in the same activity.
- Approved applications consume position capacity.
- Attendance is created on check-in, not approval.
- Service minutes default to check-out minus check-in.
- Admin can adjust service minutes and revoke attendance.

APIs:

```text
GET    /api/admin/activities/{activityId}/volunteer-positions
POST   /api/admin/activities/{activityId}/volunteer-positions
PUT    /api/admin/volunteer-positions/{positionId}
DELETE /api/admin/volunteer-positions/{positionId}
GET    /api/admin/volunteer-applications
POST   /api/admin/volunteer-applications/{applicationId}/approve
POST   /api/admin/volunteer-applications/{applicationId}/reject
POST   /api/admin/volunteer-applications/{applicationId}/cancel
GET    /api/admin/volunteer-applications/export
GET    /api/admin/volunteer-attendance
POST   /api/admin/activities/{activityId}/volunteer-attendance/manual-check-in
POST   /api/admin/volunteer-applications/{applicationId}/attendance/manual-check-out
POST   /api/admin/volunteer-attendance/{attendanceId}/adjust
POST   /api/admin/volunteer-attendance/{attendanceId}/revoke
```

Mobile volunteer APIs:

```text
GET  /api/mobile/activities/{activityId}/volunteer-positions
POST /api/mobile/activities/{activityId}/volunteer-applications
GET  /api/mobile/activities/{activityId}/volunteer-applications/status
POST /api/mobile/volunteer-applications/{applicationId}/check-in
POST /api/mobile/volunteer-applications/{applicationId}/check-out
```

## Visitor Reports

Visitor reports are independent records and can optionally link to an activity.

APIs:

```text
GET    /api/admin/visitor-reports
POST   /api/admin/visitor-reports
GET    /api/admin/visitor-reports/{visitorReportId}
PUT    /api/admin/visitor-reports/{visitorReportId}
DELETE /api/admin/visitor-reports/{visitorReportId}
GET    /api/admin/visitor-reports/export
```

## Surveys

Each activity can have one V1 survey. Admins configure questions and options; checked-in audience users submit responses on mobile.

Rules:

- One survey per activity.
- Question types: single choice, multiple choice, rating, text.
- Only checked-in registrations can submit.
- One response per survey and registration.
- Submitted surveys cannot be edited.

Admin APIs:

```text
GET    /api/admin/activities/{activityId}/survey
POST   /api/admin/activities/{activityId}/survey
PUT    /api/admin/surveys/{surveyId}
POST   /api/admin/surveys/{surveyId}/publish
POST   /api/admin/surveys/{surveyId}/close
POST   /api/admin/surveys/{surveyId}/questions
PUT    /api/admin/survey-questions/{questionId}
DELETE /api/admin/survey-questions/{questionId}
POST   /api/admin/survey-questions/{questionId}/options
PUT    /api/admin/survey-options/{optionId}
DELETE /api/admin/survey-options/{optionId}
GET    /api/admin/surveys/{surveyId}/statistics
GET    /api/admin/surveys/{surveyId}/responses
GET    /api/admin/surveys/{surveyId}/export
```

Mobile APIs:

```text
GET  /api/mobile/activities/{activityId}/survey/eligibility
GET  /api/mobile/activities/{activityId}/survey
POST /api/mobile/activities/{activityId}/survey/responses
```

## Files and Photos

Admins can upload covers, attachments, and photos. File bytes are stored in local storage; metadata is stored in MySQL.

Rules:

- Cover/photo types: JPG, JPEG, PNG, WEBP up to 10 MB.
- Attachment types: PDF, Word, Excel, PPT, and images up to 20 MB.
- Deleted files are hidden from normal lists.
- Photo ZIP includes only non-deleted photos.

APIs:

```text
GET    /api/admin/activities/{activityId}/files
POST   /api/admin/activities/{activityId}/files
GET    /api/admin/files/{fileId}/preview
GET    /api/admin/files/{fileId}/download
DELETE /api/admin/files/{fileId}
GET    /api/admin/activities/{activityId}/photos.zip
```

## Dashboard and Activity Summary

Activity details include summary metrics for registration, check-in, volunteers, surveys, and photos.

Dashboard APIs:

```text
GET /api/admin/dashboard/summary
GET /api/admin/dashboard/upcoming-activities
GET /api/admin/dashboard/pending-volunteer-applications
```

Dashboard metric visibility follows the current admin role permissions.

## Operation Logs

Super admin can view append-only operation logs. Activity admin and volunteer admin cannot view them.

Logged actions include activity lifecycle, exports, registration backfill/cancel, check-in backfill/revoke, volunteer review/attendance, and file deletion.

APIs:

```text
GET /api/admin/operation-logs
GET /api/admin/operation-logs/{logId}
```

There is no operation log delete API in V1.

## Account Management

Super admin can create and manage admin accounts.

Supported operations:

- List and filter by keyword, status, and role.
- Create account.
- Edit display name, phone, and status.
- Enable or disable by status.
- Reset password.
- Assign roles.
- View roles and permissions.

APIs:

```text
GET  /api/admin/users
POST /api/admin/users
GET  /api/admin/users/{adminUserId}
PUT  /api/admin/users/{adminUserId}
POST /api/admin/users/{adminUserId}/reset-password
PUT  /api/admin/users/{adminUserId}/roles
GET  /api/admin/roles
GET  /api/admin/permissions
```

Account deletion is not included in V1.

## Exports

Export endpoints return UTF-8 CSV files that Excel can open.

Implemented exports:

- Registrations
- Check-ins
- Volunteer applications
- Visitor reports
- Survey responses
- Photo ZIP archive

Native `.xlsx` exports can be added later if delivery requires it.
