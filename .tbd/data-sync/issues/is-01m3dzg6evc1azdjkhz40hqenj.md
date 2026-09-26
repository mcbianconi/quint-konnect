---
type: is
id: is-01m3dzg6evc1azdjkhz40hqenj
title: Avoid unwrapping user sum types with Some/None variants as Option
kind: bug
status: closed
priority: 4
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfs74s438xapfcmfsmexa
created_at: 2026-09-26T04:27:44.986Z
updated_at: 2026-09-26T05:59:00.759Z
closed_at: 2026-09-26T05:59:00.758Z
close_reason: Confirmed fixed as a side effect of qk-9sdm's descriptor gating (only unwrap when target is nullable); added regression test. Commit spl on itf-descriptor-normalization
resolution: null
duplicate_of: null
---
itf/ItfValue.kt:100 treats any record tagged Some/None as Option, silently unwrapping user sum types with those variant names.
