---
type: is
id: is-01m3jwk98q7d9ks9s3gr11qc27
title: Isolate ParallelDynamicTestExecutionTest from outer JUnit parallel config
kind: task
status: closed
priority: 2
version: 3
spec_path: docs/project/specs/active/plan-2026-09-27-parallel-test-execution.md
labels: []
dependencies:
  - type: blocks
    target: is-01m3jwk9g8b77d7wh6fp6a3atr
parent_id: is-01m3jwgy900me6x6prh4pvdxjc
created_at: 2026-09-28T02:13:12.599Z
updated_at: 2026-09-29T01:28:23.290Z
closed_at: 2026-09-29T01:28:23.289Z
close_reason: "Phase 2 not needed: after Phase 1 the build is CPU-bound (every Test task slowed under org.gradle.parallel); ksp:test already forks; functionalTest is the critical path and JUnit class concurrency doesn't apply to it. See spec Results."
resolution: null
duplicate_of: null
---
Only if ksp is in Phase 2 scope. Add @Isolated; set junit.jupiter.execution.parallel.* explicitly on its nested Launcher request so it never inherits the outer JVM config. Verify: temporarily remove @Execution(CONCURRENT) from QuintRunTestGenerator and confirm the test fails; restore.
