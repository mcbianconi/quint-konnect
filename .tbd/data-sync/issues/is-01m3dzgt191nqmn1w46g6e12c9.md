---
type: is
id: is-01m3dzgt191nqmn1w46g6e12c9
title: KSP processor tests
kind: task
status: closed
priority: 2
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
created_at: 2026-09-26T04:28:05.033Z
updated_at: 2026-09-26T13:16:16.195Z
closed_at: 2026-09-26T13:16:16.194Z
close_reason: "Added ksp/src/test/kotlin/.../ksp/{CompileTestSupport,StepMethodGeneratorTest,QuintRunTestGeneratorTest,QuintTestTestGeneratorTest,ProcessorErrorTest}.kt using dev.zacsweers.kctfork:core/:ksp 0.14.0 (matches Kotlin 2.4.20 / KSP 2.3.12). 17 tests passed, 1 disabled documenting qk-nqry (duplicate @QuintAction names not rejected). Full build --rerun-tasks: 111 tests, 110 passed, 1 skipped, ABI check green. Committed as ksp-tests branch (wns) above maven-central-publish."
resolution: null
duplicate_of: null
---
No tests for the processor. Use kotlin-compile-testing (KSP2 support) to assert generated code for actions, nullable params, generic params.
