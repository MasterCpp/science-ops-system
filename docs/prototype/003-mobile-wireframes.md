# Mobile H5 Wireframes V1

Status: Draft

Date: 2026-06-05

## Purpose

Low-fidelity mobile H5 wireframes for public audience and volunteer flows. These pages do not require login in V1.

## Mobile Principles

- Single-column layout.
- Large touch targets.
- Minimal form fields per screen.
- Clear status messages for full, deadline passed, already registered, already checked in, not approved, and already submitted.
- No account login.
- Phone number is used to find registration or volunteer application records.

## Activity Detail and Audience Registration

```text
+------------------------------+
| Cover image                  |
| Activity title               |
| Status tag                   |
| Time                         |
| Location                     |
| Remaining capacity / Full    |
| Registration deadline        |
| Description                  |
| Contact                      |
|                              |
| Registration Form            |
| Name [____________]          |
| Phone [____________]         |
| Attendee count [__]          |
| Unit/School [________]       |
| Age group [v]                |
| Remark [____________]        |
| Custom fields...             |
| [Submit Registration]        |
+------------------------------+
```

States:

- Registration open: form enabled.
- Deadline passed: form disabled with message.
- Capacity full: form disabled with message.
- Already registered: show existing registration result message.

API impact:

- Public activity detail.
- Public registration submit.
- Duplicate phone and capacity validation.

## Registration Result

```text
+------------------------------+
| Registration Successful      |
| Activity title               |
| Name                         |
| Phone                        |
| Attendee count               |
|                              |
| Check-in instructions        |
| [Back to Activity]           |
+------------------------------+
```

States:

- Success.
- Already registered.
- Registration cancelled, if looked up later.

API impact:

- Registration submit response should include enough data to render this page.

## Audience Check-In

```text
+------------------------------+
| Activity Check-in            |
| Activity title               |
| Time / Location              |
|                              |
| Phone [____________]         |
| [Check In]                   |
+------------------------------+
```

Result states:

```text
+------------------------------+
| Check-in Successful          |
| Name                         |
| Check-in time                |
+------------------------------+
```

Error states:

- Already checked in.
- Registration not found.
- Activity not in progress.
- Activity ended.
- Registration cancelled.

API impact:

- Public check-in endpoint.
- Response should distinguish success and known business states.

## Volunteer Position List

```text
+------------------------------+
| Volunteer Positions          |
| Activity title               |
|                              |
| Position card                |
| Name                         |
| Description                  |
| Service time                 |
| Approved / Capacity          |
| [Apply]                      |
|                              |
| Position card...             |
+------------------------------+
```

States:

- Position available.
- Position full.
- Already applied to one position in this activity.

API impact:

- Public volunteer positions endpoint.
- Include approved count and capacity.

## Volunteer Application

```text
+------------------------------+
| Volunteer Application        |
| Activity title               |
| Selected position            |
|                              |
| Name [____________]          |
| Phone [____________]         |
| Unit/School [________]       |
| Age group [v]                |
| Availability note [_____]    |
| Experience note [_____]      |
| Remark [____________]        |
| [Submit Application]         |
+------------------------------+
```

Result states:

- Submitted, pending review.
- Already applied to this activity.
- Position full.

API impact:

- Public volunteer application submit.
- One application per activity phone validation.
- Approved-only capacity rule.

## Volunteer Check-In/Check-Out

```text
+------------------------------+
| Volunteer Service            |
| Activity title               |
| Position name                |
|                              |
| Phone [____________]         |
| [Find My Application]        |
+------------------------------+
```

After lookup:

```text
+------------------------------+
| Name                         |
| Application status           |
| Attendance status            |
| Check-in time                |
| Check-out time               |
| Service minutes              |
|                              |
| [Check In] [Check Out]       |
+------------------------------+
```

States:

- Not found.
- Pending review.
- Rejected.
- Approved, not checked in.
- Checked in.
- Checked out.
- Revoked.

Rules:

- Check-in is only available after approval.
- Check-out is only available after check-in.

API impact:

- Volunteer application lookup/status endpoint.
- Public volunteer check-in endpoint.
- Public volunteer check-out endpoint.

## Satisfaction Survey

```text
+------------------------------+
| Satisfaction Survey          |
| Activity title               |
| Survey title                 |
|                              |
| Phone [____________]         |
| [Start Survey]               |
+------------------------------+
```

After validation:

```text
+------------------------------+
| Question 1                   |
| ( ) Option A                 |
| ( ) Option B                 |
|                              |
| Question 2                   |
| [ ] Option A                 |
| [ ] Option B                 |
|                              |
| Question 3 Rating            |
| [1] [2] [3] [4] [5]          |
|                              |
| Question 4 Text              |
| [________________________]   |
|                              |
| [Submit Survey]              |
+------------------------------+
```

States:

- Not checked in.
- Already submitted.
- Survey closed.
- Submit success.

API impact:

- Public survey eligibility validation.
- Public survey detail.
- Public survey submit.
- One response per registration.

## Survey Submission Result

```text
+------------------------------+
| Survey Submitted             |
| Thank you                    |
| [Back to Activity]           |
+------------------------------+
```

API impact:

- Survey submit response can be minimal after success.

## Primary Mobile Routes

Route names are conceptual and may change during API/frontend design.

```text
/m/activities/{activityId}
/m/activities/{activityId}/registration/success
/m/activities/{activityId}/check-in
/m/activities/{activityId}/volunteers
/m/activities/{activityId}/volunteers/apply
/m/activities/{activityId}/volunteers/attendance
/m/activities/{activityId}/survey
/m/activities/{activityId}/survey/success
```

## API Design Notes

Mobile APIs should be public but business-rule protected:

- Validate activity state before registration and check-in.
- Validate registration deadline and capacity.
- Validate phone uniqueness per activity.
- Validate volunteer approval before attendance.
- Validate check-in before survey submission.
- Avoid exposing admin-only fields.

