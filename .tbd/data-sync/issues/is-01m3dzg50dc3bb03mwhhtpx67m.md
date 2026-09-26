---
type: is
id: is-01m3dzg50dc3bb03mwhhtpx67m
title: Document and test Set<T> for Quint Set instead of List<T>
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
created_at: 2026-09-26T04:27:43.500Z
updated_at: 2026-09-26T04:46:39.526Z
started_at: 2026-09-26T04:33:31.714Z
closed_at: 2026-09-26T04:46:39.525Z
close_reason: Set decodes into Set<T> (order-independent, tested with records/sum types); tuple/record map keys now normalize to a flat JsonArray + allowStructuredMapKeys and decode via Map<List<Long>,V>/Map<R,V>; empty complex-keyed maps remain a documented limitation (indistinguishable from empty primitive-keyed maps at the ItfValue level).
resolution: null
duplicate_of: null
---
README/CLAUDE.md type table maps Set[T] to List<T>. ITF set element order is unspecified, so List equality can fail on a correct implementation. Verify kotlinx decodes the normalized JSON array into Set<T>, add a test, update both docs.
