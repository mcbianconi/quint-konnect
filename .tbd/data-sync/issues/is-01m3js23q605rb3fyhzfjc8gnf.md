---
type: is
id: is-01m3js23q605rb3fyhzfjc8gnf
title: Update README to reduce boilerplate
kind: task
status: in_progress
priority: 2
version: 3
delegate: claude-code@macmurillo.local
labels:
  - readme
  - docs
dependencies: []
hold: null
hold_until: null
created_at: 2026-09-28T01:11:24.126Z
updated_at: 2026-10-01T05:37:15.757Z
started_at: 2026-10-01T05:19:36.693Z
---
Trim the README so it carries less boilerplate. Consider rewriting it in ASD-STE100 (Simplified Technical English) style for clarity and unambiguous parsing.

## Notes

README rewrite is in the working tree and is not committed.

Mode: how-to. Lookup tables stayed on the same page. A second reference file would copy the same facts again.

Corrected one false claim: maxSteps -1 omits --max-steps. Quint 0.32.0 then uses 20. The old README said unlimited.

qk-5fyu still owns removal of Runner.runTest and -Pquint.parallelism. Those names stay in "Override a run from Gradle" and "Run Runner.runTest on a thread pool" so that bead can delete them.

qk-g80c README items are done here. The intro states test-time generation as the default, and generateTraces as the path that runs quint before test. The gradle-plugin module row names checkQuint, downloadQuint, quintIr, generateQuintTraces, and shrinkQuintTraces. AGENTS.md version-grep text and the ReplayRunner comment move stay on qk-g80c.

Heading renames are updated in AGENTS.md, the replay decision, ReplayRunner.kt, the skill, and the parallel-execution spec.
