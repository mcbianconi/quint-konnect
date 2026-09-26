---
type: is
id: is-01m3dzg67n688k8p22qr3kqqz7
title: Handle BigInt values larger than Long
kind: bug
status: open
priority: 3
version: 1
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfs74s438xapfcmfsmexa
created_at: 2026-09-26T04:27:44.756Z
updated_at: 2026-09-26T04:27:44.756Z
---
ItfValueNormalizer.kt:31 emits a BigDecimal JSON primitive with no serializer to decode it. Provide a BigInteger serializer or document the limitation.
