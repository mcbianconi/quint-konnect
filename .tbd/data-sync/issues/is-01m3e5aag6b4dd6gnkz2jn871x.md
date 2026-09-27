---
type: is
id: is-01m3e5aag6b4dd6gnkz2jn871x
title: Generate Kotlin state and nondet types from the spec
kind: feature
status: closed
priority: 3
version: 7
delegate: claude-code@macmurillo.local
labels:
  - roadmap
dependencies: []
parent_id: is-01m3e5aa1sevc19rem5gdd0hye
hold: null
hold_until: null
created_at: 2026-09-26T06:09:23.974Z
updated_at: 2026-09-27T18:46:56.148Z
started_at: 2026-09-27T18:30:29.803Z
closed_at: 2026-09-27T18:46:56.142Z
close_reason: KSP generates object <Module>Spec (@Serializable typedef classes, State, anonymous nondet records) from spec IR; example TicTacToeState uses it. Branch spec-types-codegen, stacked on spec-aware-codegen.
resolution: null
duplicate_of: null
---
Generate @Serializable mirror types (records, sum types, Option) from spec types so users only write extractFromDriver. Removes hand-written types like example/.../tictactoe/GameState.kt.
