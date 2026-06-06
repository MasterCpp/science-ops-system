# API Design V1

Status: Draft

Date: 2026-06-05

## Purpose

This document defines the first-pass API design for the Science Operations System V1.

It is based on:

- `docs/prd/001-科普运营系统-v1.md`
- `docs/database/001-er-design.md`
- `docs/prototype/001-page-map.md`
- `docs/prototype/002-admin-wireframes.md`
- `docs/prototype/003-mobile-wireframes.md`
- `docs/adr/004-authentication-and-authorization.md`

This is an API planning document, not an OpenAPI specification.

## API Groups

### Protected Admin APIs

Base path:

```text
/api/admin
```

Rules:

- Require JWT unless explicitly stated otherwise.
- Enforce RBAC permissions from the confirmed role matrix.
- Return only data allowed for the authenticated admin role.
- State-changing operations that are audit-relevant must create operation logs.

### Public Mobile APIs

Base path:

```text
/api/mobile
```

Rules:

- Do not require JWT in V1.
- Validate business rules because there is no mobile login identity.
- Do not expose admin-only fields.
- Use activity state, phone, registration, volunteer application, and check-in status to determine eligibility.

## Common Conventions

### Response Envelope

Success:

```json
{
  "success": true,
  "data": {},
  "message": "OK"
}
```

Failure:

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Human-readable error message",
  "details": {}
}
```

### Pagination

Request query parameters:

```text
page=1
pageSize=20
```

Paged response:

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0
}
```

### Common Error Codes

- `UNAUTHORIZED`
- `FORBIDDEN`
- `VALIDATION_ERROR`
- `NOT_FOUND`
- `CONFLICT`
- `INVALID_STATE`
- `CAPACITY_FULL`
- `DEADLINE_PASSED`
- `DUPLICATE_SUBMISSION`
- `ALREADY_CHECKED_IN`
- `NOT_CHECKED_IN`
- `NOT_APPROVED`
- `FILE_TOO_LARGE`
- `UNSUPPORTED_FILE_TYPE`

### ID Format

IDs are returned as strings in JSON to avoid JavaScript precision loss.

Example:

```json
{
  "id": "1930240012345671680"
}
```

Backend entities still use `BIGINT` snowflake IDs.

## Admin Auth APIs

### `POST /api/admin/auth/login`

Login with username and password.

Request:

```json
{
  "username": "admin",
  "password": "password"
}
```

Response data:

```json
{
  "token": "jwt",
  "admin": {
    "id": "1",
    "username": "admin",
    "displayName": "Admin",
    "roles": ["SUPER_ADMIN"],
    "permissions": ["activity:create"]
  }
}
```

### `GET /api/admin/auth/me`

Return current admin profile, roles, and permissions.

### `POST /api/admin/auth/logout`

Client-side token removal is enough for V1. Endpoint may exist for audit consistency.

## Admin Dashboard APIs

### `GET /api/admin/dashboard/summary`

Returns role-aware dashboard metrics.

Response data:

```json
{
  "activityCountByStatus": {},
  "registrationCount": 0,
  "checkInCount": 0,
  "volunteerApplicationCount": 0,
  "approvedVolunteerCount": 0,
  "totalServiceMinutes": 0,
  "surveyResponseCount": 0,
  "photoCount": 0
}
```

### `GET /api/admin/dashboard/upcoming-activities`

Returns upcoming activities for dashboard table.

### `GET /api/admin/dashboard/pending-volunteer-applications`

Returns pending volunteer counts grouped by activity and position.

## Admin Activity APIs

### `GET /api/admin/activities`

List activities.

Query:

- `keyword`
- `status`
- `startFrom`
- `startTo`
- `page`
- `pageSize`

List item fields:

- `id`
- `title`
- `status`
- `startTime`
- `endTime`
- `location`
- `capacity`
- `registeredAttendeeCount`
- `checkedInCount`
- `ownerName`

### `POST /api/admin/activities`

Create activity.

Request includes:

- Basic activity fields
- `processItems`
- `customFields`

### `GET /api/admin/activities/{activityId}`

Return activity detail, process items, custom fields, summary metrics, file references, registration link, and check-in QR metadata.

### `PUT /api/admin/activities/{activityId}`

Update activity according to state-based edit rules.

### `DELETE /api/admin/activities/{activityId}`

Delete activity. Super admin only.

### `POST /api/admin/activities/{activityId}/publish`

Move draft activity to registration open.

### `POST /api/admin/activities/{activityId}/start`

Move registration-open activity to in progress.

### `POST /api/admin/activities/{activityId}/end`

Move in-progress activity to ended.

### `POST /api/admin/activities/{activityId}/archive`

Archive ended activity.

### `POST /api/admin/activities/{activityId}/unarchive`

Super admin restores archived activity to editable state.

## Admin Registration APIs

### `GET /api/admin/activities/{activityId}/registrations`

List registrations for one activity.

Query:

- `keyword`
- `status`
- `createdFrom`
- `createdTo`
- `page`
- `pageSize`

### `POST /api/admin/activities/{activityId}/registrations`

Backfill registration.

Rules:

- Enforce duplicate phone constraint.
- Enforce activity capacity unless later changed.
- Can be used after deadline by permitted admins.

### `POST /api/admin/registrations/{registrationId}/cancel`

Cancel registration by changing status to `CANCELLED`.

### `GET /api/admin/activities/{activityId}/registrations/export`

Export registration Excel.

## Admin Check-In APIs

### `GET /api/admin/activities/{activityId}/check-ins`

List check-in records.

Query:

- `keyword`
- `status`
- `checkedFrom`
- `checkedTo`
- `page`
- `pageSize`

### `POST /api/admin/activities/{activityId}/check-ins/manual`

Manual check-in.

Request:

```json
{
  "registrationId": "1930240012345671680",
  "checkInTime": "2026-06-05T14:00:00"
}
```

### `POST /api/admin/check-ins/{checkInId}/revoke`

Revoke check-in by changing status to `REVOKED`.

### `GET /api/admin/activities/{activityId}/check-ins/export`

Export check-in Excel.

## Admin Volunteer APIs

### Position APIs

#### `GET /api/admin/activities/{activityId}/volunteer-positions`

List volunteer positions with approved counts.

#### `POST /api/admin/activities/{activityId}/volunteer-positions`

Create volunteer position.

#### `PUT /api/admin/volunteer-positions/{positionId}`

Update volunteer position.

#### `DELETE /api/admin/volunteer-positions/{positionId}`

Delete volunteer position.

### Application APIs

#### `GET /api/admin/volunteer-applications`

List volunteer applications.

Query:

- `activityId`
- `positionId`
- `status`
- `keyword`
- `page`
- `pageSize`

#### `POST /api/admin/volunteer-applications/{applicationId}/approve`

Approve volunteer application.

Rules:

- Approved applications occupy position capacity.
- Return `CAPACITY_FULL` when approved count reaches capacity.

#### `POST /api/admin/volunteer-applications/{applicationId}/reject`

Reject volunteer application.

Request may include `reviewNote`.

#### `POST /api/admin/volunteer-applications/{applicationId}/cancel`

Cancel volunteer application.

#### `GET /api/admin/volunteer-applications/export`

Export volunteer data.

### Attendance APIs

#### `GET /api/admin/volunteer-attendance`

List volunteer attendance records.

Query:

- `activityId`
- `positionId`
- `status`
- `keyword`
- `page`
- `pageSize`

#### `POST /api/admin/volunteer-applications/{applicationId}/attendance/manual-check-in`

Manual volunteer check-in.

#### `POST /api/admin/volunteer-applications/{applicationId}/attendance/manual-check-out`

Manual volunteer check-out.

#### `POST /api/admin/volunteer-attendance/{attendanceId}/adjust`

Adjust service minutes.

Request:

```json
{
  "serviceMinutes": 120,
  "reason": "Manual correction"
}
```

#### `POST /api/admin/volunteer-attendance/{attendanceId}/revoke`

Revoke volunteer attendance.

## Admin Visitor Report APIs

### `GET /api/admin/visitor-reports`

List visitor reports.

Query:

- `keyword`
- `activityId`
- `visitFrom`
- `visitTo`
- `page`
- `pageSize`

### `POST /api/admin/visitor-reports`

Create visitor report.

### `GET /api/admin/visitor-reports/{visitorReportId}`

Get visitor report detail.

### `PUT /api/admin/visitor-reports/{visitorReportId}`

Update visitor report.

### `DELETE /api/admin/visitor-reports/{visitorReportId}`

Delete visitor report.

### `GET /api/admin/visitor-reports/export`

Export visitor report Excel.

## Admin Survey APIs

### `GET /api/admin/activities/{activityId}/survey`

Get survey for one activity.

### `POST /api/admin/activities/{activityId}/survey`

Create survey.

Rules:

- One survey per activity.

### `PUT /api/admin/surveys/{surveyId}`

Update survey title and description.

### `POST /api/admin/surveys/{surveyId}/publish`

Publish survey.

### `POST /api/admin/surveys/{surveyId}/close`

Close survey.

### `POST /api/admin/surveys/{surveyId}/questions`

Create question.

### `PUT /api/admin/survey-questions/{questionId}`

Update question.

### `DELETE /api/admin/survey-questions/{questionId}`

Delete question.

### `POST /api/admin/survey-questions/{questionId}/options`

Create question option.

### `PUT /api/admin/survey-options/{optionId}`

Update question option.

### `DELETE /api/admin/survey-options/{optionId}`

Delete question option.

### `GET /api/admin/surveys/{surveyId}/statistics`

Get survey statistics.

### `GET /api/admin/surveys/{surveyId}/responses`

List raw survey responses.

### `GET /api/admin/surveys/{surveyId}/export`

Export survey result Excel.

## Admin File APIs

### `GET /api/admin/activities/{activityId}/files`

List files for activity.

Query:

- `category`
- `keyword`
- `page`
- `pageSize`

### `POST /api/admin/activities/{activityId}/files`

Upload attachment or photo.

Request:

- Multipart file
- `category`: `COVER`, `ATTACHMENT`, `PHOTO`

Rules:

- Validate type and size by category.
- If category is `COVER`, response can be used as `activity.coverFileId`.

### `GET /api/admin/files/{fileId}/preview`

Preview file when supported.

### `GET /api/admin/files/{fileId}/download`

Download file.

### `DELETE /api/admin/files/{fileId}`

Delete file metadata and file bytes according to file-storage ADR.

### `GET /api/admin/activities/{activityId}/photos.zip`

Download all non-deleted photos for an activity as ZIP.

## Admin Operation Log APIs

### `GET /api/admin/operation-logs`

List operation logs.

Query:

- `adminUserId`
- `action`
- `targetType`
- `createdFrom`
- `createdTo`
- `page`
- `pageSize`

### `GET /api/admin/operation-logs/{logId}`

Get operation log detail.

No delete API in V1.

## Admin Account and RBAC APIs

### `GET /api/admin/users`

List admin users.

### `POST /api/admin/users`

Create admin user.

### `GET /api/admin/users/{adminUserId}`

Get admin user detail.

### `PUT /api/admin/users/{adminUserId}`

Update admin user profile and status.

### `POST /api/admin/users/{adminUserId}/reset-password`

Reset admin password.

### `PUT /api/admin/users/{adminUserId}/roles`

Assign roles to admin user.

### `GET /api/admin/roles`

List roles.

### `GET /api/admin/permissions`

List permissions.

## Public Mobile Activity APIs

### `GET /api/mobile/activities/{activityId}`

Get public activity detail.

Response data includes:

- Activity public fields
- Cover URL
- Remaining capacity
- Registration availability state
- Registration custom fields
- Volunteer entry availability
- Survey entry availability, if relevant

### `POST /api/mobile/activities/{activityId}/registrations`

Submit audience registration.

Request:

```json
{
  "name": "张三",
  "phone": "13800000000",
  "attendeeCount": 1,
  "unitName": "某学校",
  "ageGroup": "ADULT",
  "remark": "",
  "customValues": [
    {
      "fieldKey": "grade",
      "value": "三年级"
    }
  ]
}
```

Rules:

- Activity must be registration open.
- Registration deadline must not have passed.
- Capacity must be available.
- Same phone can register only once per activity.

## Public Mobile Check-In APIs

### `POST /api/mobile/activities/{activityId}/check-ins`

Audience check-in.

Request:

```json
{
  "phone": "13800000000"
}
```

Rules:

- Activity must be in progress.
- Registration must exist and not be cancelled.
- Active check-in must not already exist.

Response should distinguish:

- Success
- Already checked in
- Registration not found
- Invalid activity state
- Registration cancelled

## Public Mobile Volunteer APIs

### `GET /api/mobile/activities/{activityId}/volunteer-positions`

List public volunteer positions.

Response data includes capacity and approved count.

### `POST /api/mobile/activities/{activityId}/volunteer-applications`

Submit volunteer application.

Rules:

- Same phone can apply to only one position in the same activity.
- Position must not be full based on approved count.

### `GET /api/mobile/activities/{activityId}/volunteer-applications/status`

Lookup volunteer application and attendance status by phone.

Query:

- `phone`

### `POST /api/mobile/volunteer-applications/{applicationId}/check-in`

Volunteer check-in.

Rules:

- Application must be approved.
- Attendance must not already be checked in or checked out.

### `POST /api/mobile/volunteer-applications/{applicationId}/check-out`

Volunteer check-out.

Rules:

- Application must be approved.
- Attendance must be checked in.
- Attendance must not already be checked out.

## Public Mobile Survey APIs

### `GET /api/mobile/activities/{activityId}/survey/eligibility`

Validate whether a phone can answer the survey.

Query:

- `phone`

Rules:

- Registration must exist.
- Registration must be checked in.
- Survey must be published.
- Response must not already exist.

### `GET /api/mobile/activities/{activityId}/survey`

Get public survey detail after eligibility validation.

Query:

- `phone`

### `POST /api/mobile/activities/{activityId}/survey/responses`

Submit survey response.

Request:

```json
{
  "phone": "13800000000",
  "answers": [
    {
      "questionId": "1930240012345671680",
      "optionId": "1930240012345671681"
    },
    {
      "questionId": "1930240012345671682",
      "optionIds": ["1930240012345671683", "1930240012345671684"]
    },
    {
      "questionId": "1930240012345671685",
      "numericValue": 5
    },
    {
      "questionId": "1930240012345671686",
      "textValue": "活动很好"
    }
  ]
}
```

Rules:

- Same eligibility rules as survey eligibility.
- One response per survey and registration.
- Submitted answers cannot be edited in V1.

## Export APIs

Exports return downloadable files.

Admin export endpoints:

- `GET /api/admin/activities/{activityId}/registrations/export`
- `GET /api/admin/activities/{activityId}/check-ins/export`
- `GET /api/admin/volunteer-applications/export`
- `GET /api/admin/visitor-reports/export`
- `GET /api/admin/surveys/{surveyId}/export`

Export APIs must apply the same role permissions as the related list pages.

## Business Validation Matrix

| Scenario | API Layer Behavior |
| --- | --- |
| Registration after deadline | Return `DEADLINE_PASSED` |
| Registration over capacity | Return `CAPACITY_FULL` |
| Duplicate registration phone | Return `DUPLICATE_SUBMISSION` |
| Check-in when activity is not in progress | Return `INVALID_STATE` |
| Duplicate active check-in | Return `ALREADY_CHECKED_IN` |
| Volunteer check-in before approval | Return `NOT_APPROVED` |
| Volunteer duplicate application | Return `DUPLICATE_SUBMISSION` |
| Survey before check-in | Return `NOT_CHECKED_IN` |
| Duplicate survey response | Return `DUPLICATE_SUBMISSION` |
| Unsupported file upload type | Return `UNSUPPORTED_FILE_TYPE` |
| File too large | Return `FILE_TOO_LARGE` |

## Next Step

Use this API design as input for:

1. Test plan.
2. Local Markdown implementation issue breakdown.
3. Backend controller/service DTO design.
4. Frontend route and data-fetching design.

