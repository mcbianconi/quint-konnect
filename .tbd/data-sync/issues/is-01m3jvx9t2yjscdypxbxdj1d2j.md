---
type: is
id: is-01m3jvx9t2yjscdypxbxdj1d2j
title: Drivers with the same simple name share one trace directory
kind: bug
status: open
priority: 2
version: 2
spec_path: docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md
labels:
  - roadmap
  - assessment
dependencies:
  - type: blocks
    target: is-01m3jvxa6f7c7wqkh45swmmh4v
parent_id: is-01m3jvwgzvywep10dcef17jwd3
created_at: 2026-09-28T02:01:12.258Z
updated_at: 2026-09-28T02:01:12.655Z
---
Spec section B2 (docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md).
Problem: GenerateQuintTracesTask.kt:161 names the trace directory by DriverManifest.simpleName; core/.../trace/TraceSource.kt defaultTraceSource(testName) reads the same; the generated suite passes testName = simple name (ksp/.../generators/QuintSuiteGenerator.kt). Drivers a.Foo and b.Foo replay each other's traces => false failures. docs/decisions/generate-quint-traces-task.md:28 wrongly says this 'wasn't made worse'.
Fix: use the fully qualified driver name for the trace directory and FailureTraceWriter files; keep the simple name for display. Update core.api if a public signature changes.
Acceptance: a test with two same-named drivers in different packages, each replays only its own traces; decision doc corrected; ./gradlew build and ./gradlew -p example build pass.
