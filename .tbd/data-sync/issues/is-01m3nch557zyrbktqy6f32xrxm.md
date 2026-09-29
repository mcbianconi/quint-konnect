---
type: is
id: is-01m3nch557zyrbktqy6f32xrxm
title: Is concurrent GradleRunner use with the default TestKit dir safe on Gradle 9.8?
kind: task
status: open
priority: 3
version: 1
spec_path: docs/project/specs/active/plan-2026-09-27-parallel-test-execution.md
labels: []
dependencies: []
created_at: 2026-09-29T01:30:08.934Z
updated_at: 2026-09-29T01:30:08.934Z
---
Open question from the parallel-test-execution spec. :gradle-plugin:functionalTest (49s after Phase 1) is the longest test task and stays at maxParallelForks=1 because the Gradle docs don't say whether concurrent GradleRunners sharing the default test-kit directory are safe. Confirm by test or docs; if unsafe, consider a per-fork withTestKitDir. Only worth doing if the CI contention bead shows spare CPU.
