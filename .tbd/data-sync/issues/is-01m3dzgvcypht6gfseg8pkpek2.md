---
type: is
id: is-01m3dzgvcypht6gfseg8pkpek2
title: Real diff on state mismatch
kind: feature
status: closed
priority: 2
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T04:28:06.429Z
updated_at: 2026-09-26T14:49:02.003Z
closed_at: 2026-09-26T14:49:02.002Z
close_reason: Field-level diff via SerialDescriptor/Encoder walk landed on state-diagnostics (commit tkx). buildFieldDiff/DiffValue in core/.../StateDiff.kt; full toString() diff kept as the exception's cause.
resolution: null
duplicate_of: null
---
TypedState.buildDiff prints both toString() outputs in full. Show a field-level or line diff.
