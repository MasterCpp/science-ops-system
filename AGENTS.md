# Agent Instructions

This repository is the project memory for the Science Operations System. It is intended to support work across multiple Codex/agent conversations.

## Start Every Task

Before changing requirements, issues, docs, or code, read these files in order:

1. `AGENTS.md`
2. `STATUS.md`
3. `CONTEXT.md`
4. `docs/agents/issue-tracker.md`
5. `docs/agents/triage-labels.md`
6. `docs/agents/domain.md`
7. The current PRD, ADR, requirement note, or issue relevant to the task

If a new conversation starts without enough context, recover context from these files instead of relying on previous chat history.

## Agent Skills

### Issue tracker

Issues are tracked as local Markdown files under `.scratch/issues/`. See `docs/agents/issue-tracker.md`.

### Triage labels

The project uses the default Matt Pocock skills triage label vocabulary. See `docs/agents/triage-labels.md`.

### Domain docs

The project uses a single-context domain layout: root `CONTEXT.md` plus `docs/adr/`. See `docs/agents/domain.md`.

## Working Rules

- Do not write business code unless the current issue or the user explicitly asks to enter implementation.
- Requirement changes must update the relevant requirement or PRD document before issues or code are changed.
- Architectural decisions that affect implementation direction must be captured in `docs/adr/`.
- Keep project knowledge in repository documents so future conversations can resume from files.
- Prefer small, independently reviewable issues when splitting implementation work.
