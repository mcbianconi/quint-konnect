---
type: is
id: is-01m3jvx9b3e1bemkg8mjv156jy
title: "generateQuintTraces ignores spec content: stale traces with a pinned seed"
kind: bug
status: open
priority: 1
version: 8
spec_path: docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md
delegate: null
labels:
  - roadmap
  - assessment
dependencies:
  - type: blocks
    target: is-01m3jvx9t2yjscdypxbxdj1d2j
  - type: blocks
    target: is-01m3jvxakc7desgmn98yrhs2r8
parent_id: is-01m3jvwgzvywep10dcef17jwd3
hold: null
hold_until: null
created_at: 2026-09-28T02:01:11.778Z
updated_at: 2026-10-01T02:58:18.121Z
started_at: 2026-09-28T02:17:47.853Z
closed_at: null
close_reason: null
resolution: null
duplicate_of: null
---
Spec section B1 (docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md).
Problem: GenerateQuintTracesTask inputs are the KSP manifests (spec path only), quintVersion, overrides and QUINT_SEED (gradle-plugin/.../GenerateQuintTracesTask.kt:96-123). The task is @CacheableTask (:89). With a pinned seed, a spec change does not generate new traces; the build cache can also send old traces to other machines. Imported .qnt files are not inputs either. Closed bead qk-adm2 said the spec is an input.
Repro (confirmed 2026-09-28): ./gradlew -p example test --tests '*InvariantCounterDriver*' -Pquint.seed=0x1 passes. Change n' = n + 1 to n' = n + 2 in example/src/test/resources/invariants/counter.qnt. Same command: generateQuintTraces UP-TO-DATE, test passes. With generateQuintTraces --rerun: fails 'n: spec=2, impl=1'.
Fix: add an @InputFiles @PathSensitive(RELATIVE) spec property, set from extension.quintIrSpecs and each manifest's resolved spec path.
Acceptance: new case in GenerateQuintTracesFunctionalTest.kt (pinned seed + spec change => task executes, not UP-TO-DATE/FROM-CACHE); the repro above fails without --rerun; update docs/decisions/generate-quint-traces-task.md and skills/quint-konnect/ if behavior text changes.

## Notes

Restart 2026-09-30: work existed only on GitButler branch fix/qk-q5z8-traces-spec-input (commit bf1b534), stacked under fix/qk-q6av. Not on origin/main. Reimplement from the bead/spec; local branch is optional reference only.
