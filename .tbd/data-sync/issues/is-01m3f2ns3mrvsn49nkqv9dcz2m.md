---
type: is
id: is-01m3f2ns3mrvsn49nkqv9dcz2m
title: "ItfValueSerializer doesn't decode ADR-015 #unserializable values"
kind: bug
status: closed
priority: 2
version: 2
labels: []
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T14:42:28.083Z
updated_at: 2026-09-26T15:28:58.020Z
closed_at: 2026-09-26T15:28:58.019Z
close_reason: "Fixed in fix(itf): parse and write ADR-015 #unserializable values; regression test enabled."
resolution: null
duplicate_of: null
---
ItfValueSerializer.fromObject has no case for ADR-015's {"#unserializable": "<string>"} shape (https://apalache-mc.org/docs/adr/015adr-trace.html), so it falls through to the generic Record branch and parses it as a record with a literal "#unserializable" key instead of ItfValue.Unserializable. Found by a round-trip property test in itf/src/test/kotlin/io/github/mcbianconi/itf/ItfValuePropertyTest.kt (disabled test: 'ItfValueSerializer parses ADR-015 unserializable values'). Fix: add a keys.size==1 && "#unserializable" in obj branch to fromObject, mirroring #bigint/#tup/#set/#map.
