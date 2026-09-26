---
type: is
id: is-01m3e0m0ssyvzycnq141ag65sd
title: Decode empty tuple/record-keyed maps
kind: bug
status: open
priority: 2
version: 1
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfs74s438xapfcmfsmexa
created_at: 2026-09-26T04:47:18.841Z
updated_at: 2026-09-26T04:47:18.841Z
---
An empty ItfValue.Map normalizes to {} because there are no keys to inspect, so it fails to decode into Map<List<Long>, V> or Map<R, V>. A Quint var initialized to Map() with complex keys breaks at init. Needs descriptor-aware normalization (thread the target serializer into toNormalizedJson from State.kt and NondetPicks.kt).
