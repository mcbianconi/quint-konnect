---
type: is
id: is-01m3e63g8rhpv0grhqg8n1zga7
title: Step.fromState mutates the shared trace state map, breaking replay of the same List<ItfTrace>
kind: bug
status: open
priority: 2
version: 1
labels: []
dependencies: []
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
created_at: 2026-09-26T06:23:09.080Z
updated_at: 2026-09-26T06:23:09.080Z
---
extractFromMbtVars/extractFromSumType call state.remove("mbt::actionTaken") and state.remove("mbt::nondetPicks") on the caller-owned LinkedHashMap inside ItfState.value. Calling Runner.runTest twice with the same List<ItfTrace> (e.g. reusing a fixture across two @Test methods, or any caller that keeps traces around for re-use) fails the second time with "Missing mbt::actionTaken variable in the trace", because the keys were already removed on the first pass. See RunnerTest.kt's disabled test `replays the same traces more than once`. Fix: copy the map (or only read, don't mutate) in Step.fromState.
