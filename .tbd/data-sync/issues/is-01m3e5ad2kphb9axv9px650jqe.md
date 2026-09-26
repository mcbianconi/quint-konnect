---
type: is
id: is-01m3e5ad2kphb9axv9px650jqe
title: Partial state checks and @QuintIgnore
kind: feature
status: closed
priority: 3
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T06:09:26.610Z
updated_at: 2026-09-26T15:12:10.848Z
closed_at: 2026-09-26T15:12:10.848Z
close_reason: Added @QuintIgnore (annotations, @SerialInfo) and TypedState.compareField hook; check() now decides equality from the field-diff tree so ignored fields never compare/diff. Tests + rock-paper-scissors partial-state example added.
resolution: null
duplicate_of: null
---
State check is equals() on the whole state. Add projection helpers, @QuintIgnore on fields and per-field comparators.
