# Triage Labels

This project uses the default triage label vocabulary expected by Matt Pocock-style skills.

## Labels

| Role | Label |
| --- | --- |
| Maintainer needs to evaluate | `needs-triage` |
| Waiting on reporter or requirement owner | `needs-info` |
| Fully specified and ready for an agent | `ready-for-agent` |
| Ready but should be implemented by a human | `ready-for-human` |
| Will not be actioned | `wontfix` |

## Usage

- New unclear requirements should start as `needs-triage`.
- Missing requirement details should move to `needs-info`.
- Implementation issues should only become `ready-for-agent` when acceptance criteria are clear and no hidden business decisions remain.
- Use `ready-for-human` when the task is specified but should not be delegated to an agent.
- Use `wontfix` for explicitly rejected work.

