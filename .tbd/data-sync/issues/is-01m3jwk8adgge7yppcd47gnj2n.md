---
type: is
id: is-01m3jwk8adgge7yppcd47gnj2n
title: Set maxHeapSize for ksp test forks if baseline shows memory pressure
kind: task
status: open
priority: 2
version: 2
spec_path: docs/project/specs/active/plan-2026-09-27-parallel-test-execution.md
labels: []
dependencies:
  - type: blocks
    target: is-01m3jwk8hz5xgy6w0bsg8gey9j
parent_id: is-01m3jwgy900me6x6prh4pvdxjc
created_at: 2026-09-28T02:13:11.628Z
updated_at: 2026-09-28T02:13:11.870Z
---
Each fork gets its own heap (default 512m); org.gradle.jvmargs only sizes the daemon. If the baseline shows ksp (kotlin-compile-testing) near its heap limit, set maxHeapSize on ksp tasks.test and check peak forks x heap against a 4-vCPU ubuntu-latest runner. Close as not needed otherwise, with the numbers.
