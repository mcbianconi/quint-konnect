---
name: ship-it-lands-directly
date: 2026-09-26
---

"Ship it" means: land every finished branch directly onto `origin/main` with `but land`
(no pull requests), push, watch CI, and sync tbd. The procedure is the project skill
`.claude/skills/ship-it/SKILL.md`.

**Why:** Set by the project owner. Review happens in-session (branch diffs plus a full
build) before landing, so a PR round-trip adds no check.

**How to apply:** The full build must pass with all branches applied before landing, and
CI on `main` must pass before reporting success. Other agents' in-progress branches are
not included without asking.
