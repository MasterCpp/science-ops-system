# ADR 005: File Storage

Status: Accepted

Date: 2026-06-05

## Context

V1 needs to store activity plan attachments and activity photos.

Confirmed file requirements:

- Activity plan attachments support PDF, Word, Excel, PPT, and images up to 20MB.
- Activity photos support JPG, JPEG, PNG, and WEBP up to 10MB.
- Photos are archived by activity.
- Back-office users can preview photos, delete individual photos, and batch download activity photos as ZIP.
- V1 uses server local storage and does not implement object storage, recycle bin, or restore flow.

## Decision

Use server local storage for file bytes and store file metadata in MySQL.

Target storage layout:

```text
storage/
  activities/
    {activityId}/
      attachments/
      photos/
```

The database should store metadata for each file, including:

- Stored filename
- Original filename
- File category
- MIME type or file extension
- File size
- Storage path
- Related activity
- Uploading admin
- Upload time
- Deletion status if logical deletion is chosen later

File operations must go through backend APIs for upload, preview, download, ZIP download, and deletion.

## Rationale

- Local storage matches the confirmed V1 requirement.
- Storing file bytes in the database would make backups, downloads, previews, and ZIP generation heavier than necessary.
- Database metadata keeps files queryable and auditable while allowing the file bytes to remain on disk.
- Activity-based directories make archive structure easy to inspect and back up.
- The metadata boundary keeps a future object storage migration feasible.

## Consequences

- Deployment documentation must define the storage root path and backup expectations.
- Local development must create or configure a writable storage directory.
- File upload APIs must validate type and size.
- Download and preview APIs must enforce back-office permissions.
- Public access to raw file paths should be avoided; files should be served through backend-controlled endpoints unless a later ADR chooses otherwise.
- Batch photo download should generate a temporary ZIP from the selected activity photos.

## Out of Scope

This ADR does not decide:

- Exact table names
- Logical vs physical deletion
- Temporary ZIP cleanup schedule
- Image thumbnail generation
- Object storage integration
- CDN/public static hosting

