---
type: is
id: is-01m3jwk917ewch8w2a1f8b450c
title: Remove system-property sharing in chosen modules tests
kind: task
status: open
priority: 2
version: 2
spec_path: docs/project/specs/active/plan-2026-09-27-parallel-test-execution.md
labels: []
dependencies:
  - type: blocks
    target: is-01m3jwk9g8b77d7wh6fp6a3atr
parent_id: is-01m3jwgy900me6x6prh4pvdxjc
created_at: 2026-09-28T02:13:12.359Z
updated_at: 2026-09-28T02:13:12.839Z
---
Replace System.setProperty in the chosen module tests (GeneratorConfigTest, ReplayRunnerTest, ShrinkTest, BuggyRockPaperScissorsShrinkTest, QuintRun/QuintTestTestGeneratorTest) with the existing override parameters (resolveSpec, defaultTraceSource, resolveSeed, maxSamplesOverride, ...). Where the real property is required, @ResourceLock(Resources.SYSTEM_PROPERTIES) READ_WRITE on writers AND READ on every test reaching a property-reading default path.
