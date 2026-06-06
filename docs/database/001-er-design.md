# Database Design: ER Design V1

Status: Ready for API design

Date: 2026-06-05

## Purpose

This document records the first-pass database design for the Science Operations System V1.

It is based on:

- `docs/requirements/需求澄清.md`
- `docs/prd/001-科普运营系统-v1.md`
- `docs/adr/003-persistence-and-migrations.md`
- `docs/adr/004-authentication-and-authorization.md`
- `docs/adr/005-file-storage.md`

## Global Conventions

- Database: MySQL 8.
- Persistence: MyBatis-Plus.
- Migrations: Flyway.
- Primary keys: `BIGINT` snowflake IDs.
- Table names use lowercase singular snake case, such as `activity`.
- Column names use lowercase snake case, such as `created_at`.
- Foreign key columns use `{entity}_id`, such as `activity_id`.
- Business-table logical deletion: `deleted` boolean-like flag.
- Status fields: `VARCHAR` string enums.
- MySQL time type: `datetime`.
- Java time type: `LocalDateTime`.
- Service duration is stored as integer minutes in `service_minutes`.
- Common audit fields on business tables:
  - `id`
  - `created_at`
  - `updated_at`
  - `deleted`
- Tables that need back-office audit attribution also include:
  - `created_by`
  - `updated_by`
- Operation log records are append-only and are not logically deleted.
- Phone numbers are stored in plaintext in V1.

## Confirmed Modeling Decisions

### Permissions

Use standard RBAC tables:

- `admin_user`
- `role`
- `permission`
- `role_permission`
- `admin_user_role`

Rationale:

- Matches ADR 004.
- Keeps the current three-role model extensible.
- Avoids hard-coding all permissions into `admin_user.role`.

### Registration Custom Fields

Use field definition and field value tables:

- `activity_custom_field`
- `registration_custom_value`

Rationale:

- Fits the confirmed fixed fields plus limited custom fields model.
- Keeps export behavior explicit.
- Avoids burying custom field values in JSON for V1.

## Candidate Entity Groups

### Admin and Permissions

- `admin_user`
- `role`
- `permission`
- `admin_user_role`
- `role_permission`

### Activity Core

- `activity`
- `activity_process_item`
- `activity_custom_field`

### Audience Registration and Check-In

- `registration`
- `registration_custom_value`
- `check_in`

### Volunteer

- `volunteer_position`
- `volunteer_application`
- `volunteer_attendance`

### Visitor Report

- `visitor_report`

### Survey

- `survey`
- `survey_question`
- `survey_option`
- `survey_response`
- `survey_answer`

### Files

- `file_asset`

### Audit

- `operation_log`

## Modeling Status

The V1 ER design decisions needed before API design have been confirmed.

## Confirmed Relationships and Rules

### Activity, Registration, and Check-In

- Activity cover is stored as a `file_asset`.
- `activity.cover_file_id` references `file_asset.id`.
- `registration` is the audience registration master table.
- `check_in` is a separate table from `registration`.
- `check_in.registration_id` is unique, enforcing one check-in record per registration.
- Check-in fields should not be stored directly on `registration`.
- Capacity calculation uses the sum of `registration.attendee_count` for non-cancelled registrations.
- `registration.attendee_count` represents how many seats one registration consumes.
- Registration cancellation is represented by status, not physical deletion.
- `registration.status` includes at least:
  - `REGISTERED`
  - `CANCELLED`
- Check-in revocation is represented by status, not physical deletion.
- `check_in.status` includes at least:
  - `CHECKED_IN`
  - `REVOKED`
- `check_in` should store enough audit fields to support manual backfill and revocation, including the acting admin and relevant timestamps.

### Volunteer Position, Application, and Attendance

- Volunteer modeling uses three separate tables:
  - `volunteer_position`
  - `volunteer_application`
  - `volunteer_attendance`
- `volunteer_position` stores activity positions and capacity.
- `volunteer_application` stores volunteer application and review state.
- `volunteer_attendance` stores check-in, check-out, service duration, manual correction, and revocation data.
- A volunteer can apply to only one position within the same activity.
- Enforce one application per activity phone number with a uniqueness constraint on `activity_id + phone`.
- Position capacity is calculated from approved applications only.
- `volunteer_application.status` includes at least:
  - `PENDING`
  - `APPROVED`
  - `REJECTED`
  - `CANCELLED`
- `volunteer_attendance` records are created only when a volunteer actually checks in or an admin backfills check-in.
- Do not pre-create empty volunteer attendance rows at application approval time.
- `volunteer_attendance.status` includes at least:
  - `CHECKED_IN`
  - `CHECKED_OUT`
  - `REVOKED`
- `volunteer_attendance` should reference `volunteer_application.id`.
- Service duration defaults to check-out time minus check-in time and may be manually corrected by permitted admins.

### Survey Questions, Responses, and Answers

- One activity has one V1 satisfaction survey.
- `survey` stores the survey master record.
- `survey_question` stores questions.
- `survey_question` fields should include question title/content, question type, required flag, and sort order.
- `survey_question.type` includes at least:
  - `SINGLE_CHOICE`
  - `MULTIPLE_CHOICE`
  - `RATING`
  - `TEXT`
- `survey_option` stores options for single-choice and multiple-choice questions.
- `survey_response` stores one submitted questionnaire response.
- `survey_response` references `survey.id`.
- `survey_response` references `registration.id`.
- `survey_response` should redundantly store respondent name and phone for easier export.
- Enforce one response per registration per survey with a uniqueness constraint on `survey_id + registration_id`.
- `survey_answer` stores one answer per question.
- Single-choice answers store `option_id`.
- Multiple-choice answers store selected option IDs in `option_ids_json`.
- Rating answers store `numeric_value`.
- Text answers store `text_value`.
- This avoids an additional answer-option join table in V1 while keeping export and statistics practical.

### File Metadata

- Use one `file_asset` table for activity covers, attachments, and photos.
- `file_asset.category` includes at least:
  - `COVER`
  - `ATTACHMENT`
  - `PHOTO`
- `file_asset` should store:
  - `activity_id`
  - `category`
  - `original_name`
  - `stored_name`
  - `mime_type`
  - `extension`
  - `size_bytes`
  - `storage_path`
  - `uploaded_by`
  - common audit fields
- Activity cover references `file_asset` through `activity.cover_file_id`.
- Activity attachments and photos are queried through `file_asset.activity_id + category`.
- V1 only supports activity-related files and does not include a global file library.

### Operation Log

- Use one generic `operation_log` table for auditable admin actions.
- `operation_log` should not use logical deletion.
- `operation_log` should store:
  - `admin_user_id`
  - `admin_username`
  - `admin_role_code`
  - `action`
  - `target_type`
  - `target_id`
  - `target_summary`
  - `ip`
  - `user_agent`
  - `detail_json`
  - `created_at`
- `target_type` and `target_id` use a generic target-object model instead of strict foreign keys.
- This keeps audit logging flexible across activity, registration, check-in, volunteer, file, export, and other operations.

## Table Structure Draft

This section is a design draft, not a Flyway migration script.

### `admin_user`

Back-office admin account.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `username` | varchar(64) | Unique login name |
| `password_hash` | varchar(255) | Secure one-way hash |
| `display_name` | varchar(64) | Display name |
| `phone` | varchar(32) | Optional contact phone |
| `status` | varchar(32) | `ENABLED`, `DISABLED` |
| `last_login_at` | datetime | Last successful login time |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

### `role`

Back-office role.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `code` | varchar(64) | Unique role code |
| `name` | varchar(64) | Role name |
| `description` | varchar(255) | Optional description |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

Initial role codes:

- `SUPER_ADMIN`
- `ACTIVITY_ADMIN`
- `VOLUNTEER_ADMIN`

### `permission`

Permission definition.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `code` | varchar(128) | Unique permission code |
| `name` | varchar(128) | Permission display name |
| `module` | varchar(64) | Module grouping |
| `description` | varchar(255) | Optional description |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

### `admin_user_role`

Admin-to-role join table.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `admin_user_id` | bigint | References `admin_user.id` |
| `role_id` | bigint | References `role.id` |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

### `role_permission`

Role-to-permission join table.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `role_id` | bigint | References `role.id` |
| `permission_id` | bigint | References `permission.id` |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

### `activity`

Activity master table.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `title` | varchar(200) | Activity title |
| `cover_file_id` | bigint | References `file_asset.id`, nullable |
| `description` | text | Activity introduction |
| `start_time` | datetime | Activity start time |
| `end_time` | datetime | Activity end time |
| `location` | varchar(255) | Activity location |
| `capacity` | int | Nullable means unlimited |
| `registration_deadline` | datetime | Registration deadline |
| `owner_name` | varchar(64) | Activity owner |
| `contact_phone` | varchar(32) | Contact phone |
| `plan_content` | text | Rich-text plan content |
| `status` | varchar(32) | Activity lifecycle status |
| `created_by` | bigint | Admin user id |
| `updated_by` | bigint | Admin user id |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

Activity statuses:

- `DRAFT`
- `REGISTRATION_OPEN`
- `IN_PROGRESS`
- `ENDED`
- `ARCHIVED`

### `activity_process_item`

Timeline entry for activity process.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `activity_id` | bigint | References `activity.id` |
| `time_label` | varchar(64) | Human-readable time, such as `14:00` |
| `title` | varchar(200) | Process item title |
| `description` | text | Optional details |
| `sort_order` | int | Display order |
| `created_by` | bigint | Admin user id |
| `updated_by` | bigint | Admin user id |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

### `activity_custom_field`

Custom registration field definition for one activity.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `activity_id` | bigint | References `activity.id` |
| `field_key` | varchar(64) | Stable key within activity |
| `label` | varchar(128) | Display label |
| `field_type` | varchar(32) | `TEXT`, `SELECT`, `MULTI_SELECT`, `NUMBER` |
| `required` | tinyint | Required flag |
| `options_json` | json | Options for select fields |
| `sort_order` | int | Display order |
| `created_by` | bigint | Admin user id |
| `updated_by` | bigint | Admin user id |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

### `registration`

Audience registration record.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `activity_id` | bigint | References `activity.id` |
| `name` | varchar(64) | Audience name |
| `phone` | varchar(32) | Audience phone |
| `attendee_count` | int | Seats consumed |
| `unit_name` | varchar(128) | Unit or school |
| `age_group` | varchar(64) | Age group |
| `remark` | varchar(500) | Optional remark |
| `status` | varchar(32) | `REGISTERED`, `CANCELLED` |
| `cancelled_by` | bigint | Admin user id, nullable |
| `cancelled_at` | datetime | Nullable |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

### `registration_custom_value`

Submitted value for one registration custom field.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `registration_id` | bigint | References `registration.id` |
| `custom_field_id` | bigint | References `activity_custom_field.id` |
| `field_key` | varchar(64) | Snapshot key |
| `label` | varchar(128) | Snapshot label |
| `value_text` | text | Submitted value |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

### `check_in`

Audience check-in record.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `activity_id` | bigint | References `activity.id` |
| `registration_id` | bigint | Unique reference to `registration.id` |
| `check_in_time` | datetime | Check-in time |
| `method` | varchar(32) | `QR`, `MANUAL` |
| `status` | varchar(32) | `CHECKED_IN`, `REVOKED` |
| `manual` | tinyint | Whether admin backfilled |
| `handled_by` | bigint | Admin user id for manual action, nullable |
| `revoked_by` | bigint | Admin user id, nullable |
| `revoked_at` | datetime | Nullable |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

### `volunteer_position`

Volunteer position for an activity.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `activity_id` | bigint | References `activity.id` |
| `name` | varchar(128) | Position name |
| `description` | text | Position description |
| `capacity` | int | Position capacity |
| `service_start_time` | datetime | Service start time |
| `service_end_time` | datetime | Service end time |
| `created_by` | bigint | Admin user id |
| `updated_by` | bigint | Admin user id |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

### `volunteer_application`

Volunteer application and review record.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `activity_id` | bigint | References `activity.id` |
| `position_id` | bigint | References `volunteer_position.id` |
| `name` | varchar(64) | Volunteer name |
| `phone` | varchar(32) | Volunteer phone |
| `unit_name` | varchar(128) | Unit or school |
| `age_group` | varchar(64) | Age group |
| `available_time_note` | varchar(500) | Availability note |
| `experience_note` | varchar(500) | Past service experience |
| `remark` | varchar(500) | Optional remark |
| `status` | varchar(32) | `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED` |
| `reviewed_by` | bigint | Admin user id, nullable |
| `reviewed_at` | datetime | Nullable |
| `review_note` | varchar(500) | Nullable |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

### `volunteer_attendance`

Volunteer check-in, check-out, and service duration.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `activity_id` | bigint | References `activity.id` |
| `application_id` | bigint | References `volunteer_application.id` |
| `check_in_time` | datetime | Nullable until checked in |
| `check_out_time` | datetime | Nullable until checked out |
| `service_minutes` | int | Service duration in minutes |
| `status` | varchar(32) | `CHECKED_IN`, `CHECKED_OUT`, `REVOKED` |
| `manually_adjusted` | tinyint | Whether service minutes were manually adjusted |
| `adjusted_service_minutes` | int | Nullable manual value |
| `adjustment_reason` | varchar(500) | Nullable |
| `handled_by` | bigint | Admin user id for manual action, nullable |
| `revoked_by` | bigint | Admin user id, nullable |
| `revoked_at` | datetime | Nullable |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

### `visitor_report`

Visitor report record.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `activity_id` | bigint | Nullable reference to `activity.id` |
| `visitor_unit` | varchar(128) | Visitor unit |
| `contact_name` | varchar(64) | Contact name |
| `contact_phone` | varchar(32) | Contact phone |
| `visitor_count` | int | Number of visitors |
| `visit_date` | datetime | Visit date |
| `visit_reason` | varchar(500) | Visit reason |
| `remark` | varchar(500) | Optional remark |
| `created_by` | bigint | Admin user id |
| `updated_by` | bigint | Admin user id |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

### `survey`

Survey master record.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `activity_id` | bigint | Unique reference to `activity.id` |
| `title` | varchar(200) | Survey title |
| `description` | text | Optional description |
| `status` | varchar(32) | `DRAFT`, `PUBLISHED`, `CLOSED` |
| `created_by` | bigint | Admin user id |
| `updated_by` | bigint | Admin user id |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

### `survey_question`

Survey question definition.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `survey_id` | bigint | References `survey.id` |
| `title` | varchar(500) | Question title |
| `type` | varchar(32) | `SINGLE_CHOICE`, `MULTIPLE_CHOICE`, `RATING`, `TEXT` |
| `required` | tinyint | Required flag |
| `sort_order` | int | Display order |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

### `survey_option`

Survey question option.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `question_id` | bigint | References `survey_question.id` |
| `label` | varchar(255) | Option label |
| `value` | varchar(128) | Option value |
| `sort_order` | int | Display order |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

### `survey_response`

One submitted survey response.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `survey_id` | bigint | References `survey.id` |
| `registration_id` | bigint | References `registration.id` |
| `respondent_name` | varchar(64) | Snapshot name |
| `respondent_phone` | varchar(32) | Snapshot phone |
| `submitted_at` | datetime | Submit time |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

### `survey_answer`

Answer for one question in one response.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `response_id` | bigint | References `survey_response.id` |
| `question_id` | bigint | References `survey_question.id` |
| `option_id` | bigint | For single-choice answer, nullable |
| `option_ids_json` | json | For multiple-choice answer, nullable |
| `numeric_value` | decimal(5,2) | For rating answer, nullable |
| `text_value` | text | For text answer, nullable |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

### `file_asset`

Activity-related file metadata.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `activity_id` | bigint | References `activity.id` |
| `category` | varchar(32) | `COVER`, `ATTACHMENT`, `PHOTO` |
| `original_name` | varchar(255) | Original filename |
| `stored_name` | varchar(255) | Stored filename |
| `mime_type` | varchar(128) | MIME type |
| `extension` | varchar(32) | File extension |
| `size_bytes` | bigint | File size |
| `storage_path` | varchar(500) | Relative or configured storage path |
| `uploaded_by` | bigint | Admin user id |
| `created_at` | datetime | Common field |
| `updated_at` | datetime | Common field |
| `deleted` | tinyint | Logical delete |

### `operation_log`

Append-only operation audit log.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | Snowflake ID |
| `admin_user_id` | bigint | Acting admin id |
| `admin_username` | varchar(64) | Snapshot username |
| `admin_role_code` | varchar(64) | Snapshot role code |
| `action` | varchar(128) | Operation action |
| `target_type` | varchar(64) | Generic target type |
| `target_id` | bigint | Generic target id |
| `target_summary` | varchar(255) | Human-readable target summary |
| `ip` | varchar(64) | Request IP |
| `user_agent` | varchar(500) | Request user agent |
| `detail_json` | json | Additional operation details |
| `created_at` | datetime | Log time |

## Indexes and Constraints

### Unique Constraints

- `admin_user.username`
- `role.code`
- `permission.code`
- `admin_user_role(admin_user_id, role_id)`
- `role_permission(role_id, permission_id)`
- `activity_custom_field(activity_id, field_key)`
- `registration(activity_id, phone)`
- `check_in(registration_id)`
- `volunteer_application(activity_id, phone)`
- `volunteer_attendance(application_id)`
- `survey(activity_id)`
- `survey_response(survey_id, registration_id)`
- `survey_answer(response_id, question_id)`

### Normal Indexes

- `activity.status`
- `activity.start_time`
- `registration.activity_id`
- `registration.phone`
- `registration.status`
- `check_in.activity_id`
- `volunteer_position.activity_id`
- `volunteer_application.position_id`
- `volunteer_application.status`
- `volunteer_attendance.activity_id`
- `visitor_report.activity_id`
- `survey_question.survey_id`
- `survey_answer.response_id`
- `file_asset(activity_id, category)`
- `operation_log.admin_user_id`
- `operation_log.action`
- `operation_log(target_type, target_id)`
- `operation_log.created_at`

## Next Step

Use this ER design as the input for:

1. API design under `docs/api/`.
2. Flyway migration design once implementation starts.
3. MyBatis-Plus entity and mapper design.
