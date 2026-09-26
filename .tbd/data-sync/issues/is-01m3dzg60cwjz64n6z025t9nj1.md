---
type: is
id: is-01m3dzg60cwjz64n6z025t9nj1
title: Escape regex in --match and string literals in KSP-generated code
kind: bug
status: open
priority: 3
version: 1
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfs74s438xapfcmfsmexa
created_at: 2026-09-26T04:27:44.523Z
updated_at: 2026-09-26T04:27:44.523Z
---
trace/TestConfig.kt:20 builds ^test$ without escaping. KSP generators paste spec, seed and action names into Kotlin string literals without escaping, so $ or quotes break compilation.
