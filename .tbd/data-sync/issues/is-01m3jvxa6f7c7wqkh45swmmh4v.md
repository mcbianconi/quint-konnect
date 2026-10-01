---
type: is
id: is-01m3jvxa6f7c7wqkh45swmmh4v
title: Test runs generate traces for all drivers, even with --tests
kind: feature
status: in_progress
priority: 2
version: 6
spec_path: docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md
delegate: claude-code@macmurillo.local
labels:
  - roadmap
  - assessment
dependencies:
  - type: blocks
    target: is-01m3jvxaytxv2pp08pnx2rkw48
  - type: blocks
    target: is-01m3jvxba17gv8xfrd824tq1xc
  - type: blocks
    target: is-01m3js23q605rb3fyhzfjc8gnf
parent_id: is-01m3jvwgzvywep10dcef17jwd3
hold: null
hold_until: null
created_at: 2026-09-28T02:01:12.655Z
updated_at: 2026-10-01T03:30:21.374Z
started_at: 2026-10-01T03:30:21.374Z
---
Spec section B3 (docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md).
Problem: QuintKonnectPlugin.kt:177,183 make every Test task depend on checkQuint and generateQuintTraces. The default seed is unpinned, so the task never is UP-TO-DATE. A --tests filter does not reduce the work (confirmed: a filtered example run generated traces for all 8 drivers, each time). Test tasks without drivers also need quint.
First step: write the chosen design in this bead (spec Open Questions). Recommended: make generateQuintTraces opt-in with a new QuintKonnectExtension flag (default off), keep test-time generation (TraceGenerator) as default, and do not add checkQuint to Test tasks of a project without drivers.
Acceptance: a filtered test run starts quint only for the drivers that run; functional test; gradle-plugin.api, README, AGENTS.md, skills/quint-konnect/ and docs/decisions/generate-quint-traces-task.md updated.

## Notes

Design decision (user, 2026-09-28): make generateQuintTraces opt-in via a new QuintKonnectExtension flag, default off. Default path is test-time generation (TraceGenerator) per driver. Do not add checkQuint to Test tasks of a project without drivers.
