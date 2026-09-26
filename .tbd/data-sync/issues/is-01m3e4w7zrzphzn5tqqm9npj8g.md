---
type: is
id: is-01m3e4w7zrzphzn5tqqm9npj8g
title: "@QuintTest cannot replay: quint test emits no mbt:: variables"
kind: bug
status: closed
priority: 1
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
created_at: 2026-09-26T06:01:42.648Z
updated_at: 2026-09-26T06:25:26.591Z
closed_at: 2026-09-26T06:25:26.590Z
close_reason: "Viable: quint test (0.32.0) has no --mbt, but Step.fromState's existing extractFromSumType/DriverConfig.nondetPath fallback (mirrors upstream quint-connect's Config.nondet) lets a spec model the action taken as its own sum-type variable. Verified end to end with a new example/quinttest fixture (:example:test green). Recorded in docs/decisions/quint-test-needs-nondet-path.md. No source changes needed to QuintTest.kt/TestConfig.kt/QuintTestTestGenerator.kt; only a hint added to Step.kt's error message."
resolution: null
duplicate_of: null
---
Found while fixing qk-gu38 (quint 0.32.0): quint test does not write mbt::actionTaken / mbt::nondetPicks, and TestConfig passes no --mbt, so Step.fromState fails with 'Missing mbt::actionTaken variable' for every generated @QuintTest. No example uses @QuintTest. Check whether quint test accepts --mbt; otherwise derive the step another way or remove @QuintTest before 0.1.0.
