---
type: is
id: is-01m3dzg67n688k8p22qr3kqqz7
title: Handle BigInt values larger than Long
kind: bug
status: closed
priority: 3
version: 4
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3e5ae0ashs5x7r8bs4nk1c1
parent_id: is-01m3dzfs74s438xapfcmfsmexa
created_at: 2026-09-26T04:27:44.756Z
updated_at: 2026-09-26T06:09:27.561Z
closed_at: 2026-09-26T06:00:02.672Z
close_reason: Added BigIntegerSerializer and switched BigInt normalization from BigDecimal to BigInteger. Commit rkk on itf-descriptor-normalization
resolution: null
duplicate_of: null
---
ItfValueNormalizer.kt:31 emits a BigDecimal JSON primitive with no serializer to decode it. Provide a BigInteger serializer or document the limitation.
