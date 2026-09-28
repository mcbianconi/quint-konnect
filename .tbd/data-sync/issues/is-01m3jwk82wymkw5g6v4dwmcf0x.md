---
type: is
id: is-01m3jwk82wymkw5g6v4dwmcf0x
title: CPU-derived maxParallelForks for every Test task
kind: task
status: open
priority: 2
version: 3
spec_path: docs/project/specs/active/plan-2026-09-27-parallel-test-execution.md
labels: []
dependencies:
  - type: blocks
    target: is-01m3jwk8adgge7yppcd47gnj2n
  - type: blocks
    target: is-01m3jwk8hz5xgy6w0bsg8gey9j
parent_id: is-01m3jwgy900me6x6prh4pvdxjc
created_at: 2026-09-28T02:13:11.387Z
updated_at: 2026-09-28T02:13:11.870Z
---
In build-logic quintkonnect.kotlin-jvm.gradle.kts: tasks.withType<Test>().configureEach { maxParallelForks = (availableProcessors / divisor).coerceIn(1, MAX_FORKS) }, values from the baseline, reference comment. Pin gradle-plugin:functionalTest to 1 unless concurrent GradleRunner use with the shared test-kit dir is confirmed safe (else per-fork withTestKitDir).
