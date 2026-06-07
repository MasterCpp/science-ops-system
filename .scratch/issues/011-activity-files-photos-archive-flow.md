# Issue 011: 活动文件、封面、附件和照片归档闭环

Status: `completed`

## Parent

- `.scratch/issues/000-prd-science-ops-system-v1.md`

## Related docs

- `docs/adr/005-file-storage.md`
- `docs/api/001-api-design.md`
- `docs/database/001-er-design.md`
- `docs/prototype/002-admin-wireframes.md`
- `docs/testing/001-test-plan.md`

## What to build

实现本地文件存储、文件元数据、活动封面、活动附件、活动照片、预览、下载、删除和活动照片 ZIP 下载。

## Acceptance criteria

- [x] 活动封面上传接受支持的图片类型。
- [x] 活动附件上传接受 PDF、Word、Excel、PPT 和图片，最大 20MB。
- [x] 活动照片上传接受 JPG、JPEG、PNG、WEBP，最大 10MB。
- [x] 不支持的文件类型返回 `UNSUPPORTED_FILE_TYPE`。
- [x] 超过大小限制返回 `FILE_TOO_LARGE`。
- [x] 文件字节保存到本地存储目录。
- [x] 文件元数据保存到 `file_asset`。
- [x] 活动封面可以通过 `activity.cover_file_id` 引用。
- [x] 后台可以查看活动文件列表。
- [x] 后台可以预览支持的照片。
- [x] 后台可以下载文件。
- [x] 后台可以删除文件。
- [x] 删除后的文件不出现在普通列表。
- [x] 活动照片 ZIP 只包含未删除照片。
- [x] 覆盖类型校验、大小校验、上传、预览、下载、删除和 ZIP 下载测试。

## Blocked by

- `.scratch/issues/004-admin-activity-lifecycle.md`

## Completion notes

Completed on 2026-06-07.

- Added local file storage using `science-ops.storage.local-path` and the ADR storage layout under `activities/{activityId}`.
- Added admin activity file upload, list, preview, download, delete, and photo ZIP endpoints.
- Added per-category type and size validation with `UNSUPPORTED_FILE_TYPE` and `FILE_TOO_LARGE` business errors.
- File metadata is persisted to `file_asset`; deleted files are logically hidden from normal lists and excluded from photo ZIP downloads.
- Existing activity update can reference an uploaded cover through `activity.cover_file_id`.
- Covered upload, validation, cover reference, list, preview, download, delete, permission denial, and ZIP behavior in integration tests.
