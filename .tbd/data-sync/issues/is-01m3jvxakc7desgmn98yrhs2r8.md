---
type: is
id: is-01m3jvxakc7desgmn98yrhs2r8
title: No time limit for quint in generateQuintTraces and quintIr
kind: bug
status: open
priority: 3
version: 1
spec_path: docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md
labels:
  - roadmap
  - assessment
dependencies: []
parent_id: is-01m3jvwgzvywep10dcef17jwd3
created_at: 2026-09-28T02:01:13.068Z
updated_at: 2026-09-28T02:01:13.068Z
---
Spec section B4 (docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md).
Problem: execOperations.exec has no time limit (GenerateQuintTracesTask.kt:174, QuintIrTask.kt:73). TraceGenerator has 10 minutes (GeneratorConfig.timeout); the default plugin path lost it after qk-adm2, against the intent of qk-0vs7. A hung quint stops the build.
Fix: set the Gradle Task.timeout convention to 10 minutes on both tasks at registration.
Acceptance: a test with a fake quint that never exits; the build fails with a message that names the task.
