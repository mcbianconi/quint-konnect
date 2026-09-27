---
type: is
id: is-01m3e5aag6b4dd6gnkz2jn871x
title: Generate Kotlin state and nondet types from the spec
kind: feature
status: in_progress
priority: 3
version: 6
delegate: claude-code@macmurillo.local
labels:
  - roadmap
dependencies: []
parent_id: is-01m3e5aa1sevc19rem5gdd0hye
hold: null
hold_until: null
created_at: 2026-09-26T06:09:23.974Z
updated_at: 2026-09-27T18:30:29.804Z
started_at: 2026-09-27T18:30:29.803Z
---
Generate @Serializable mirror types (records, sum types, Option) from spec types so users only write extractFromDriver. Removes hand-written types like example/.../tictactoe/GameState.kt.
