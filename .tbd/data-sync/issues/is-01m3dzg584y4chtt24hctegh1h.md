---
type: is
id: is-01m3dzg584y4chtt24hctegh1h
title: Support tuple and record keys in Quint maps
kind: bug
status: open
priority: 1
version: 1
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfs74s438xapfcmfsmexa
created_at: 2026-09-26T04:27:43.747Z
updated_at: 2026-09-26T04:27:43.747Z
---
itf/ItfValueNormalizer.kt:45 throws 'Cannot use Tup as a JSON object key'. (int, int) -> V is a common Quint shape. Option: normalize non-primitive-key maps to a list of pairs and document the Kotlin mapping.
