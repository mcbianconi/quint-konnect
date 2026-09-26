---
type: is
id: is-01m3dzg50dc3bb03mwhhtpx67m
title: Document and test Set<T> for Quint Set instead of List<T>
kind: bug
status: open
priority: 1
version: 1
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfs74s438xapfcmfsmexa
created_at: 2026-09-26T04:27:43.500Z
updated_at: 2026-09-26T04:27:43.500Z
---
README/CLAUDE.md type table maps Set[T] to List<T>. ITF set element order is unspecified, so List equality can fail on a correct implementation. Verify kotlinx decodes the normalized JSON array into Set<T>, add a test, update both docs.
