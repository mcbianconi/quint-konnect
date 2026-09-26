---
type: is
id: is-01m3dzg5g01a96h58yw3ynv3mr
title: Unwrap Quint Option values inside state
kind: bug
status: open
priority: 2
version: 1
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfs74s438xapfcmfsmexa
created_at: 2026-09-26T04:27:43.998Z
updated_at: 2026-09-26T04:27:43.998Z
---
Only nondet picks are unwrapped via intoOption. A nullable Kotlin state field cannot decode {tag: None}. Upstream offers itf::de::Option for this. Provide a serializer or normalizer option.
