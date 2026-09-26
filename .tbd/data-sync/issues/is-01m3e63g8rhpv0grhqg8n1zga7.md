---
type: is
id: is-01m3e63g8rhpv0grhqg8n1zga7
title: Step.fromState mutates the shared trace state map, breaking replay of the same List<ItfTrace>
kind: bug
status: closed
priority: 2
version: 3
delegate: claude-code@macmurillo.local
labels: []
dependencies: []
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
hold: null
hold_until: null
created_at: 2026-09-26T06:23:09.080Z
updated_at: 2026-09-26T12:23:55.120Z
started_at: 2026-09-26T07:01:24.887Z
closed_at: 2026-09-26T12:23:55.116Z
close_reason: Fixed on replay-fixes branch (759e107, 4c9d88c), stacked above itf-module. Tests re-enabled and passing.
resolution: null
duplicate_of: null
---
extractFromMbtVars/extractFromSumType call state.remove("mbt::actionTaken") and state.remove("mbt::nondetPicks") on the caller-owned LinkedHashMap inside ItfState.value. Calling Runner.runTest twice with the same List<ItfTrace> (e.g. reusing a fixture across two @Test methods, or any caller that keeps traces around for re-use) fails the second time with "Missing mbt::actionTaken variable in the trace", because the keys were already removed on the first pass. See RunnerTest.kt's disabled test `replays the same traces more than once`. Fix: copy the map (or only read, don't mutate) in Step.fromState.
