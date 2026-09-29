---
type: is
id: is-01m3jwk8swnrq45r9twma3ht5p
title: "Decide Phase 2 scope: which modules get in-JVM JUnit concurrency"
kind: task
status: closed
priority: 2
version: 4
spec_path: docs/project/specs/active/plan-2026-09-27-parallel-test-execution.md
labels: []
dependencies:
  - type: blocks
    target: is-01m3jwk917ewch8w2a1f8b450c
  - type: blocks
    target: is-01m3jwk98q7d9ks9s3gr11qc27
parent_id: is-01m3jwgy900me6x6prh4pvdxjc
created_at: 2026-09-28T02:13:12.123Z
updated_at: 2026-09-29T01:28:22.871Z
closed_at: 2026-09-29T01:28:22.871Z
close_reason: "Phase 2 not needed: after Phase 1 the build is CPU-bound (every Test task slowed under org.gradle.parallel); ksp:test already forks; functionalTest is the critical path and JUnit class concurrency doesn't apply to it. See spec Results."
resolution: null
duplicate_of: null
---
From the Phase 1 numbers, pick modules whose test task is still the critical path. If none, close this and the remaining Phase 2 beads as not needed.
