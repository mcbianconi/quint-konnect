---
type: is
id: is-01m3nch4xyc94szrj89tbc80bs
title: "CI: Build step is not faster after Phase 1 parallelism; measure and reduce contention"
kind: task
status: open
priority: 2
version: 1
spec_path: docs/project/specs/active/plan-2026-09-27-parallel-test-execution.md
labels: []
dependencies: []
created_at: 2026-09-29T01:30:08.702Z
updated_at: 2026-09-29T01:30:08.702Z
---
Local build dropped 83s to ~60s, but CI (ubuntu-latest, 4 vCPU) Build step went from ~168s (baseline, 3 runs) to 181s (run 36413189477) and 201s (run 36457753260, includes the quint Rust evaluator fetch from qk-0lpw). Collect more CI samples, then try a lower maxParallelForks when CI is set (e.g. 1 fork on 4 vCPUs) and re-measure. If CI is still slower than ~168s, pass --no-parallel in the workflow. Record results in the spec's Results section.
