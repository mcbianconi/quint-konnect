---
type: is
id: is-01m3jvxbra50zc3hhfsm1fgvqn
title: Small doc and comment fixes from the assessment
kind: chore
status: open
priority: 3
version: 5
spec_path: docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md
delegate: null
labels:
  - roadmap
  - assessment
dependencies: []
parent_id: is-01m3jvwgzvywep10dcef17jwd3
hold: null
hold_until: null
created_at: 2026-09-28T02:01:14.250Z
updated_at: 2026-10-01T05:37:16.124Z
started_at: 2026-09-28T02:17:47.858Z
---
Spec section B7 (docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md).

README items are done in qk-6fru, still uncommitted. The intro states that the default runs quint at test time, and that generateTraces runs quint before test. The gradle-plugin row names checkQuint, downloadQuint, quintIr, generateQuintTraces, and shrinkQuintTraces. If qk-blt0 already changed the test-runtime sentence, that item was already skippable. Do not redo the README.

Remaining:
- AGENTS.md release text that says to search for 0.1.0. Make that text independent of the version.
- core/.../ReplayRunner.kt comments at the two sites the spec names. Move each comment to the declaration it describes. Line numbers may have moved.

## Notes

Restart 2026-09-30: claim cleared; no code landed. Ready to pick up again from the bead description.
