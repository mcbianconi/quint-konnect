---
type: is
id: is-01m3jvxcejc3bt8rx60emxxynq
title: End-to-end test for replay with readSpecIr
kind: task
status: open
priority: 3
version: 1
spec_path: docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md
labels:
  - roadmap
  - assessment
dependencies: []
parent_id: is-01m3jvwgzvywep10dcef17jwd3
created_at: 2026-09-28T02:01:14.962Z
updated_at: 2026-09-28T02:01:14.962Z
---
Spec section B9 (docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md).
Closed bead qk-sa74 asked for this; current tests (gradle-plugin/src/functionalTest/.../QuintIrFunctionalTest.kt:136,165) only check the task graph.
Add a functional test or a CI step that compiles a readSpecIr project and replays a saved .itf.json with -Pquint.replay (compileTestKotlin and test both pass).
