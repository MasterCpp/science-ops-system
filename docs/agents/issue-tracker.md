# Issue Tracker

This project uses a local Markdown issue tracker.

## Location

Issues live under:

```text
.scratch/issues/
```

## Usage

- Each issue should be a Markdown file.
- Use clear, stable filenames such as `001-activity-management.md`.
- Each issue should include:
  - Title
  - Status or triage label
  - Background
  - Scope
  - Acceptance criteria
  - Dependencies or related documents

## Skill Behavior

Skills such as `to-prd`, `to-issues`, `triage`, and `qa` should read and write local Markdown issues instead of using GitHub or GitLab.

Do not call `gh issue create` or `glab issue create` for this project unless the issue tracker decision is changed in this file and `AGENTS.md`.

