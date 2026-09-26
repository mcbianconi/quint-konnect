---
type: is
id: is-01m3e5ae0ashs5x7r8bs4nk1c1
title: Register BigIntegerSerializer contextually
kind: chore
status: closed
priority: 3
version: 4
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T06:09:27.561Z
updated_at: 2026-09-26T13:58:25.871Z
closed_at: 2026-09-26T13:58:25.870Z
close_reason: Registered BigIntegerSerializer contextually in ItfValueDecode.kt's SerializersModule; @Contextual BigInteger now decodes via ItfValue.decode without needing @Serializable(with=...). Commit urt on branch itf-dx.
resolution: null
duplicate_of: null
---
Every BigInteger field needs @Serializable(with = BigIntegerSerializer::class). Register it once as a contextual serializer in QuintJson.

## Notes

QuintJson becomes internal ItfJson in :itf (qk-pzl0); register the contextual serializer there.
