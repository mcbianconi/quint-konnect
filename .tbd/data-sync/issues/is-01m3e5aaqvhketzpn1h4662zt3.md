---
type: is
id: is-01m3e5aaqvhketzpn1h4662zt3
title: Check action and nondet names against the spec at compile time
kind: feature
status: closed
priority: 3
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3e5aa1sevc19rem5gdd0hye
created_at: 2026-09-26T06:09:24.218Z
updated_at: 2026-09-27T18:03:35.668Z
closed_at: 2026-09-27T18:03:35.668Z
close_reason: null
resolution: null
duplicate_of: null
---
Fail compilation when a @QuintAction name or parameter name does not exist in the spec, or a parameter type does not match. Today a typo in a rarely picked action passes until a trace reaches it.
