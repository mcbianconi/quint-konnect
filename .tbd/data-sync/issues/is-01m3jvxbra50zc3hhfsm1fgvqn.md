---
type: is
id: is-01m3jvxbra50zc3hhfsm1fgvqn
title: Small doc and comment fixes from the assessment
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
created_at: 2026-09-28T02:01:14.250Z
updated_at: 2026-09-28T02:01:14.250Z
---
Spec section B7 (docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md).
- README.md:15 says quint runs 'at test runtime'; with the plugin, generateQuintTraces runs it before the tests.
- README 'Modules' row for gradle-plugin misses trace generation, quintIr and shrinkQuintTraces.
- AGENTS.md:102-104 says to grep for 0.1.0 at each release; make it version-neutral.
- core/.../ReplayRunner.kt:43-48 and :72-78: move the two comments to the declarations they describe.
If qk-blt0 already changed README.md:15, skip that item.
