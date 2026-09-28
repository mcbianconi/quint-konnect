---
type: is
id: is-01m3jvxaytxv2pp08pnx2rkw48
title: One quint command builder, or a parity test
kind: chore
status: open
priority: 3
version: 1
spec_path: docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md
labels:
  - roadmap
  - assessment
dependencies: []
parent_id: is-01m3jvwgzvywep10dcef17jwd3
created_at: 2026-09-28T02:01:13.434Z
updated_at: 2026-09-28T02:01:13.434Z
---
Spec section B5 (docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md).
Problem: GenerateQuintTracesTask.buildCommand (:192) copies RunConfig.toCommand/TestConfig.toCommand; no test compares them. escapeRegex, sanitizeFileName and sequenceNumber have 2-3 copies; TraceGenerator matches the first digit group, ItfFileTraceSource/TracesDirTraceSource the last.
Fix: parity test in gradle-plugin (:core as test dependency) comparing both argv lists for run and test kinds; move the core helpers into one internal file. If qk-blt0 removed the Gradle path, do only the core part.
