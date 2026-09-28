---
type: is
id: is-01m3jm8wrx99q66pvaq0x5e6d2
title: Repo-wide ktlintFormat pass
kind: task
status: closed
priority: 2
version: 4
delegate: claude-code@vm
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3jm95saj2s5c0gh6d9vmgat
parent_id: is-01m3jm7b8yafz6jr653btzjykf
hold: null
hold_until: null
created_at: 2026-09-27T23:47:43.516Z
updated_at: 2026-09-28T11:35:40.761Z
started_at: 2026-09-28T11:24:21.201Z
closed_at: 2026-09-28T11:35:40.761Z
close_reason: spotlessApply across all modules plus build-logic/ and example/ (38 files, whitespace/wrapping only; 4 wildcard imports expanded by hand). build and -p example build green, 559 tests.
resolution: null
duplicate_of: null
---
Run the formatter once across all modules covered by qk-fhdg, as an isolated, mechanical commit/PR so the tooling diff and the reformat diff review separately.

Coordinate timing: land only when no other agent branches are applied/in flight. A repo-wide reformat will conflict with anyone else's uncommitted or in-progress diffs (tbd showed docs/decisions/agent-skill*.md modified and 1 issue in_progress at planning time — check current state before running this).
