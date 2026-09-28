---
type: is
id: is-01m3jm960y58d5swkkv3aw8e9p
title: Document the ktlint workflow
kind: task
status: closed
priority: 3
version: 3
delegate: claude-code@vm
labels:
  - roadmap
dependencies: []
parent_id: is-01m3jm7b8yafz6jr653btzjykf
hold: null
hold_until: null
created_at: 2026-09-27T23:47:52.989Z
updated_at: 2026-09-28T11:52:58.281Z
started_at: 2026-09-28T11:42:22.294Z
closed_at: 2026-09-28T11:52:58.280Z
close_reason: "AGENTS.md Formatting section (commands, single-file -PspotlessIdeHook form, pre-push hook and which git/GitButler commands run it; no bypass flag), README Build & test line, .editorconfig header on the daemon cache (reproduced: ktlint's static ThreadSafeEditorConfigCache survives builds in one daemon)."
resolution: null
duplicate_of: null
---
Add a short section to AGENTS.md (and README.md if it's user-facing rather than internal) covering the ktlintFormat/ktlintCheck commands and how the local hook works. Do not document a bypass flag — project rules forbid --no-verify, and agents read AGENTS.md.
