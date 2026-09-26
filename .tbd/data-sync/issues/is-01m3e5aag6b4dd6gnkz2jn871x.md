---
type: is
id: is-01m3e5aag6b4dd6gnkz2jn871x
title: Generate Kotlin state and nondet types from the spec
kind: feature
status: open
priority: 3
version: 2
labels:
  - roadmap
dependencies: []
parent_id: is-01m3e5aa1sevc19rem5gdd0hye
created_at: 2026-09-26T06:09:23.974Z
updated_at: 2026-09-26T06:49:38.116Z
---
Generate @Serializable mirror types (records, sum types, Option) from spec types so users only write extractFromDriver. Removes hand-written types like example/.../tictactoe/GameState.kt.
