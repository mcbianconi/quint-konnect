---
type: is
id: is-01m3dzg6evc1azdjkhz40hqenj
title: Avoid unwrapping user sum types with Some/None variants as Option
kind: bug
status: open
priority: 4
version: 1
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfs74s438xapfcmfsmexa
created_at: 2026-09-26T04:27:44.986Z
updated_at: 2026-09-26T04:27:44.986Z
---
itf/ItfValue.kt:100 treats any record tagged Some/None as Option, silently unwrapping user sum types with those variant names.
