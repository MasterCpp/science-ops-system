# Admin Wireframes V1

Status: Draft

Date: 2026-06-05

## Purpose

Low-fidelity back-office wireframes for API and frontend planning. These are layout and interaction notes, not final visual design.

## Global Admin Layout

```text
+--------------------------------------------------------------+
| Top bar: system name | current user | role | logout           |
+----------------------+---------------------------------------+
| Sidebar              | Main content                          |
| - Dashboard          |                                       |
| - Activities         | Page title + actions                  |
| - Visitors           | Filters / tabs                        |
| - Operation Logs     | Table / form / detail                 |
| - Accounts           | Pagination / dialogs                  |
+----------------------+---------------------------------------+
```

Global rules:

- Sidebar items are role-aware.
- Protected pages require JWT.
- Tables use filter area, action area, data table, pagination.
- Destructive actions require confirmation.
- State-changing actions should produce operation logs where required.

## Login

```text
+--------------------------------+
| 科普运营系统                   |
| Username [________________]     |
| Password [________________]     |
| [ Log in ]                     |
+--------------------------------+
```

Validation:

- Username required.
- Password required.
- Failed login shows inline error.

## Dashboard

```text
+--------------------------------------------------------------+
| Dashboard                                                    |
| [Activities] [Registrations] [Check-ins] [Volunteers]        |
| [Surveys]    [Photos]        [Service Hours]                 |
|                                                              |
| Upcoming Activities                                          |
| Title | Time | Status | Registered | Checked-in | Actions     |
|                                                              |
| Pending Volunteer Applications                              |
| Activity | Position | Pending Count | Action                 |
+--------------------------------------------------------------+
```

Role behavior:

- Super admin and activity admin see activity, registration, check-in, survey, photo metrics.
- Volunteer admin sees volunteer-related metrics and permitted activity references.

## Activity List

```text
+--------------------------------------------------------------+
| Activities                                      [Create]      |
| Keyword [____] Status [v] Time range [____] [Search] [Reset] |
|                                                              |
| Title | Status | Time | Location | Capacity | Registered     |
| Checked-in | Owner | Actions                                  |
|                                                              |
| Actions: Detail | Edit | Publish | Start | End | Archive     |
| Super admin only: Delete                                    |
+--------------------------------------------------------------+
```

Notes:

- Action buttons depend on activity status and role.
- Capacity displays unlimited when `capacity` is null.
- Registered count uses non-cancelled attendee count.

## Activity Create/Edit

```text
+--------------------------------------------------------------+
| Activity Form                                 [Save Draft]    |
| Basic Info                                                   |
| Title [________________]                                     |
| Cover [Upload] [Preview]                                     |
| Description [rich text area]                                 |
| Start time [____] End time [____]                            |
| Location [________________]                                  |
| Capacity [____] Registration deadline [____]                 |
| Owner [____] Contact phone [____]                            |
|                                                              |
| Plan                                                        |
| Rich text plan [..........................................]   |
| Attachments [Upload]                                        |
|                                                              |
| Process Timeline                                            |
| [Add item]                                                   |
| Time | Title | Description | Sort | Actions                  |
|                                                              |
| Registration Custom Fields                                  |
| [Add field]                                                  |
| Label | Type | Required | Options | Sort | Actions           |
+--------------------------------------------------------------+
```

State behavior:

- Draft: all sections editable.
- Registration open: edits to registration fields, capacity, and deadline allowed but logged.
- In progress: registration fields and capacity disabled.
- Ended: only attachments/photos/summary-related content remains editable.
- Archived: read-only unless super admin unarchives.

## Activity Detail

```text
+--------------------------------------------------------------+
| Activity Title                       Status [REG_OPEN]        |
| [Edit] [Publish/Start/End/Archive] [Copy registration link]  |
| [View check-in QR]                                         |
|                                                              |
| Summary cards: Registered | Checked-in | Check-in Rate       |
| Volunteers | Service Hours | Survey Responses | Photos       |
|                                                              |
| Tabs: Overview | Registration | Check-in | Volunteers        |
|       Survey | Files & Photos | Visitors | Exports           |
+--------------------------------------------------------------+
```

Overview tab:

- Basic information.
- Process timeline.
- Plan content.
- Attachments.

## Registration Management

```text
+--------------------------------------------------------------+
| Registrations                                [Backfill]       |
| Keyword [____] Status [v] Time range [____] [Export]         |
|                                                              |
| Name | Phone | Count | Unit/School | Age | Status | Time     |
| Custom Fields | Actions                                      |
|                                                              |
| Actions: Cancel                                              |
+--------------------------------------------------------------+
```

Backfill dialog fields:

- Name
- Phone
- Attendee count
- Unit/school
- Age group
- Remark
- Custom fields

Rules:

- Cancel changes status to `CANCELLED`.
- Backfill still enforces duplicate phone unless product later changes it.

## Check-In Management

```text
+--------------------------------------------------------------+
| Check-ins                                   [Manual Check-in] |
| Keyword [____] Status [v] Time range [____] [Export]         |
|                                                              |
| Name | Phone | Check-in Time | Method | Manual | Status      |
| Actions                                                      |
|                                                              |
| Actions: Revoke                                              |
+--------------------------------------------------------------+
```

Manual check-in dialog:

- Search registration by phone/name.
- Confirm selected registration.
- Submit manual check-in.

Rules:

- One active check-in per registration.
- Revoke changes status to `REVOKED`.

## Volunteer Position Management

```text
+--------------------------------------------------------------+
| Volunteer Positions                         [Create]         |
| Position | Capacity | Approved | Service Time | Actions      |
|                                                              |
| Actions: Edit | Delete | View Applications                   |
+--------------------------------------------------------------+
```

Create/edit dialog fields:

- Position name
- Description
- Capacity
- Service start time
- Service end time

## Volunteer Application Review

```text
+--------------------------------------------------------------+
| Volunteer Applications                       [Export]        |
| Activity [v] Position [v] Status [v] Keyword [____]          |
|                                                              |
| Name | Phone | Position | Unit/School | Age | Status         |
| Availability | Experience | Actions                         |
|                                                              |
| Actions: Approve | Reject | Cancel                          |
+--------------------------------------------------------------+
```

Review dialog:

- Application detail.
- Review note.
- Approve or reject.

Rules:

- Approved applications occupy position capacity.
- Pending applications do not occupy capacity.

## Volunteer Attendance Management

```text
+--------------------------------------------------------------+
| Volunteer Attendance                         [Export]        |
| Activity [v] Position [v] Status [v] Keyword [____]          |
|                                                              |
| Name | Phone | Position | Check-in | Check-out | Minutes     |
| Adjusted | Status | Actions                                  |
|                                                              |
| Actions: Manual In | Manual Out | Adjust Minutes | Revoke    |
+--------------------------------------------------------------+
```

Adjust dialog fields:

- Service minutes
- Adjustment reason

Rules:

- Attendance row appears after check-in/backfill.
- Service minutes default to check-out minus check-in.

## Visitor Report Management

```text
+--------------------------------------------------------------+
| Visitor Reports                              [Create] [Export]|
| Keyword [____] Activity [v] Visit date [____]                 |
|                                                              |
| Unit | Contact | Phone | Count | Date | Related Activity     |
| Reason | Actions                                             |
|                                                              |
| Actions: Edit | Delete                                      |
+--------------------------------------------------------------+
```

Form fields:

- Visitor unit
- Contact name
- Contact phone
- Visitor count
- Visit date
- Visit reason
- Related activity, optional
- Remark

## Survey Editor

```text
+--------------------------------------------------------------+
| Survey Editor                              [Publish] [Close] |
| Title [________________]                                    |
| Description [.............................................]   |
|                                                              |
| Questions                                      [Add question]|
| Sort | Type | Required | Title | Options | Actions           |
+--------------------------------------------------------------+
```

Question editor:

- Question title
- Type: single choice, multiple choice, rating, text
- Required flag
- Options for single/multiple choice
- Sort order

Rules:

- One survey per activity.
- Published survey can be submitted by checked-in attendees.

## Survey Statistics

```text
+--------------------------------------------------------------+
| Survey Statistics                            [Export]        |
| Responses | Average Rating                                    |
|                                                              |
| Question Statistics                                          |
| Question | Type | Result Summary                             |
|                                                              |
| Raw Responses                                                |
| Name | Phone | Submitted At | Answers | Detail              |
+--------------------------------------------------------------+
```

Notes:

- Rating questions contribute to average rating.
- Raw response export matches confirmed Excel columns.

## Files and Photos

```text
+--------------------------------------------------------------+
| Files & Photos                      [Upload Attachment]       |
|                                    [Upload Photos] [ZIP]      |
| Category [v] Keyword [____]                                  |
|                                                              |
| Preview | Original Name | Category | Size | Uploaded By      |
| Upload Time | Actions                                       |
|                                                              |
| Actions: Preview | Download | Delete                         |
+--------------------------------------------------------------+
```

Rules:

- Attachments support PDF, Word, Excel, PPT, images up to 20MB.
- Photos support JPG, JPEG, PNG, WEBP up to 10MB.
- No recycle bin in V1.

## Operation Log

```text
+--------------------------------------------------------------+
| Operation Logs                                               |
| Admin [____] Action [v] Target [v] Time range [____]         |
|                                                              |
| Time | Admin | Role | Action | Target | IP | Detail          |
+--------------------------------------------------------------+
```

Detail drawer:

- Target summary.
- User agent.
- Detail JSON.

Rules:

- Super admin only.
- No delete action.

## Admin Account Management

```text
+--------------------------------------------------------------+
| Admin Accounts                              [Create]         |
| Keyword [____] Status [v] Role [v]                           |
|                                                              |
| Username | Display Name | Phone | Status | Roles | Actions   |
|                                                              |
| Actions: Edit | Disable/Enable | Assign Roles                |
+--------------------------------------------------------------+
```

Form fields:

- Username
- Password, create/reset only
- Display name
- Phone
- Status
- Roles

Rules:

- Super admin only.
- Role permissions can be viewed but not necessarily edited in V1 unless later added.

## API Design Notes

The admin API should group endpoints by module:

- Auth
- Dashboard
- Activities
- Registrations
- Check-ins
- Volunteers
- Visitors
- Surveys
- Files
- Exports
- Operation logs
- Admin accounts and RBAC

Each list endpoint should support pagination unless it is a small reference list.

