---
type: is
id: is-01m3dzg4s6e75qzmk53s7kzxag
title: "Runner: report trace index, step index and action on failure"
kind: bug
status: closed
priority: 1
version: 3
delegate: claude-code@macmurillo.local
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfs74s438xapfcmfsmexa
hold: null
hold_until: null
created_at: 2026-09-26T04:27:43.269Z
updated_at: 2026-09-26T04:36:37.910Z
started_at: 2026-09-26T04:33:32.758Z
closed_at: 2026-09-26T04:36:37.910Z
close_reason: Runner catches Throwable, wraps step failures with trace/step/action/nondet context, preserves original as cause
resolution: null
duplicate_of: null
---
Runner.kt:33-53. A failure only shows the raw exception; wrap it with trace number, step number, action name and nondet picks so the diverging step is identifiable.
