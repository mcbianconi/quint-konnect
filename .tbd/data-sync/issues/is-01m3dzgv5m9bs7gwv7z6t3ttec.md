---
type: is
id: is-01m3dzgv5m9bs7gwv7z6t3ttec
title: One JUnit dynamic test per trace
kind: feature
status: closed
priority: 2
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T04:28:06.195Z
updated_at: 2026-09-26T15:13:08.887Z
closed_at: 2026-09-26T15:13:08.886Z
close_reason: "Added ReplayRunner.traceReplays / TraceReplay (core) and switched QuintRunTestGenerator/QuintTestTestGenerator to emit a @TestFactory returning one DynamicTest per trace; runTest/Runner kept for binary compat. Verified in example: test report shows one testcase named 'trace N (seed 0x...)' per trace."
resolution: null
duplicate_of: null
---
All traces run in one @Test. Generate a @TestFactory so each trace appears separately in IDE and reports.
