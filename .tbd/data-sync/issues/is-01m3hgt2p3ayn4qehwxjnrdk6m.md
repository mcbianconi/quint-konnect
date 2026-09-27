---
type: is
id: is-01m3hgt2p3ayn4qehwxjnrdk6m
title: "Parallel per-trace tests: README documents a JUnit property that doesn't exist"
kind: bug
status: closed
priority: 2
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfsw93v96g3zbdqzjze2j
created_at: 2026-09-27T13:27:57.890Z
updated_at: 2026-09-27T13:48:07.756Z
closed_at: 2026-09-27T13:48:07.755Z
close_reason: "Fixed: generated @TestFactory methods carry @Execution(ExecutionMode.CONCURRENT); verified via junit-jupiter-engine source (DynamicNodeTestDescriptor/JupiterTestDescriptor) that dynamic tests inherit their factory's mode, and it's a no-op without junit.jupiter.execution.parallel.enabled=true (SameThreadHierarchicalTestExecutorService ignores ExecutionMode). Proved with ParallelDynamicTestExecutionTest (JUnit Platform Launcher, ksp module). Fixed README/SKILL.md's bogus mode.dynamic.default property."
resolution: null
duplicate_of: null
---
Found by qk-pbk2: README 'Running traces in parallel' (qk-t281) sets junit.jupiter.execution.parallel.mode.dynamic.default, which exists in neither JUnit 5 nor 6 (only mode.default and mode.classes.default; checked Constants.java). So generated @TestFactory traces don't run concurrently as documented. Fix: verify how dynamic tests inherit execution mode (e.g. generate @Execution(ExecutionMode.CONCURRENT) on the generated @TestFactory method/class so only quint-konnect tests go parallel, with junit.jupiter.execution.parallel.enabled=true), add a test that proves traces actually overlap in time, and fix README + skill.
