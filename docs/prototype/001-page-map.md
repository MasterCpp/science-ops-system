# Prototype Page Map V1

Status: Draft

Date: 2026-06-05

## Purpose

This document defines the first-pass page map before detailed wireframes and API design.

It is based on:

- `docs/prd/001-科普运营系统-v1.md`
- `docs/database/001-er-design.md`
- `docs/requirements/需求澄清.md`

The goal is to clarify pages, visible data, user actions, navigation, and API-design impact. This is not a visual design document.

## Page Map Summary

### Back Office

- Login
- Dashboard
- Activity list
- Activity create/edit
- Activity detail
- Registration management
- Check-in management
- Volunteer position management
- Volunteer application review
- Volunteer attendance management
- Visitor report management
- Survey editor
- Survey statistics
- Photo and attachment archive
- Export center or export actions
- Operation log
- Admin account and permission management

### Mobile H5

- Activity detail and audience registration
- Registration result
- Audience check-in
- Volunteer position list
- Volunteer application
- Volunteer check-in/check-out
- Satisfaction survey
- Survey submission result

## Back-Office Pages

### Login

Actors:

- Super admin
- Activity admin
- Volunteer admin

Core fields:

- Username
- Password

Actions:

- Log in

Navigation:

- Success goes to Dashboard.

API impact:

- Admin login endpoint.
- JWT storage and authenticated request behavior.

### Dashboard

Actors:

- Super admin
- Activity admin
- Volunteer admin, limited volunteer-related metrics

Core data:

- Activity count by status
- Upcoming activities
- Registration count
- Check-in count
- Volunteer application count
- Approved volunteer count
- Total volunteer service minutes/hours
- Survey response count
- Photo count

Actions:

- Open activity detail
- Open pending volunteer applications

Navigation:

- To Activity list
- To Volunteer application review

API impact:

- Dashboard summary endpoint.
- Role-aware metric visibility.

### Activity List

Actors:

- Super admin
- Activity admin
- Volunteer admin, read-only or limited visibility

Core fields:

- Title
- Status
- Start time
- End time
- Location
- Capacity
- Registered attendee count
- Checked-in count
- Owner name

Filters:

- Keyword
- Status
- Time range

Actions:

- Create activity
- Edit activity
- Publish activity
- Mark in progress
- End activity
- Archive activity
- Delete activity, super admin only
- Open detail

Navigation:

- To Activity create/edit
- To Activity detail

API impact:

- Activity list query with filters.
- Activity lifecycle operation endpoints.
- Delete permission endpoint behavior.

### Activity Create/Edit

Actors:

- Super admin
- Activity admin

Core fields:

- Title
- Cover image
- Description
- Start time
- End time
- Location
- Capacity
- Registration deadline
- Owner name
- Contact phone
- Plan rich text
- Plan attachments
- Process timeline items
- Registration custom fields

Actions:

- Save draft
- Save changes
- Upload cover
- Upload attachment
- Add/edit/delete process item
- Add/edit/delete custom registration field

State rules:

- Draft: all fields editable.
- Registration open: base information editable; registration fields, capacity, and deadline changes must be logged.
- In progress: registration fields and capacity locked; plan, process, attachments, and photos editable.
- Ended: only photos, attachments, and summary data can be added.
- Archived: read-only unless super admin unarchives.

API impact:

- Activity create/update endpoints.
- File upload endpoints.
- Process item CRUD.
- Custom field CRUD.
- State-aware validation.

### Activity Detail

Actors:

- Super admin
- Activity admin
- Volunteer admin, limited volunteer areas

Core sections:

- Activity base information
- Lifecycle status
- Registration link
- Check-in QR code
- Summary metrics
- Process timeline
- Plan attachments
- Related tabs for registration, check-in, volunteers, survey, photos, visitor reports, exports

Actions:

- Copy registration link
- View/generate check-in QR code
- Open related management tabs
- Change activity status, if permitted

API impact:

- Activity detail endpoint.
- Registration link endpoint or computed route.
- Check-in QR code endpoint.
- Activity summary endpoint.

### Registration Management

Actors:

- Super admin
- Activity admin
- Volunteer admin, partial read-only fields only

Core fields:

- Activity name
- Name
- Phone
- Attendee count
- Unit or school
- Age group
- Remark
- Custom field values
- Registration time
- Status

Filters:

- Keyword
- Status
- Registration time range

Actions:

- Backfill registration
- Cancel registration
- Export registration Excel

API impact:

- Registration list endpoint.
- Registration backfill endpoint.
- Registration cancel endpoint.
- Registration export endpoint.
- Capacity calculation based on non-cancelled `attendee_count`.

### Check-In Management

Actors:

- Super admin
- Activity admin

Core fields:

- Activity name
- Name
- Phone
- Check-in time
- Method
- Manual flag
- Status

Filters:

- Keyword
- Check-in status
- Check-in time range

Actions:

- Manual check-in
- Revoke check-in
- Export check-in Excel

API impact:

- Check-in list endpoint.
- Manual check-in endpoint.
- Revoke check-in endpoint.
- Check-in export endpoint.

### Volunteer Position Management

Actors:

- Super admin
- Activity admin
- Volunteer admin

Core fields:

- Activity name
- Position name
- Description
- Capacity
- Approved count
- Service start time
- Service end time

Actions:

- Create position
- Edit position
- Delete position

API impact:

- Volunteer position CRUD.
- Approved count calculation.
- Position capacity validation.

### Volunteer Application Review

Actors:

- Super admin
- Activity admin
- Volunteer admin

Core fields:

- Activity name
- Position name
- Name
- Phone
- Unit or school
- Age group
- Availability note
- Experience note
- Remark
- Status
- Review note

Filters:

- Activity
- Position
- Status
- Keyword

Actions:

- Approve application
- Reject application
- Cancel application
- Export volunteer data

API impact:

- Volunteer application list endpoint.
- Review endpoints.
- One application per activity phone validation.
- Approved-only capacity occupancy.

### Volunteer Attendance Management

Actors:

- Super admin
- Activity admin
- Volunteer admin

Core fields:

- Activity name
- Position name
- Volunteer name
- Phone
- Check-in time
- Check-out time
- Service minutes/hours
- Manual adjustment flag
- Status

Actions:

- Manual check-in
- Manual check-out
- Revoke attendance
- Adjust service minutes
- Export volunteer data

API impact:

- Volunteer attendance list endpoint.
- Volunteer manual check-in/check-out endpoints.
- Revoke endpoint.
- Service minutes adjustment endpoint.

### Visitor Report Management

Actors:

- Super admin
- Activity admin

Core fields:

- Visitor unit
- Contact name
- Contact phone
- Visitor count
- Visit date
- Visit reason
- Related activity
- Remark

Filters:

- Visit date range
- Related activity
- Keyword

Actions:

- Create report
- Edit report
- Delete report
- Export visitor report Excel

API impact:

- Visitor report CRUD.
- Optional activity relation.
- Visitor report export endpoint.

### Survey Editor

Actors:

- Super admin
- Activity admin

Core fields:

- Survey title
- Description
- Question list
- Question type
- Required flag
- Options for single/multiple choice
- Sort order

Actions:

- Create survey
- Edit survey
- Add/edit/delete question
- Add/edit/delete option
- Publish survey
- Close survey

API impact:

- Survey CRUD.
- Question CRUD.
- Option CRUD.
- One survey per activity validation.

### Survey Statistics

Actors:

- Super admin
- Activity admin

Core fields:

- Survey response count
- Average rating
- Question-level statistics
- Raw answer table

Actions:

- View statistics
- View raw responses
- Export survey result Excel

API impact:

- Survey statistics endpoint.
- Survey response list endpoint.
- Survey export endpoint.

### Photo and Attachment Archive

Actors:

- Super admin
- Activity admin

Core fields:

- File category
- Original filename
- File size
- Upload time
- Uploaded by
- Preview for photos

Actions:

- Upload attachment
- Upload photos
- Preview photo
- Delete file
- Batch download photos ZIP

API impact:

- File upload endpoint.
- File list endpoint.
- File preview/download endpoint.
- File delete endpoint.
- Photo ZIP endpoint.
- Type and size validation.

### Operation Log

Actors:

- Super admin

Core fields:

- Admin username
- Role code
- Action
- Target type
- Target summary
- IP
- User agent
- Created time
- Detail JSON

Filters:

- Admin
- Action
- Target type
- Time range

Actions:

- Search logs
- View log detail

API impact:

- Operation log list endpoint.
- Operation log detail endpoint.
- No delete endpoint in V1.

### Admin Account and Permission Management

Actors:

- Super admin

Core fields:

- Username
- Display name
- Phone
- Status
- Roles
- Permissions by role

Actions:

- Create admin user
- Edit admin user
- Disable/enable admin user
- Assign roles
- View role permissions

API impact:

- Admin user CRUD.
- Role list endpoint.
- Permission list endpoint.
- User-role assignment endpoint.

## Mobile H5 Pages

### Activity Detail and Audience Registration

Actors:

- Audience user

Core fields:

- Activity cover
- Activity title
- Description
- Time
- Location
- Remaining capacity or full status
- Registration deadline
- Owner/contact phone
- Name
- Phone
- Attendee count
- Unit or school
- Age group
- Remark
- Custom fields

Actions:

- Submit registration

States:

- Registration open
- Deadline passed
- Capacity full
- Already registered

API impact:

- Public activity detail endpoint.
- Public registration submit endpoint.
- Capacity and duplicate phone validation.

### Registration Result

Actors:

- Audience user

Core data:

- Activity title
- Registration status
- Registration name
- Registration phone
- Check-in instructions

Actions:

- Return to activity detail

API impact:

- Registration detail or submit response shape.

### Audience Check-In

Actors:

- Audience user

Core fields:

- Activity title
- Phone
- Optional name confirmation

Actions:

- Submit check-in

States:

- Check-in success
- Already checked in
- Registration not found
- Activity not in progress
- Activity ended

API impact:

- Public check-in endpoint.
- Duplicate check-in behavior.
- Activity-state validation.

### Volunteer Position List

Actors:

- Volunteer applicant

Core fields:

- Activity title
- Position name
- Description
- Capacity
- Approved count
- Service start time
- Service end time

Actions:

- Select position

States:

- Position available
- Position full
- Already applied

API impact:

- Public volunteer position list endpoint.
- Position capacity display.

### Volunteer Application

Actors:

- Volunteer applicant

Core fields:

- Selected position
- Name
- Phone
- Unit or school
- Age group
- Availability note
- Experience note
- Remark

Actions:

- Submit application

States:

- Submit success
- Already applied to this activity
- Position full

API impact:

- Public volunteer application submit endpoint.
- One application per activity phone validation.

### Volunteer Check-In/Check-Out

Actors:

- Approved volunteer

Core fields:

- Activity title
- Position name
- Phone
- Application status
- Current attendance status

Actions:

- Check in
- Check out

States:

- Not approved
- Checked in
- Checked out
- Attendance revoked

API impact:

- Public volunteer attendance status endpoint.
- Public volunteer check-in endpoint.
- Public volunteer check-out endpoint.

### Satisfaction Survey

Actors:

- Checked-in audience user

Core fields:

- Activity title
- Survey title
- Questions
- Options
- Rating controls
- Text answers

Actions:

- Submit survey

States:

- Not checked in
- Already submitted
- Survey closed
- Submit success

API impact:

- Public survey detail endpoint.
- Public survey submit endpoint.
- One response per registration validation.

### Survey Submission Result

Actors:

- Audience user

Core data:

- Activity title
- Submission success message

Actions:

- Return to activity detail

API impact:

- Survey submit response shape.

## Primary Navigation Flows

### Back Office Activity Flow

```text
Login -> Dashboard -> Activity List -> Activity Create/Edit -> Activity Detail
Activity Detail -> Registration Management
Activity Detail -> Check-In Management
Activity Detail -> Volunteer Position Management
Activity Detail -> Survey Editor/Statistics
Activity Detail -> Photo and Attachment Archive
Activity Detail -> Visitor Report Management
```

### Mobile Audience Flow

```text
Activity Detail -> Submit Registration -> Registration Result
Check-In QR -> Audience Check-In -> Check-In Result
Survey Link -> Satisfaction Survey -> Survey Submission Result
```

### Mobile Volunteer Flow

```text
Activity Detail -> Volunteer Position List -> Volunteer Application
Volunteer Check-In Link -> Volunteer Check-In/Check-Out
```

## API Design Inputs

API design should be driven by these page needs:

- Role-aware back-office navigation and permissions.
- Activity list filters and activity lifecycle actions.
- Activity detail aggregation.
- Mobile public detail pages that do not require JWT.
- Registration capacity and duplicate-phone validation.
- Check-in duplicate and activity-state validation.
- Volunteer application uniqueness and approved-only attendance validation.
- Survey checked-in-only and one-response validation.
- File upload/download/preview/delete/ZIP operations.
- Export endpoints for confirmed Excel outputs.
- Operation log search and detail.

## Next Documents

- `docs/prototype/002-admin-wireframes.md`
- `docs/prototype/003-mobile-wireframes.md`
- `docs/api/001-api-design.md`

