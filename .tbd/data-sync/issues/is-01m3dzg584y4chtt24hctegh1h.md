---
type: is
id: is-01m3dzg584y4chtt24hctegh1h
title: Support tuple and record keys in Quint maps
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
created_at: 2026-09-26T04:27:43.747Z
updated_at: 2026-09-26T04:46:39.534Z
started_at: 2026-09-26T04:33:31.918Z
closed_at: 2026-09-26T04:46:39.534Z
close_reason: Set decodes into Set<T> (order-independent, tested with records/sum types); tuple/record map keys now normalize to a flat JsonArray + allowStructuredMapKeys and decode via Map<List<Long>,V>/Map<R,V>; empty complex-keyed maps remain a documented limitation (indistinguishable from empty primitive-keyed maps at the ItfValue level).
resolution: null
duplicate_of: null
---
itf/ItfValueNormalizer.kt:45 throws 'Cannot use Tup as a JSON object key'. (int, int) -> V is a common Quint shape. Option: normalize non-primitive-key maps to a list of pairs and document the Kotlin mapping.
